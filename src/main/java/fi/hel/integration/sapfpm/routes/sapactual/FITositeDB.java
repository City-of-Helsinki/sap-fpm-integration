package fi.hel.integration.sapfpm.routes.sapactual;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class FITositeDB extends RouteBuilder {

    @Inject
    FITositeInRouteBuilder tositeInRoute;

    @Override
    public void configure() throws Exception {
        from("direct:init-tositerivi-db")
                .setProperty("originalBody", body())
            .setBody(constant("""
CREATE TABLE IF NOT EXISTS TOSITERIVI(
  id bigint NOT NULL AUTO_INCREMENT,
  createdTimestamp TIMESTAMP default now() not null,
  fileName varchar(255) not null,
  toimiala varchar(255) not null,
""" + Arrays.stream(tositeInRoute.createCsvHeader()).map(csvHead -> {
    if (csvHead.equals("BUKRS") || csvHead.equals("BELNR") || csvHead.equals("GJAHR") || csvHead.equals("POPER")) {
        return csvHead + " varchar(255) not null";
    } else {
        return csvHead + " varchar(255) default ''";
    }
}).collect(Collectors.joining(", ")) +
"""
,
primary key (id),
CONSTRAINT TOSITERIVIUNIQUE UNIQUE NULLS NOT DISTINCT (
filename,
toimiala,
""" +
    String.join(",", tositeInRoute.createCsvHeader()) +
"""
)
);

CREATE TABLE IF NOT EXISTS SAPFILE(
  createdTimestamp TIMESTAMP default now() not null,
  processedTimestamp TIMESTAMP,
  filename varchar(255) not null,
  toimiala varchar(255) not null,
  primary key (filename)
);
"""
            ))
                .to("jdbc:sapactual")
                .setBody(exchangeProperty("originalBody"));

        from("direct:insert-tositerivi-into-db")
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
           //.log("Inserted, updated: ${headers.CamelJdbcUpdateCount}")
            .onException(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple("${exception.message} contains 'Unique index or primary key violation'"))
                    .log("Failed to insert tositerivi into the db due to a duplicate in ${headers.CamelFileName}!")
                    .handled(true)
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-all-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT(TOIMIALA, POPER, GJAHR) FROM TOSITERIVI"))
                .to("jdbc:sapactual?useHeadersAsParameters=true");

        from("direct:fetch-tositerivit-from-db-by-year-and-month")
            .setBody(constant("SELECT * FROM TOSITERIVI WHERE TOIMIALA = :?toimiala AND GJAHR = :?GJAHR AND POPER = :?POPER " +
                    "ORDER BY FILENAME DESC")) // latest first
            // outputType=StreamList split(body()).streaming()
                .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch size: ${body.size()}");
    }
}
