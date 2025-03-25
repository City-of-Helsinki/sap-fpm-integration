package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// /203/ORD_OUT_*.xml _> SAPSISTILAUS
// kaikki filut yhteen tiedostoon?
// Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto)
// tiedosto per sisäinen tilaus <- saattaa olla myös useita per tiedosto
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class OrdInRouteBuilder extends LoopingFileReader {
    @Inject
    Logger log;

    static final String POLL_ENRICH_IN = "file:ordIn";
    final static String IN_FILE_PREFIX = "ORD_OUT_";
    final static String AGGREGATED_PROPERTY = "partBody";

    CsvDataFormat ordCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
            "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
    });

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> commonValues, Map<String, Object> valuesLine) {
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

        createLoopingFileReaderRoute("ORD_IN", POLL_ENRICH_IN, IN_FILE_PREFIX, "direct:unmarshal-xml-and-process-ord",
                AGGREGATED_PROPERTY)
            .setHeader("CamelFileName", constant("SAPSISTILAUS.csv"))
            .log("writing to Azure ${headers.CamelFileName}")
            .to("direct:ord-csv-out");

        from("direct:unmarshal-xml-and-process-ord").routeId("ORDUnmarshalXMLAndProcess")
            .unmarshal().jacksonXml().to("direct:process-ord");

        // ORD_OUT_167_SOTE*.xml
        // ORD_OUT_138_*.xml
        // SAPSISTILAUS.csv
        from("direct:process-ord")
            .process(e -> {
                Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> commonValuesAndValues = extractValuesFromIDOC(e,  null, "ZHKI_TARSISTILAUKSET", this::extractValues);
                List<LinkedHashMap<String, Object>> valueLines = commonValuesAndValues.getItem2();
                List<LinkedHashMap<String, Object>> prevLines = e.getProperty(AGGREGATED_PROPERTY, List.class);
                if (prevLines == null) {
                    prevLines = valueLines;
                } else {
                    prevLines = concatNewLinesToOld(prevLines, valueLines);
                }
                e.setProperty(AGGREGATED_PROPERTY, prevLines);
                e.getMessage().setBody(prevLines); // needed?
            }).id("ProcessOrd");

        from("direct:ord-csv-out").routeId("ordCsvOut")
                .marshal(ordCsvDataFormat)
                .to("direct:ord-file-out");

        from("direct:ord-file-out").id("ORDFileOut")
            .log("Trying to write the file ${headers.CamelFileName}")
                .onException(Exception.class)
                    .maximumRedeliveries(10).redeliveryDelay(1000)
                    .log("Failed to write the file to Azure: ${exchangeProperty.CamelExceptionCaught}")
                .end()
            .to("file:ordOut?fileExist=Override")
            .log("File ${headers.CamelFileName} written");
                //fileExist=Fail throws GenericFileOperationException
                // could catch that and then append without header

    }

}

