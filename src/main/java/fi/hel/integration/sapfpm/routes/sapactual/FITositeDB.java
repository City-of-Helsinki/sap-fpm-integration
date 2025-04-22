package fi.hel.integration.sapfpm.routes.sapactual;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class FITositeDB extends RouteBuilder {

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
  toimiala varchar(255) not null,
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
  createdTimestamp TIMESTAMP default CURRENT_TIMESTAMP not null,
  processedTimestamp TIMESTAMP,
  filename varchar(255) not null,
  toimiala varchar(10) not null,
  primary key (filename)
);
                """))
                .to("jdbc:sapactual")
                .setBody(exchangeProperty("originalBody"));

        from("direct:insert-tositerivit-into-db")
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple("${exception.message} contains 'Duplicate entry'"))
                .log("Failed to insert tosite ${headers.BUKRS} ${headers.BELNR} into the db due to a duplicate in ${headers.CamelFileName}!")
                .handled(true)
            .end()
            .process(e -> {
                List<LinkedHashMap<String, Object>> receiptMetadata =  e.getMessage().getBody(List.class);
                receiptMetadata.stream().findFirst().ifPresentOrElse(firstReceipt ->
                                e.getMessage().setHeader("firstReceipt", firstReceipt)
                        , () -> e.getMessage().removeHeader("firstReceipt"));
            })
            .transacted()
            .to("direct:insert-tosite-into-db")
            .split(body())
                .to("direct:insert-tositerivi-into-db")
            .end()
            .log("inserted all into db ${headers.toimiala}/${headers.CamelFileName} ${body.size()}");

        from("direct:insert-tosite-into-db")
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

        from("direct:insert-tositerivi-into-db")
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
                    "INSERT INTO TOSITERIVI (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-all-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT TOIMIALA, POPER, GJAHR FROM TOSITERIVI"))
                .to("jdbc:sapactual?useHeadersAsParameters=true");

        from("direct:fetch-tositerivit-from-db-by-year-and-month")
            .setBody(constant("SELECT * FROM TOSITERIVI WHERE TOIMIALA = :?toimiala AND GJAHR = :?GJAHR AND POPER = :?POPER " +
                    "ORDER BY fileName DESC")) // latest first
            // outputType=StreamList split(body()).streaming()
                .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch size: ${body.size()}");
    }
}
