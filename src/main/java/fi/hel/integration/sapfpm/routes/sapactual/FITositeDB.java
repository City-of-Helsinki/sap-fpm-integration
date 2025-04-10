package fi.hel.integration.sapfpm.routes.sapactual;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.hel.integration.sapfpm.routes.sapactual.FITositeInRouteBuilderSteps.createTositeCsvDataFormat;

public class FITositeDB extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:init-tositerivi-db")
                .setProperty("originalBody", body())
            .setBody(constant("""
CREATE TABLE IF NOT EXISTS TOSITERIVI(
  id bigint NOT NULL AUTO_INCREMENT,
  createdTimestamp TIMESTAMP default now() not null,
  fileName varchar(255) not null,
BUKRS varchar(255) not null,
BELNR varchar(255) not null,
CO_BELNR varchar(255) default '',
GJAHR varchar(255) not null,
POPER varchar(255) not null,
BLART varchar(255) default '',
BLDAT varchar(255) default '',
BUDAT varchar(255) default '',
CPUDT varchar(255) default '',
TCODE varchar(255) default '',
XBLNR varchar(255) default '',
KUNNR varchar(255) default '',
LIFNR varchar(255) default '',
LIFNR_NAME1 varchar(255) default '',
EBELN varchar(255) default '',
Attachment varchar(255) default '',
BUZEI varchar(255) default '',
CO_BUZEI varchar(255) default '',
RACCT varchar(255) default '',
RCNTR varchar(255) default '',
PRCTR varchar(255) default '',
RFAREA varchar(255) default '',
AUFNR varchar(255) default '',
PS_PSPID varchar(255) default '',
RASSC varchar(255) default '',
SEGMENT varchar(255) default '',
SGTXT varchar(255) default '',
DRCRK varchar(255) default '',
MWSKZ varchar(255) default '',
VAT_PERCENT varchar(255) default '',
HSL varchar(255) default '',
PPRCTR varchar(255) default '',
MATNR varchar(255) default '',
EBELP varchar(255) default '',
LAST_CHANGE_DATETIME varchar(255) default '',
AUGBL varchar(255) default '',
primary key (id),
CONSTRAINT TOSITERIVIUNIQUE UNIQUE NULLS NOT DISTINCT (
filename,
BUKRS,
BELNR,
CO_BELNR,
GJAHR,
POPER,
BLART,
BLDAT,
BUDAT,
CPUDT,
TCODE,
XBLNR,
KUNNR,
LIFNR,
LIFNR_NAME1,
EBELN,
Attachment,
BUZEI,
CO_BUZEI,
RACCT,
RCNTR,
PRCTR,
RFAREA,
AUFNR,
PS_PSPID,
RASSC,
SEGMENT,
SGTXT,
DRCRK,
MWSKZ,
VAT_PERCENT,
HSL,
PPRCTR,
MATNR,
EBELP,
LAST_CHANGE_DATETIME,
AUGBL)
);

CREATE TABLE IF NOT EXISTS SAPFILE(
  createdTimestamp TIMESTAMP default now() not null,
  processedTimestamp TIMESTAMP,
  filename varchar(255) not null,
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
                jdbcParams.put("fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                e.setProperty("originalBody", body);
                e.getMessage().setHeader(JdbcConstants.JDBC_PARAMETERS, jdbcParams);
                e.setProperty("sqlValNames", String.join(",", jdbcParams.keySet()));
                e.setProperty("sqlNamedParams", jdbcParams.keySet().stream().map(k -> ":?" + k).collect(Collectors.joining(",")));
            })
            .setBody(simple(
                    "INSERT INTO TOSITERIVI (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
           //.log("Inserted, updated: ${headers.CamelJdbcUpdateCount}")
            .onException(Exception.class)
                .log("Failed to insert tositerivi into the db!")
                .handled(true)
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-all-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT(POPER, GJAHR) FROM TOSITERIVI"))
                .to("jdbc:sapactual?useHeadersAsParameters=true");

        from("direct:fetch-tositerivit-from-db-by-year-and-month")
            .setBody(constant("SELECT * FROM TOSITERIVI WHERE GJAHR = :?GJAHR AND POPER = :?POPER"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch size: ${body.size()}");
    }
}
