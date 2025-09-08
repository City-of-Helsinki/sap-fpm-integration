package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.tositecommon.TositeDbCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class COTositeDB extends TositeDbCommon {

    @Inject
    COTositeInRouteBuilder coTtositeInRoute;

    @Inject
    IsConfigEnabled mainConfig;

    // cotosite unique by BUKRS, BELNR, GJAHR, PERIO
    // inserted in transaction with lines
    // to make sure only 1 exists
    // to allow from multiple files: PRIMARY KEY (fileName, toimiala, BUKRS, ...
    @Override
    public void configure() throws Exception {
        if (!mainConfig.localOrFTPCoToteumatEnabled()) {
            return;
        }

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
        .setBody(constant("""
CREATE TABLE IF NOT EXISTS COTOSITESAPFILE(
  processedTimestamp TIMESTAMP default CURRENT_TIMESTAMP not null,
  fileName varchar(255) not null,
  toimiala varchar(20) not null,
  primary key (fileName)
);
"""))
        .to("jdbc:sapactual")
        .setBody(exchangeProperty("originalBody"));

        buildFileAndContentsDbRoute("direct:insert-cotosite-file-and-contents-into-db",
            "insertCoTositeFileAndContents",
        "direct:insert-cotositesapfile-into-db",
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
                .doTry()
                    .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
                    .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                    .setBody(exchangeProperty("originalBody"))
                .doCatch(SQLIntegrityConstraintViolationException.class)
                    .onWhen(simple(DUPLICATE_ENTRY_EXCEPTION_MESSAGE))
                    .process(e -> {
                        Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
                        if (jdbcParams != null && !jdbcParams.isEmpty()) {
                            log.info("Failed to insert co %s %s into db due to a duplicate in %s".formatted(
                                    jdbcParams.get("BUKRS"), jdbcParams.get("BELNR"),
                                    e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)
                            ));
                        } else {
                            log.info("Failed to insert co receipt into the db due to a duplicate in %s".formatted(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)));
                        }
                    })
                    .process(e -> e.getMessage().setBody(null))
                    .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .end();

        buildInsertRiviIntoDb("direct:insert-cotositerivi-into-db", "insertCoTositeRiviIntoDb", "COTOSITERIVI");

        from("direct:insert-cotositesapfile-into-db").routeId("insertCoTositeSapFileIntoDb")
            .errorHandler(noErrorHandler()) // propagate errors to calling route
            .process(e -> {
                Map<String, String> jdbcParams = Map.of(
                        "fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class),
                        "toimiala", e.getMessage().getHeader("toimiala", String.class)
                );
                setJdbcParamsSqlValsAndOriginalBody(e, jdbcParams);
            })
            .setBody(simple(
                    "INSERT INTO COTOSITESAPFILE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-all-cotosite-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT PERIO, GJAHR FROM COTOSITERIVI WHERE toimiala = :?toimiala ORDER BY GJAHR DESC, PERIO DESC"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("${headers.toimiala} cotosite all years and months: ${body}");

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
