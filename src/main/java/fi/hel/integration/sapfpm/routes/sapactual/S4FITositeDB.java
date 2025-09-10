package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.tositecommon.TositeDbCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class S4FITositeDB extends TositeDbCommon {

    @Inject
    IsConfigEnabled mainConfig;

    // s4 tosite rivi unique by BUKRS, BELNR, GJAHR, POPER, DOCLN
    @Override
    public void configure() throws Exception {
        // palke FTP toteumat needs to send both ECC and S4 data
        if (!mainConfig.localOrSFTPS4ToteumatEnabled() && !mainConfig.localOrFTPToteumatEnabled() && !mainConfig.palkeFTPToteumatEnabled()) {
            return;
        }

        from("direct:init-s4-tositerivi-db")
            .setProperty("originalBody", body())
            .setBody(constant("""
CREATE TABLE IF NOT EXISTS S4TOSITERIVI(
id bigint NOT NULL AUTO_INCREMENT,
createdTimestamp TIMESTAMP default now() not null,
fileName varchar(255) not null,
toimiala varchar(10) not null,
BUKRS varchar(50) not null,
BELNR varchar(50) not null,
CO_BELNR varchar(255) default null,
GJAHR varchar(10) not null,
POPER varchar(10) not null,
DOCLN varchar(50) not null,""" +
Stream.of("BLART", "BLDAT", "BUDAT", "CPUDT", "TCODE", "XBLNR", "KUNNR", "LIFNR", "LIFNR_NAME1",
                "EBELN", "Attachment", "BUZEI", "CO_BUZEI", "RACCT", "RCNTR", "PRCTR", "RFAREA", "AUFNR", "PS_PSPID", "RASSC", "SEGMENT", "SGTXT", "DRCRK", "MWSKZ",
                "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL", "AWTYP"
).map(csvVal -> csvVal + " varchar(255) default null").collect(Collectors.joining(", "))
                    // ALTER TABLE S4TOSITERIVI ADD COLUMN new_csv_val varchar(255) default null;
+ ", primary key (id), S4_ID varchar(255) as (CONCAT_WS( '_', toimiala, GJAHR, POPER, BUKRS, BELNR, DOCLN )) not null," +
                            "unique (S4_ID) );"
            ))
                .to("jdbc:sapactual")
                .setBody(constant("""
CREATE TABLE IF NOT EXISTS S4TOSITESAPFILE(
  processedTimestamp TIMESTAMP default CURRENT_TIMESTAMP not null,
  fileName varchar(255) not null,
  toimiala varchar(20) not null,
  primary key (fileName)
);
                """))
                .to("jdbc:sapactual")
                .setBody(exchangeProperty("originalBody"));

        buildFileAndContentsDbRoute("direct:insert-s4-tosite-file-and-contents-into-db",
                "insertS4TositeFileAndContents",
                "direct:insert-s4-tositesapfile-into-db",
                "direct:insert-s4-tositerivi-into-db");

        from("direct:insert-s4-tositerivi-into-db")
            .routeId("insertS4TositeRiviIntoDb")
            .errorHandler(noErrorHandler())
            .process(e -> {
                Map<String, String> jdbcParams = copyNonNullValues(e.getMessage().getBody(Map.class));
                jdbcParams.put("toimiala", e.getMessage().getHeader("toimiala", String.class));
                jdbcParams.put("fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                setJdbcParamsSqlValsAndOriginalBody(e, jdbcParams);
            })
            .setBody(simple(
                    "INSERT INTO S4TOSITERIVI (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .doTry()
                .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
                .removeHeader(JdbcConstants.JDBC_PARAMETERS)
                .setBody(exchangeProperty("originalBody"))
            .doCatch(SQLIntegrityConstraintViolationException.class)
                .onWhen(simple(DUPLICATE_ENTRY_EXCEPTION_MESSAGE))
                .process(e -> {
                    Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
                    if (jdbcParams != null && !jdbcParams.isEmpty()) {
                        log.info("Failed to insert %s %s %s into db due to a duplicate in %s".formatted(
                                jdbcParams.get("BUKRS"), jdbcParams.get("BELNR"), jdbcParams.get("DOCLN"),
                                e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)
                        ));
                    } else {
                        log.info("Failed to insert receipt line into the db due to a duplicate in %s".formatted(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)));
                    }
                })
               .process(e -> e.getMessage().setBody(null)) // TODO: check
               .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .end();

        from("direct:insert-s4-tositesapfile-into-db").routeId("insertS4TositeSapFileIntoDb")
            .errorHandler(noErrorHandler()) // propagate errors to calling route
            .process(e -> {
                Map<String, String> jdbcParams = Map.of(
                "fileName", e.getMessage().getHeader(FileConstants.FILE_NAME, String.class),
                "toimiala", e.getMessage().getHeader("toimiala", String.class)
                );
                setJdbcParamsSqlValsAndOriginalBody(e, jdbcParams);
            })
            .setBody(simple(
                    "INSERT INTO S4TOSITESAPFILE (${exchangeProperty.sqlValNames}) VALUES (${exchangeProperty.sqlNamedParams})"))
            .to("jdbc:sapactual?useHeadersAsParameters=true&resetAutoCommit=false")
            .removeHeader(JdbcConstants.JDBC_PARAMETERS)
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-s4-years-and-months-count-from-db")
            .onException(Exception.class)
                .continued(true) // so originalBody is set
            .end()
            .setProperty("originalBody", body())
            .setBody(constant("SELECT COUNT(id) FROM S4TOSITERIVI WHERE toimiala = :?toimiala AND GJAHR = :?GJAHR AND POPER = :?POPER"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("${headers.toimiala} s4 by year and month count: ${body}")
            .setBody(exchangeProperty("originalBody"));

        from("direct:fetch-all-s4-years-and-months-from-db")
            .setBody(constant("SELECT DISTINCT POPER, GJAHR FROM S4TOSITERIVI WHERE toimiala = :?toimiala ORDER BY GJAHR DESC, POPER DESC"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("${headers.toimiala} s4 all years and months: ${body}");

        String tositeRiviSelect = "SELECT * FROM S4TOSITERIVI WHERE toimiala = :?toimiala AND GJAHR = :?GJAHR AND POPER = :?POPER ";
        String tositeRiviSelectOrderBy = " ORDER BY id DESC LIMIT :?pageLimit";

        from("direct:fetch-s4-tositerivit-from-db-by-year-and-month")
            .routeId("fetchS4TositeRivitFromDbByYearAndMonth")
            .choice().when(header("lastId").isNull())
                .setBody(constant(tositeRiviSelect + tositeRiviSelectOrderBy))
            .otherwise()
                .setBody(constant(tositeRiviSelect + " AND id < :?lastId" + tositeRiviSelectOrderBy))
            .end()
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("db fetch done, size: ${body.size}, lastId: ${headers.lastId}");

        from("direct:fetch-latest-s4-createdtimestamp-from-db")
            .setBody(constant(
                    "SELECT DISTINCT createdTimestamp FROM S4TOSITERIVI WHERE toimiala = :?toimiala ORDER BY createdTimestamp DESC LIMIT 1"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("${headers.toimiala} latest createdTimestamp ${body}");

        from("direct:fetch-s4-changed-years-and-months-from-db")
            .process(e -> {
                Integer daysToSubtract = e.getMessage().getHeader("daysToSubtract", Integer.class);
                Timestamp latest = e.getMessage().getHeader("latestCreatedTimestamp", Timestamp.class);
                if (daysToSubtract == null) {
                    e.getMessage().setHeader("afterOrEqualTime", latest);
                } else {
                    e.getMessage().setHeader("afterOrEqualTime", Timestamp.from(latest.toInstant().minus(Duration.ofDays(daysToSubtract))));
                }
            })
            .setBody(constant(
            "SELECT DISTINCT POPER, GJAHR FROM S4TOSITERIVI WHERE toimiala = :?toimiala AND createdTimestamp >= :?afterOrEqualTime ORDER BY GJAHR DESC, POPER DESC"))
            .to("jdbc:sapactual?useHeadersAsParameters=true")
            .log("${headers.toimiala} s4 years and months changed at or after ${headers.afterOrEqualTime}: ${body}");

    }
}
