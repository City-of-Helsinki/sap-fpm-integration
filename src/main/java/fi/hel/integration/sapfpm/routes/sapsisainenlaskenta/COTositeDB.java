package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import fi.hel.integration.sapfpm.tositecommon.TositeDbCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class COTositeDB extends TositeDbCommon {

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

        buildFileAndContentsDbRoute("direct:insert-cotosite-file-and-contents-into-db",
            "insertCoTositeFileAndContents",
        "direct:insert-sapfile-into-db",
        "direct:insert-cotosite-and-rivit-into-db");

        buildInsertReceiptAndLinesIntoDb("direct:insert-cotosite-and-rivit-into-db",
                "direct:insert-cotosite-into-db",
            "direct:insert-cotositerivi-into-db");

        from("direct:insert-cotosite-into-db").routeId("insertCoTositeIntoDb")
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
                    setJdbcParamsSqlValsAndOriginalBody(e, jdbcParams);
                })
                .setBody(simple(
                        "INSERT INTO COTOSITE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
                .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .setBody(exchangeProperty("originalBody"));

        from("direct:insert-cotositerivi-into-db").routeId("insertCoTositeRiviIntoDb")
                .errorHandler(noErrorHandler())
            .setProperty("DB_TABLE", constant("COTOSITERIVI"))
            .to("direct:insert-tosite-or-cotosite-rivi-into-db");

        from("direct:fetch-all-cotosite-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT toimiala, PERIO, GJAHR FROM COTOSITERIVI"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("cotosite all years and months: ${body}");

        from("direct:fetch-cotosite-years-and-months-count-from-db")
            .onException(Exception.class)
                .continued(true) // so originalBody is set
            .end()
            .setProperty("originalBody", body())
            .setBody(constant("SELECT COUNT(id) FROM COTOSITERIVI WHERE toimiala = :?toimiala AND GJAHR = :?GJAHR AND PERIO = :?PERIO"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("cotosite by year and month count: ${body}")
            .setBody(exchangeProperty("originalBody"));

        String cotositeRiviSelect = "SELECT * FROM COTOSITERIVI WHERE toimiala = :?toimiala AND GJAHR = :?GJAHR AND PERIO = :?PERIO ";
        String cotositeRiviSelectOrderBy = " ORDER BY id DESC LIMIT :?pageLimit";
        from("direct:fetch-cotositerivit-from-db-by-year-and-month")
            .choice().when(header("lastId").isNull())
                .setBody(constant(cotositeRiviSelect + cotositeRiviSelectOrderBy))
            .otherwise()
                .setBody(constant(cotositeRiviSelect + " AND id < :?lastId" + cotositeRiviSelectOrderBy))
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch done, size: ${body.size}, lastId: ${headers.lastId}");
    }
}
