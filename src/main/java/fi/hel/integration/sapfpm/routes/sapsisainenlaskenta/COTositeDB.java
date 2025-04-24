package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class COTositeDB extends RouteBuilder {

    @Inject
    COTositeInRouteBuilder coTtositeInRoute;

    // cotosite unique by BUKRS, BELNR, GJAHR, PERIO
    // inserted in transaction with lines
    // to make sure only 1 exists
    // to allow from multiple files: PRIMARY KEY (fileName, toimiala, BUKRS, ...
    @Override
    public void configure() throws Exception {
        from("direct:init-cotositerivi-db")
            .to("direct:init-tositerivi-db") // TODO: move into db init
                .setProperty("originalBody", body())
                .setBody(constant("""
CREATE TABLE IF NOT EXISTS COTOSITE(
fileName varchar(255) not null,
toimiala varchar(10) not null,
BUKRS varchar(50) not null,
BELNR varchar(50) not null,
GJAHR varchar(10) not null,
PERIO varchar(10) not null,
PRIMARY KEY (toimiala, BUKRS, BELNR, GJAHR, PERIO)
);
            """))
                .to("jdbc:sapactual")
                .setBody(constant("""
CREATE TABLE IF NOT EXISTS COTOSITERIVI(
  id bigint NOT NULL AUTO_INCREMENT,
  createdTimestamp TIMESTAMP default now() not null,
  fileName varchar(255) not null,
  toimiala varchar(10) not null,
""" + Arrays.stream(coTtositeInRoute.createCsvHeader()).map(csvHead -> {
            if (csvHead.equals("BUKRS") || csvHead.equals("BELNR") || csvHead.equals("GJAHR") || csvHead.equals("PERIO")) {
                return csvHead + " varchar(50) not null";
            } else {
                return csvHead + " varchar(255) default null";
            }
        }).collect(Collectors.joining(", ")) +
            ",primary key (id)" +
            ");"
        ))
        .to("jdbc:sapactual")
        .setBody(exchangeProperty("originalBody"));

        from("direct:insert-cotosite-file-and-contents-into-db")
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple("${exception.message} contains 'Duplicate entry'"))
                .log("Failed to insert file ${headers.CamelFileName} into the db, already processed!")
                .handled(true)
            .end()
            .log("receipts to insert: ${body.size()}")
            .to("direct:insert-sapfile-into-db")
            .split(body())
                .log("receipt metadata to insert: ${body.size()}")
                .to("direct:insert-cotositerivit-into-db")
            .end();

        from("direct:insert-cotositerivit-into-db")
                .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple("${exception.message} contains 'Duplicate entry'"))
                .log("Failed to insert cotosite ${headers.BUKRS} ${headers.BELNR} into the db due to a duplicate in ${headers.CamelFileName}!")
                .handled(true)
                .end()
                .process(e -> {
                    List<LinkedHashMap<String, Object>> receiptMetadata =  e.getMessage().getBody(List.class);
                    receiptMetadata.stream().findFirst().ifPresentOrElse(firstReceipt ->
                                    e.getMessage().setHeader("firstReceipt", firstReceipt)
                            , () -> e.getMessage().removeHeader("firstReceipt"));
                })
                .transacted("PROPAGATION_REQUIRES_NEW")
                .to("direct:insert-cotosite-into-db")
                .split(body())
                .to("direct:insert-cotositerivi-into-db")
                .end()
                .log("inserted all into db ${headers.toimiala}/${headers.CamelFileName} ${body.size()}");

        from("direct:insert-cotosite-into-db")
                .errorHandler(noErrorHandler()) // propagate errors to calling route
                .process(e -> {
                    Map<String, String> firstReceipt = e.getMessage().getHeader("firstReceipt", Map.class);
                    Map<String, String> jdbcParams = Map.of(
                            "fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class),
                            "toimiala", e.getMessage().getHeader("toimiala", String.class),
                            "BUKRS", firstReceipt.get("BUKRS"),
                            "BELNR", firstReceipt.get("BELNR"),
                            "GJAHR", firstReceipt.get("GJAHR"),
                            "PERIO", firstReceipt.get("PERIO")
                    );
                    e.setProperty("originalBody", e.getMessage().getBody());
                    e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
                    e.setProperty("sqlValNames", String.join(",", jdbcParams.keySet()));
                    e.setProperty("sqlNamedParams", jdbcParams.keySet().stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
                })
                .setBody(simple(
                        "INSERT INTO COTOSITE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
                .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .setBody(exchangeProperty("originalBody"));

        from("direct:insert-cotositerivi-into-db")
            .setProperty("DB_TABLE", constant("COTOSITERIVI"))
            .to("direct:insert-tosite-or-cotosite-rivi-into-db");


        from("direct:fetch-all-cotosite-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT TOIMIALA, PERIO, GJAHR FROM COTOSITERIVI"))
            .to("jdbc:sapactual?useHeadersAsParameters=true");

        from("direct:fetch-cotositerivit-from-db-by-year-and-month")
            .setBody(constant("SELECT * FROM COTOSITERIVI WHERE TOIMIALA = :?toimiala AND GJAHR = :?GJAHR AND PERIO = :?PERIO " +
                    "ORDER BY fileName DESC")) // latest first
            // outputType=StreamList split(body()).streaming()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch size: ${body.size()}");
    }
}
