package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// PART_OUT_ _> SAPKUMPPANI
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class PartInRouteBuilder extends LoopingFileReader {
    CsvDataFormat partCsvDataFormat = new CsvDataFormat().setDelimiter(';').setHeader(new String[] {
        "RCOMP", "NAME1"
    });

    // TODO: PART_OUT_167_SOTE
    // PALKE: PART_OUT_138_9500
    final static String IN_FILE_PREFIX = "PART_OUT_167_SOTE_";

    final static String AGGREGATED_PROPERTY = "partBody";

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> commonValues, Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    @Override
    public void configure() throws Exception {
        createLoopingFileReaderRoute("PART_IN", IN_FILE_PREFIX, "direct:unmarshal-xml-and-process-part",
                AGGREGATED_PROPERTY)
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
                .to("direct:part-csv-out");

        from("direct:unmarshal-xml-and-process-part")
                .log("PART IN :: ${headers.CamelFileName}")
                .unmarshal().jacksonXml();

        from("direct:process-part-out")
            .process(e -> {
                Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> commonValuesAndValues =
                        // TODO: check against example file
                        extractValuesFromIDOC(e,  null, "Kumppaniyhtiö", this::extractValues);
                List<LinkedHashMap<String, Object>> valueLines = commonValuesAndValues.getItem2();
                List<LinkedHashMap<String, Object>> prevLines = e.getProperty(AGGREGATED_PROPERTY, List.class);
                if (prevLines == null) {
                    prevLines = valueLines;
                } else {
                    prevLines = concatNewLinesToOld(prevLines, valueLines);
                }

                e.setProperty(AGGREGATED_PROPERTY, prevLines);
                e.getMessage().setBody(prevLines);
            }).id("ProcessOrdOut");

        from("direct:part-csv-out").id("partAzureOut")
                .marshal(partCsvDataFormat)
                .to("direct:part-file-out");

        from("direct:part-file-out").id("PARTFileOut")
                .log("File ${headers.CamelFileName} written");
    }

    public String getOutFileNamePrefix(String fileInName) {
        return "SAPKUMPPANI";
    }
}