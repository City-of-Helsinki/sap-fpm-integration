package fi.hel.integration.sapfpm.tositecommon;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jdbc.JdbcConstants;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class TositeDbCommon extends RouteBuilder {

    public ProcessorDefinition<?> buildFileAndContentsDbRoute(String fromUri, String routeId, String insertFileUri, String insertTositeAndRivitUri) {
        return from(fromUri).routeId(routeId)
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple("${exception.message} contains 'Duplicate entry' || ${exception.message} contains 'primary key violation'"))
                .log("Failed to insert file ${headers.CamelFileName} into the db, already processed!")
                .process(e -> e.getMessage().setBody(null))
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .continued(true)
            .end()
            .log("receipts to insert: ${body.size()}")
                .to(insertFileUri)
            .choice().when(body().isNotNull())
                .split(body())
                    .to(insertTositeAndRivitUri)
                .end()
            .end();
    }

    public ProcessorDefinition<?> buildInsertReceiptAndLinesIntoDb(String fromUri, String insertReceiptUri, String insertReceiptLineUri) {
        return from(fromUri)
            .onException(SQLIntegrityConstraintViolationException.class)
                    .onWhen(simple("${exception.message} contains 'Duplicate entry' || ${exception.message} contains 'primary key violation'"))
                    .log("Failed to insert ${headers.BUKRS} ${headers.BELNR} into db due to a duplicate in ${headers.CamelFileName}!")
                    .continued(true)
                    .process(e -> e.getMessage().setBody(null))
                    .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .end()
            .process(e -> {
                List<LinkedHashMap<String, Object>> receiptMetadata =  e.getMessage().getBody(List.class);
                receiptMetadata.stream().findFirst().ifPresentOrElse(firstReceipt ->
                                e.getMessage().setHeader("firstReceipt", firstReceipt)
                        , () -> e.getMessage().removeHeader("firstReceipt"));
            })
            .transacted("PROPAGATION_REQUIRES_NEW")
            .to(insertReceiptUri)
            .choice()
                .when(body().isNotNull())
                    .split(body())
                        .to(insertReceiptLineUri)
                    .end()
            .end();
    }
}
