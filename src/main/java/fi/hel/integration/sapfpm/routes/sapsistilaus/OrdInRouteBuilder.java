package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.aggregationstrategy.ProcessedLinesAggregationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;

// /203/ORD_OUT_*.xml _> SAPSISTILAUS
// kaikki filut yhteen tiedostoon?
// Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto)
// tiedosto per sisäinen tilaus <- saattaa olla myös useita per tiedosto
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class OrdInRouteBuilder extends RouteBuilder {
    @Inject
    Logger log;

    final static String IN_FILE_PREFIX = "ORD_OUT_";

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

    @Override
    public void configure() throws Exception {
        // include or antInclude only works with 1 file at a time
        from("file:in?" + buildInParams(IN_FILE_PREFIX)).id("OrdIn")
                .log("ORD ${headers.CamelFileName}")
            .to("direct:unmarshal-xml-and-process-ord")
            .aggregate(new ProcessedLinesAggregationStrategy()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPSISTILAUS.csv"))
            .to("direct:ord-csv-out");

        from("direct:unmarshal-xml-and-process-ord").routeId("ORDUnmarshalXMLAndProcess")
            .unmarshal().jacksonXml().to("direct:process-ord");

        // ORD_OUT_167_SOTE*.xml
        // ORD_OUT_138_*.xml
        // --> SAPSISTILAUS.csv
        from("direct:process-ord").id("ProcessOrd")
            .setBody(e -> extractValuesFromIDOC(e,  "ZHKI_TARSISTILAUKSET", this::extractValues));

        from("direct:ord-csv-out").routeId("ordCsvOut")
            .marshal(ordCsvDataFormat)
            .to("direct:any-file-out");

    }

}

