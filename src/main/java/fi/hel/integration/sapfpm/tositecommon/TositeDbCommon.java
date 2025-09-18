package fi.hel.integration.sapfpm.tositecommon;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class TositeDbCommon extends RouteBuilder {

    public final static String DUPLICATE_ENTRY_EXCEPTION_MESSAGE = "${exception.message} contains 'Duplicate entry' || ${exception.message} contains 'primary key violation'";

    @ConfigProperty(name = "default-route-redelivery-delay", defaultValue = "10000")
    public int DEFAULT_REDELIVERY_DELAY;

    @ConfigProperty(name = "default-route-max-redeliveries", defaultValue = "120")
    public int DEFAULT_MAX_REDELIVERIES;

    public ProcessorDefinition<?> buildFileAndContentsDbRoute(String fromUri, String routeId, String insertFileUri, String insertTositeAndRivitUri) {
        return from(fromUri).routeId(routeId)
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple(DUPLICATE_ENTRY_EXCEPTION_MESSAGE))
                .log("Failed to insert file ${headers.CamelFileName} into the db, already processed!")
                .process(e -> e.getMessage().setBody(null))
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .continued(true)
            .end()
            .onException(Exception.class)
                .maximumRedeliveries(DEFAULT_MAX_REDELIVERIES).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .log("receipts to insert: ${body.size()}")
            //.transacted("PROPAGATION_REQUIRES_NEW")
            .transacted()
            .to(insertFileUri)
            .choice().when(body().isNotNull())
                .split(body()).stopOnException(true)
                    .to(insertTositeAndRivitUri)
                    .setBody(constant(""))
                    .removeProperty("originalBody")
                    .removeHeader("firstReceipt")
                    .removeProperty("sqlNamedParams")
                    .removeProperty("sqlValNames")
                .end()
            .end()
            .setBody(constant(""));
    }

    public ProcessorDefinition<?> buildInsertReceiptAndLinesIntoDb(String fromUri, String insertReceiptUri, String insertReceiptLineUri) {
        return from(fromUri)
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple(DUPLICATE_ENTRY_EXCEPTION_MESSAGE))
                    .process(e -> {
                        Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
                        if (jdbcParams != null && !jdbcParams.isEmpty()) {
                            log.info("Failed to insert %s %s into db due to a duplicate in %s".formatted(
                                jdbcParams.get("BUKRS"), jdbcParams.get("BELNR"),
                                e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)
                            ));
                        } else {
                            log.info("Failed to insert receipt line into the db due to a duplicate in %s".formatted(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)));
                        }
                    })
                    .continued(true)
                    .process(e -> e.getMessage().setBody(null))
                    .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .end()
            .onException(Exception.class) // due to transaction, will retry in calling route and also this whole route
                .maximumRedeliveries(DEFAULT_MAX_REDELIVERIES).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .process(e -> {
                List<LinkedHashMap<String, Object>> receiptMetadata =  e.getMessage().getBody(List.class);
                receiptMetadata.stream().findFirst().ifPresentOrElse(firstReceipt ->
                                e.getMessage().setHeader("firstReceipt", firstReceipt)
                        , () -> e.getMessage().removeHeader("firstReceipt"));
            })
            .transacted()
            .to(insertReceiptUri)
            .choice()
                .when(body().isNotNull())
                    .split(body()).stopOnException(true)
                        .to(insertReceiptLineUri)
                    .end()
            .end();
    }

    public ProcessorDefinition<?> buildInsertRiviIntoDb(String fromUri, String routeId, String dbTable) {
        return from(fromUri)
                .routeId(routeId)
                .errorHandler(noErrorHandler())
                .process(e -> {
                    Map<String, String> jdbcParams = copyNonNullValues(e.getMessage().getBody(Map.class));
                    jdbcParams.put("toimiala", e.getMessage().getHeader("toimiala", String.class));
                    jdbcParams.put("fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                    setJdbcParamsSqlValsAndOriginalBody(e, jdbcParams);
                })
                //.transacted()
                .setBody(simple(
                        "INSERT INTO %s (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})".formatted(dbTable)))
                .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .setBody(exchangeProperty("originalBody"));
    }

    public void setSqlValNamesAndNamedParams(Exchange e, Map<String, String> jdbcParams) {
        Set<String> keySet = jdbcParams.keySet();
        e.setProperty("sqlValNames", String.join(",", keySet));
        e.setProperty("sqlNamedParams", keySet.stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
    }

    public void setJdbcParamsSqlValsAndOriginalBody(Exchange e, Map<String, String> jdbcParams) {
        e.setProperty("originalBody", e.getMessage().getBody());
        e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
        setSqlValNamesAndNamedParams(e, jdbcParams);
    }

    public Map<String, String> copyNonNullValues(Map<String, String> fromMap) {
        Map<String, String> nonNullMap = new HashMap<>();
        for (Map.Entry<String, String> keyVal : fromMap.entrySet()) {
            if (keyVal.getValue() != null) {
                nonNullMap.put(keyVal.getKey(), keyVal.getValue());
            }
        }
        return nonNullMap;
    }
}
