package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.aggregationstrategy.AggregateLinesWithoutStacking;
import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.config.PalkeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildFtpParams;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;

// /203/ORD_OUT_*.xml _> SAPSISTILAUS.csv
// kaikki filut yhteen tiedostoon?
// Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto)
// tiedosto per sisäinen tilaus <- saattaa olla myös useita per tiedosto
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class OrdInRouteBuilder extends RouteBuilder {
    @Inject
    Logger log;

    @Inject
    IsConfigEnabled mainConfig;

    @Inject
    PalkeConfig palkeConfig;

    final static String IN_FILE_PREFIX = "ORD_OUT_";
    final static String FTP_DIR = "203";

    CsvDataFormat ordCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
            "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
    });

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> ord = new LinkedHashMap<>(); // order matters
        ord.put("BUKRS", valuesLine.get("BUKRS")); // maksupiste
        ord.put("AUART", valuesLine.get("AUART")); // tilauslaji
        ord.put("AUFNR", valuesLine.get("AUFNR")); // tilausnumero
        ord.put("KTEXT", valuesLine.get("KTEXT")); // lyhytteksti
        ord.put("STTXT", valuesLine.get("STTXT")); // tilakoodit
        return ord;
    }

    public void buildMainRoute(String fileOrFtpIn, String id) {
        // include or antInclude only works with 1 file at a time
        from(fileOrFtpIn).id(id)
            .to("direct:unmarshal-and-process-ord")
            .aggregate(new AggregateLinesWithoutStacking()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPSISTILAUS.csv"))
            .to("direct:ord-csv-out");
    }

    public String buildFtpIn(String ala) {
        return "ftp://{{%s.ftp.user_perustiedot}}@{{%s.ftp.host}}/{{%s}}?password={{%s.ftp.password_perustiedot}}&".formatted(ala, ala, FTP_DIR, ala) +
                buildFtpParams(IN_FILE_PREFIX);
    }

    @Override
    public void configure() throws Exception {
        if (mainConfig.palkeFTPPerustiedotEnabled()) {
            buildMainRoute(buildFtpIn("palke"), "PalkeOrdFtpIn");
        }

        if (mainConfig.kaskoFTPPerustiedotEnabled()) {
            buildMainRoute(buildFtpIn("kasko"), "KaskoOrdFtpIn");
        }

        if (mainConfig.sotepeFTPPerustiedotEnabled()) {
            buildMainRoute(buildFtpIn("sotepe"), "SotepeOrdFtpIn");
        }

        if (mainConfig.localPerustiedotEnabled()) {
            buildMainRoute("file:in?" + buildInParams(IN_FILE_PREFIX), "OrdIn");
        }

        // TODO: get from config
        boolean disabled = false;
         if (disabled) return;

        from("direct:unmarshal-and-process-ord").routeId("ORDUnmarshalXMLAndProcess")
            .unmarshal().jacksonXml().to("direct:process-ord");

        from("direct:process-ord").id("ProcessOrd")
            .setBody(e -> extractValuesFromIDOC(e,  "ZHKI_TARSISTILAUKSET", this::extractValues));

        from("direct:ord-csv-out").routeId("ordCsvOut")
            .marshal(ordCsvDataFormat)
            .to("direct:any-file-out");

    }

}

