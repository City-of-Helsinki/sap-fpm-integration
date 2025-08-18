package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import org.apache.camel.builder.RouteBuilder;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.jboss.logging.Logger;

import static fi.hel.integration.sapfpm.IdempotentRepositoryProvider.idempotentRepositoryParam;



//Sapista FPM:lle:
// ORD_OUT -> SAPSISTILAUS: sisäiset tilaukset, uudet ja muuttuneet yksi tiedosto per sisäinen tilaus
// PART_OUT -> SAPKUMPPANI: Kumppanit (kumppanitulosyksiköt) kaikki kumppanit, yksi tiedosto per päivä
// WBS_OUT -> SAPPROJEKTI: Projektit ja projektin rakenneosat, uudet ja muuttuneet yksi tiedosto per päivä, tiedosto sisältää kaikki uudet ja muuttuneet projektit
// ID022_FI_TOSITE -> SAPACTUAL
// CO_OUT -> ID166_CO_TOSITE_ -> SAPSISAINENLASKENTA
// n. 21:00 UTC / 00:00 suomen aikaa tositetiedostot siirtyvät ftp:lle

@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    public static String buildInParamsWithExclude(String excludeRegexp, String sortBy) {
        return "includeExt=xml&exclude=" + excludeRegexp + "&" +
                "noop=true&" +
                "idempotent=true&idempotentEager=false&" + // wait until file complete until removed from idempotent repo
                "preSort=true&sortBy=" + sortBy;
    }

    public static String buildFtpIn(String protocol, String user, String password, String host, String ftpDir, String filePrefix, String passiveMode, String sortBy) {
        return "%s://%s@%s/%s?password=%s&".formatted(protocol, user, host, ftpDir, password) +
                buildInParamsWithExclude("RAW(^(?!" + filePrefix + ").+)", sortBy) +
                "&passiveMode=" + passiveMode +
                "&autoCreate=false&disconnect=true&" +
                "localWorkDirectory=/tmp&" +
                "maximumReconnectAttempts=9999&" +
                "bridgeErrorHandler=true&" +
                "timeout=120000&" +
                "delay=30000"; // 30 seconds delay in between polls
    }

    public static String buildFtpIn(String perusOrToteumat, String toimiala, String ftpDir, String filePrefix, String sortBy) {
        return buildFtpIn("ftp", "{{%s.ftp.%s.user}}".formatted(toimiala, perusOrToteumat),
                "{{%s.ftp.%s.password}}".formatted(toimiala, perusOrToteumat),
                "{{%s.ftp.host}}".formatted(toimiala),
                ftpDir,
                filePrefix, "{{%s.ftp.passiveMode}}".formatted(toimiala), sortBy) + idempotentRepositoryParam(perusOrToteumat, toimiala);
    }

    public static String buildS4SftpIn(String perusOrToteumat, String toimiala, String ftpDir, String filePrefix, String sortBy) {
        return buildFtpIn("sftp", "{{%s.sftp.%s.user}}".formatted(toimiala, perusOrToteumat),
                "{{%s.sftp.%s.password}}".formatted(toimiala, perusOrToteumat),
                "{{%s.sftp.host}}".formatted(toimiala),
                ftpDir,
                filePrefix, "{{%s.sftp.passiveMode}}".formatted(toimiala), sortBy) + idempotentRepositoryParam(perusOrToteumat, toimiala);
    }

    public static String buildFtpPerustiedotIn(String toimiala, String ftpDir, String filePrefix, String sortBy) {
        return buildFtpIn("perustiedot", toimiala, ftpDir, filePrefix, sortBy);
    }

    public static String buildS4SFtpPerustiedotIn(String toimiala, String ftpDir, String filePrefix, String sortBy) {
        return buildS4SftpIn("perustiedot", toimiala, ftpDir, filePrefix, sortBy);
    }

    public static String buildFtpToteumatIn(String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("toteumat", toimiala, ftpDir, filePrefix, "file:name");
    }

    public static String buildS4SFtpToteumatIn(String toimiala, String ftpDir, String filePrefix) {
        return buildS4SftpIn("toteumat", toimiala, ftpDir, filePrefix, "file:name");
    }

    public static String buildFtpCoToteumatIn(String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("co_toteumat", toimiala, ftpDir, filePrefix, "file:name");
    }

    public static String buildLocalIn(String perusOrToteumat, String toimiala, String filePrefix) {
        return buildLocalIn(perusOrToteumat, toimiala, filePrefix, "file:name");
    }

    public static String buildLocalIn(String perusOrToteumat, String toimiala, String filePrefix, String sortBy) {
        return buildInParamsWithExclude("RAW(^(?!" + filePrefix + ").+)", sortBy) +
                idempotentRepositoryParam(perusOrToteumat, toimiala);
    }

    public static String buildLocalPerustiedotIn(String toimiala, String filePrefix) {
        return buildLocalPerustiedotIn(toimiala, filePrefix, "file:name");
    }

    public static String buildLocalPerustiedotIn(String toimiala, String filePrefix, String sortBy) {
        return buildLocalIn("perustiedot", toimiala, filePrefix, sortBy);
    }

    public static String buildLocalCoToteumatIn(String toimiala, String filePrefix) {
        return buildLocalIn("co_toteumat", toimiala, filePrefix);
    }

    public static String buildLocalToteumatIn(String toimiala, String filePrefix) {
        return buildLocalIn("toteumat", toimiala, filePrefix);
    }

    @Inject
    Logger log;

    @Inject
    IsConfigEnabled mainConfig;


    @Override
    public void configure() throws Exception {
        from("direct:unmarshal-xml").unmarshal().jacksonXml();

        if (mainConfig.localOrFTPPerustiedotEnabled()) {
            buildLocalFileAppendingRoute();
        }

        from("direct:any-file-out").id("AnyFileOut")
            .choice()
                .when(simple("${exchangeProperty.outDir} == null"))
                .setProperty("outDir", constant("out"))
            .end()
            .choice()
                .when(simple("${exchangeProperty.fileExist} == null"))
                .setProperty("fileExist", constant("Override"))
            .end()
            .onException(Exception.class)
                .maximumRedeliveries(10).redeliveryDelay(1000)
                .log("Failed to write the file to Azure: ${exchangeProperty.CamelExceptionCaught}")
            .end()
            .toD("file:${exchangeProperty.outDir}?fileExist=${exchangeProperty.fileExist}")
            .choice().when(simple("${exchangeProperty.processedFiles} != null"))
                .log("Processed ${exchangeProperty.processedFiles.size()} files: ")
                .log("${exchangeProperty.processedFiles}")
            .end()
            .choice()
            .when(simple("${exchangeProperty.fileExist} == 'Append'"))
                //.log("Appended ${exchangeProperty.originalFileName} to ${exchangeProperty.outDir}/${headers.CamelFileName}")
            .otherwise()
                .log("Written ${exchangeProperty.outDir}/${headers.CamelFileName}");
    }

    public void buildLocalFileAppendingRoute() {
        from("direct:append-csv-to-main-csv").id("append-csv-to-main-csv")
                .pollEnrich().simple("file:${exchangeProperty.wipFileDir}?fileName=RAW(${body})&autoCreate=false&noop=true&idempotent=false")
                .aggregationStrategy((oldExchange, readFileExchange) -> {
                    if (readFileExchange == null) {
                        log.error("READ FILE IS NULL!");
                        oldExchange.getMessage().setBody("");
                    } else {
                        oldExchange.setProperty("originalFileName", readFileExchange.getMessage().getHeader(FileConstants.FILE_NAME));
                        oldExchange.getMessage().setBody(readFileExchange.getMessage().getBody());
                    }
                    return oldExchange;
                })
                .to("direct:any-file-out");

    }
}
