package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.jboss.logging.Logger;

import static fi.hel.integration.sapfpm.IdempotentRepositoryProvider.idempotentRepositoryParam;



/*Sapista FPM:lle:
BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.

PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin*/

// ORD_OUT -> sisäisen tilauksen käsittelyyn SAPSISTILAUS
// ID167/203   ORD_OUT* Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per sisäinen tilaus

// PART_OUT -> SAPKUMPPANI
// ID167/210   PART_OUT* Kumppanit (kumppanitulosyksiköt) kaikki kumppanit, yksi tiedosto per päivä

// WBS_OUT -> SAPPROJEKTI
// // ID167/204   WBS_OUT* Projektit ja projektin rakenneosat, uudet ja muuttuneet (myös esim. lukitustiedosto) yksi tiedosto per päivä tiedosto sisältää kaikki uudet ja muuttuneet projektit

// ID022_FI_TOSITE -> SAPACTUAL

// CO_OUT -> ID166_CO_TOSITE_ ->

// Toimintoalueell ei nähdä tarvetta, se ei ole käytössä Palkella (eikä Kaskolla) ja SOTEPE voi ylläpitää toistaiseksi käsin (jos hekään oikeasti käyttävät budjetoinnissa toimintoaluetta)
// Samoin en näe tarvetta pääkirjatililataukselle, sen voi viedä suoraan FPM yhtenä latauksena sillä muutoksia tulee harvakseltaan


// n. 21:00 UTC / 00:00 suomen aikaa tositetiedostot siirtyvät ftp:lle

@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    public static String buildInParamsWithExclude(String excludeRegexp) {
        return "includeExt=xml&exclude=" + excludeRegexp + "&" +
            "noop=true&" +
            "idempotent=true&idempotentEager=false&" + // wait until file complete until removed from idempotent repo
            "preSort=true&sortBy=file:name";
        //&charset=ISO-8859-1";
    }

    // TODO: move to mainConfig
    public static String buildFtpParams(String filePrefix, String passiveMode) {
        return buildInParamsWithExclude("RAW(^(?!" + filePrefix + ").+)") +
                "&passiveMode=" + passiveMode +
                "&autoCreate=false&disconnect=true&" +
                "localWorkDirectory=/tmp&" +
                "maximumReconnectAttempts=9999&" +
                "bridgeErrorHandler=true&" +
                "timeout=120000";
    }

    public static String buildFtpIn(String user, String password, String host, String ftpDir, String filePrefix, String passiveMode) {
        return "ftp://%s@%s/%s?password=%s&".formatted(user, host, ftpDir, password) +
                buildFtpParams(filePrefix, passiveMode);
    }

    public static String buildFtpIn(String perusOrToteumat, String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("{{%s.ftp.%s.user}}".formatted(toimiala, perusOrToteumat),
                "{{%s.ftp.%s.password}}".formatted(toimiala, perusOrToteumat),
                "{{%s.ftp.host}}".formatted(toimiala),
                ftpDir,
                filePrefix, "{{%s.ftp.passiveMode}}".formatted(toimiala)) + idempotentRepositoryParam(perusOrToteumat, toimiala);
    }

    public static String buildFtpPerustiedotIn(String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("perustiedot", toimiala, ftpDir, filePrefix);
    }

    public static String buildFtpToteumatIn(String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("toteumat", toimiala, ftpDir, filePrefix);
    }

    public static String buildFtpCoToteumatIn(String toimiala, String ftpDir, String filePrefix) {
        return buildFtpIn("co_toteumat", toimiala, ftpDir, filePrefix);
    }

    public static String buildLocalIn(String perusOrToteumat, String toimiala, String filePrefix) {
        return buildInParamsWithExclude("RAW(^(?!" + filePrefix + ").+)") +
                idempotentRepositoryParam(perusOrToteumat, toimiala);
    }

    public static String buildLocalPerustiedotIn(String toimiala, String filePrefix) {
        return buildLocalIn("perustiedot", toimiala, filePrefix);
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

        //SOTEPE ID167 perustiedot
        //  Samoin en näe tarvetta pääkirjatililataukselle, sen voi viedä suoraan FPM yhtenä latauksena sillä muutoksia tulee harvakseltaan
/*ID167/200   GLMAST*   Pääkirjatilit, uudet ja muuttuneet, yksi tiedosto per pääkirjatili

ID167/202   PRC_OUT* Tulosyksiköt, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per tulosyksikkö
ID167/203   ORD_OUT* Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per sisäinen tilaus
ID167/204   WBS_OUT* Projektit ja projektin rakenneosat, uudet ja muuttuneet (myös esim. lukitustiedosto) yksi tiedosto per päivä tiedosto sisältää kaikki uudet ja muuttuneet projektit
ID167/210   PART_OUT* Kumppanit (kumppanitulosyksiköt) kaikki kumppanit, yksi tiedosto per päivä

Toimintoalueell ei nähdä tarvetta, se ei ole käytössä Palkella (eikä Kaskolla) ja SOTEPE voi ylläpitää toistaiseksi käsin (jos hekään oikeasti käyttävät budjetoinnissa toimintoaluetta)
ID167/213  H_FUNC_OUT*  Toimintoalueet, kaikki toimintoalueet, yksi tiedosto per päivä*/

        // SOTEPE ID022 tositteet

        // KASKO-ID137 perustiedot
        // KASKO ID023 toteumatositteet

        // PALKE ID166 tyhjä ??? <-- toteumatositteet, palkelle myös CO toteutamatositteet
        // PALKE ID138 perustiedot
        // PALKE ID025 toteumatositteet
    }

    public void buildLocalFileAppendingRoute() {
        from("direct:append-csv-to-main-csv").id("append-csv-to-main-csv")
                .pollEnrich().simple("file:${exchangeProperty.wipFileDir}?fileName=RAW(${body})&autoCreate=false")
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
