package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// ORD_OUT_ _> SAPSISTILAUS
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class OrdInRouteBuilder extends LoopingFileReader {
    @Inject
    Logger log;

    CsvDataFormat ordCsvDataFormat = new CsvDataFormat().setDelimiter(';').setHeader(new String[] {
            "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
    });

    final static String IN_FILE_PREFIX = "ORD_OUT_";

    // TODO: check values against spec
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

        createLoopingFileReaderRoute("ORD_IN", IN_FILE_PREFIX, "direct:unmarshal-xml-and-process-ord",
        "byYearAndMonth")
            .split(body()).process(e -> {
                Map.Entry<String, List<Map<String, Object>>> yearAndMonthAndLines = e.getMessage().getBody(Map.Entry.class);
                String yearAndMonth = yearAndMonthAndLines.getKey();
                String year = yearAndMonth.substring(0, 4);
                String month = yearAndMonth.substring(4, 6);
                if (month.startsWith("0")) month = month.substring(1);
                String prefix = getOutFileNamePrefix(e.getMessage().getHeader("CamelFileName", String.class));
                e.getMessage().setHeader("OutFileName", prefix + "_" + year + "_" + month + ".csv");
                e.getMessage().setBody(yearAndMonthAndLines.getValue());
            })
            .log("processed ${headers.CamelFileName}, writing to Azure ${headers.OutFileName}")
            .setHeader("CamelFileName", simple("${headers.OutFileName}"))
            .to("direct:ord-csv-out");

        from("direct:unmarshal-xml-and-process-ord")
                .log("ORD IN :: ${headers.CamelFileName}")
                .unmarshal().jacksonXml();

        // ORD_OUT_167_SOTE*.xml
        // SAPSISTILAUS
        from("direct:process-ord-out")
            .process(e -> {
                Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> commonValuesAndValues = extractValuesFromIDOC(e,  "EDI_DC40", "ZHKI_TARSISTILAUKSET", this::extractValues);
                Map<String, Object> commonValues = commonValuesAndValues.getItem1();
                List<LinkedHashMap<String, Object>> valueLines = commonValuesAndValues.getItem2();

                // TODO: GJAHR + MONAT
                String CREDAT = (String) commonValues.get("CREDAT"); //20240820
                String yearAndMonth = CREDAT.substring(0, 6);

                Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth = addToByYearAndMonthIfExistsOrCreate(e.getProperty("byYearAndMonth", Map.class), yearAndMonth, valueLines);
                e.setProperty("byYearAndMonth", byYearAndMonth);

                e.getMessage().setBody(byYearAndMonth);
            }).id("ProcessOrdOut");

        from("direct:ord-csv-out").id("ordAzureOut")
                .marshal(ordCsvDataFormat)
                .to("direct:ord-file-out");

        from("direct:ord-file-out").id("ORDFileOut")
            .log("File ${headers.CamelFileName} written");
    }

    public String getOutFileNamePrefix(String fileInName) {
        return "SAPSISTILAUS";//"SAPSISTILAUS_TO_FPM_"
    }
}

