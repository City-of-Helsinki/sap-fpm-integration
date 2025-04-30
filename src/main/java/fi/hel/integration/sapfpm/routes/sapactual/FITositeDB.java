package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.tositecommon.TositeDbCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class FITositeDB extends TositeDbCommon {

    @Inject
    FITositeInRouteBuilder tositeInRoute;

    // tosite unique by BUKRS, BELNR, GJAHR, POPER
    // inserted in transaction with lines
    // to make sure only 1 exists
    // to allow from multiple files: PRIMARY KEY (fileName, toimiala, BUKRS, ...
    @Override
    public void configure() throws Exception {
        from("direct:init-tositerivi-db")
            .setProperty("originalBody", body())
            .setBody(constant("""
CREATE TABLE IF NOT EXISTS TOSITE(
fileName varchar(255) not null,
toimiala varchar(10) not null,
BUKRS varchar(50) not null,
BELNR varchar(50) not null,
GJAHR varchar(10) not null,
POPER varchar(10) not null,
PRIMARY KEY (toimiala, BUKRS, BELNR, GJAHR, POPER)
);
            """))
            .to("jdbc:sapactual")
            .setBody(constant("""
CREATE TABLE IF NOT EXISTS TOSITERIVI(
  id bigint NOT NULL AUTO_INCREMENT,
  createdTimestamp TIMESTAMP default now() not null,
  fileName varchar(255) not null,
  toimiala varchar(10) not null,
""" + Arrays.stream(tositeInRoute.createCsvHeader()).map(csvHead -> {
    if (csvHead.equals("BUKRS") || csvHead.equals("BELNR") || csvHead.equals("GJAHR") || csvHead.equals("POPER")) {
        return csvHead + " varchar(50) not null";
    } else {
        return csvHead + " varchar(255) default null";
    }
}).collect(Collectors.joining(", ")) +
"""
,
primary key (id)
""" +
");"
            ))
                .to("jdbc:sapactual")
                .setBody(constant("""
CREATE TABLE IF NOT EXISTS SAPFILE(
  processedTimestamp TIMESTAMP default CURRENT_TIMESTAMP not null,
  fileName varchar(255) not null,
  toimiala varchar(20) not null,
  primary key (fileName)
);
                """))
                .to("jdbc:sapactual")
                .setBody(exchangeProperty("originalBody"));

        buildFileAndContentsDbRoute("direct:insert-tosite-file-and-contents-into-db",
                "insertTositeFileAndContents",
                "direct:insert-sapfile-into-db",
                "direct:insert-tosite-and-rivit-into-db");

        buildInsertReceiptAndLinesIntoDb("direct:insert-tosite-and-rivit-into-db",
                "direct:insert-tosite-into-db",
                "direct:insert-tositerivi-into-db");

        from("direct:insert-tosite-into-db").routeId("insertTositeIntoDb")
                // TODO: catch and continued(true)
            .errorHandler(noErrorHandler()) // propagate errors to calling route
            .process(e -> {
                Map<String, String> firstReceipt = e.getMessage().getHeader("firstReceipt", Map.class);
                Map<String, String> jdbcParams = Map.of(
                        "fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class),
                        "toimiala", e.getMessage().getHeader("toimiala", String.class),
                        "BUKRS", firstReceipt.get("BUKRS"),
                        "BELNR", firstReceipt.get("BELNR"),
                        "GJAHR", firstReceipt.get("GJAHR"),
                        "POPER", firstReceipt.get("POPER")
                );
                e.setProperty("originalBody", e.getMessage().getBody());
                e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
                e.setProperty("sqlValNames", String.join(",", jdbcParams.keySet()));
                e.setProperty("sqlNamedParams", jdbcParams.keySet().stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
            })
            .setBody(simple(
                    "INSERT INTO TOSITE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:insert-tositerivi-into-db").routeId("insertTositeRiviIntoDb")
                .errorHandler(noErrorHandler())
            .setProperty("DB_TABLE", constant("TOSITERIVI"))
            .to("direct:insert-tosite-or-cotosite-rivi-into-db");

        from("direct:insert-tosite-or-cotosite-rivi-into-db")
                .routeId("insertTositeOrCoTositeRiviIntoDb")
            .errorHandler(noErrorHandler())
            .process(e -> {
                Map<String, String> body = e.getMessage().getBody(Map.class);
                Map<String, String> jdbcParams = new HashMap<>();
                for (Map.Entry<String, String> keyVal : body.entrySet()) {
                    if (keyVal.getValue() != null) {
                        jdbcParams.put(keyVal.getKey(), keyVal.getValue());
                    }
                }
                jdbcParams.put("toimiala", e.getMessage().getHeader("toimiala", String.class));
                jdbcParams.put("fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                e.setProperty("originalBody", body);
                e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
                e.setProperty("sqlValNames", String.join(",", jdbcParams.keySet()));
                e.setProperty("sqlNamedParams", jdbcParams.keySet().stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
            })
            .setBody(simple(
                    "INSERT INTO ${exchangeProperty.DB_TABLE} (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));


        from("direct:insert-sapfile-into-db").routeId("insertSapFileIntoDb")
            .errorHandler(noErrorHandler()) // propagate errors to calling route
            .process(e -> {
                Map<String, String> jdbcParams = Map.of(
                "fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class),
                "toimiala", e.getMessage().getHeader("toimiala", String.class)
                );
                e.setProperty("originalBody", e.getMessage().getBody());
                e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
                e.setProperty("sqlValNames", String.join(",", jdbcParams.keySet()));
                e.setProperty("sqlNamedParams", jdbcParams.keySet().stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
            })
            .setBody(simple(
                    "INSERT INTO SAPFILE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));


        from("direct:fetch-all-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT TOIMIALA, POPER, GJAHR FROM TOSITERIVI"))
                .to("jdbc:sapactual?useHeadersAsParameters=true");

        String tositeRiviSelect = "SELECT * FROM TOSITERIVI WHERE TOIMIALA = :?toimiala AND GJAHR = :?GJAHR AND POPER = :?POPER ";
        String tositeRiviSelectOrderBy = " ORDER BY id DESC LIMIT :?pageLimit";

        from("direct:fetch-tositerivit-from-db-by-year-and-month")
            .choice().when(header("lastId").isNull())
                .setBody(constant(tositeRiviSelect + tositeRiviSelectOrderBy))
            .otherwise()
                .setBody(constant(tositeRiviSelect + " AND id < :?lastId" + tositeRiviSelectOrderBy))
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch done, size: ${body.size}");
    }
}
