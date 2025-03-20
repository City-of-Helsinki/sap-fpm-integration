package fi.hel.integration.sapfpm.routes.sapprojekti;

import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
// TODO: saattaa olla PROJECT nimellä testissä!!!

// WBS_OUT:n käsittelee Projektit ja projektin rakenneosat  (SAPPROJEKTI_PRPS, WBS_OUT.xml)
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
// PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin
@ApplicationScoped
public class WBSInRouteBuilder extends LoopingFileReader {
    CsvDataFormat wbsCsvDataFormat = new CsvDataFormat().setDelimiter(';').setQuoteDisabled(true).setHeader(new String[] {
        "PBUKR", "POSID", "POST1", "STUFE", "ERDAT", "AEDAT", "TXT40"
    });

    final static String IN_FILE_PREFIX = "WBS_OUT_";
    final static String POLL_ENRICH_IN = "file:in";

    final static String AGGREGATED_PROPERTY = "wbsBody";

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> commonValues, Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> project = new LinkedHashMap<>();
        project.put("PBUKR", valuesLine.get("PBUKR"));
        project.put("POSID", valuesLine.get("POSID"));
        project.put("POST1", valuesLine.get("POST1"));
        project.put("STUFE", valuesLine.get("STUFE"));
        project.put("ERDAT", valuesLine.get("ERDAT"));
        project.put("AEDAT", valuesLine.get("AEDAT"));
        project.put("TXT40", valuesLine.get("TXT40"));
        return project;
    }

    @Override
    public void configure() throws Exception {

        createLoopingFileReaderRoute("WBS_IN", POLL_ENRICH_IN, IN_FILE_PREFIX, "direct:unmarshal-and-process-wbs", AGGREGATED_PROPERTY)
            .to("direct:unmarshal-and-process-wbs")
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
            .to("direct:wbs-csv-out");

        from("direct:unmarshal-and-process-wbs")
            .log("WBS IN :: ${headers.CamelFileName}")
            .unmarshal().jacksonXml()
            .to("direct:process-wbs");

        from("direct:process-wbs")
            .process(e -> {
                Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> commonValuesAndValues = extractValuesFromIDOC(e, "EDI_DC40", "ZHKI_PROJEKTIRAKENTEENOSA", this::extractValues);
                Map<String, Object> commonValues = commonValuesAndValues.getItem1();
                List<LinkedHashMap<String, Object>> valueLines = commonValuesAndValues.getItem2();
                // TODO: check for uniqueness
                String CREDAT = (String) commonValues.get("CREDAT"); //20240820
                String yearAndMonth = CREDAT.substring(0, 6);

                Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth = addToByYearAndMonthIfExistsOrCreate(e.getProperty(AGGREGATED_PROPERTY, Map.class), yearAndMonth, valueLines);
                e.setProperty(AGGREGATED_PROPERTY, byYearAndMonth);

                e.getMessage().setBody(byYearAndMonth);
            }).id("ProcessWBS");

        from("direct:wbs-csv-out").routeId("wbsCsvOut")
            .marshal(wbsCsvDataFormat)
            .to("direct:wbs-file-out");

        from("direct:wbs-file-out").id("WBSFileOut")
            .log("File ${headers.CamelFileName} written");
    }

    public String getOutFileNamePrefix(String fileInName) {
        // SAPPROJEKTI ei tarvitse vuoden / kuukauden mukaan kokoamista
        return "SAPPROJEKTI_PRPS"; // vaiko SAPPROJEKTI.csv ???
    }
}

