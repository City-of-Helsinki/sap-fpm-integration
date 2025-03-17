package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.RouteDefinition;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static fi.hel.integration.sapfpm.IDOCParser.*;
// KNA1 = asiakkaat, tälle ei perustietoliittymää eikä tule sotepelle käyttöön   (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
// LFA1 = toimittajat, tälle on perustietoliittymä mutta ei tule sotepe käyttöön (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
// BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.
// ID022_FI_TOSITE_OUT_ >
// SAPACTUAL_BKPF_YYYY_M.csv
// SAPACTUAL_BSEG_YYYY_M.csv
// SAPACTUAL_FMGLFLEXA_YYYY_M.csv
// SAPACTUAL_KNA1_YYYY_M.csv
// SAPACTUAL_LFA1_YYYY_M.csv
// SAPACTUAL_PRPS_YYYY_M.csv
// SAPACTUAL_VIBDBE_YYYY_M.csv
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class ID022FITositeInRouteBuilder extends LoopingFileReader {
    CsvDataFormat tositeCsvDataFormat = new CsvDataFormat().setDelimiter(';').setHeader(new String[] {
            // TODO: CHECK
            //"BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
    });

    final static String IN_FILE_PREFIX = "ID022_FI_TOSITE_";


    public LinkedHashMap<String, Object> extractValues(Map<String, Object> commonValues, Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> project = new LinkedHashMap<>(); // order matters
        project.put("BUKRS", valuesLine.get("BUKRS")); // maksupiste
        project.put("AUART", valuesLine.get("AUART")); // tilauslaji
        project.put("AUFNR", valuesLine.get("AUFNR")); // tilausnumero
        project.put("KTEXT", valuesLine.get("KTEXT")); // lyhytteksti
        project.put("STTXT", valuesLine.get("STTXT")); // tilakoodit
        return project;
    }

    @Override
    public void configure() throws Exception {
        createLoopingFileReaderRoute("TOSITE_IN", IN_FILE_PREFIX, "direct:unmarshal-xml-and-process-tosite",
                "byYearAndMonth")
                .split(body()).process(e -> {
                    Map.Entry<String, List<Map<String, Object>>> yearAndMonthAndLines = e.getMessage().getBody(Map.Entry.class);
                    String yearAndMonth = yearAndMonthAndLines.getKey();
                    String year = yearAndMonth.substring(0, 4);
                    String month = yearAndMonth.substring(4, 6);
                    if (month.startsWith("0")) month = month.substring(1);
                    // TODO: split to several files
                    String prefix = getOutFileNamePrefix(e.getMessage().getHeader("CamelFileName", String.class));
                    e.getMessage().setHeader("OutFileName", prefix + "_" + year + "_" + month + ".csv");
                    e.getMessage().setBody(yearAndMonthAndLines.getValue());
                })
                .log("processed ${headers.CamelFileName}, writing to Azure ${headers.OutFileName}")
                .setHeader("CamelFileName", simple("${headers.OutFileName}"))
                .to("direct:tosite-csv-out");

        from("direct:unmarshal-xml-and-process-tosite")
            .log("TOSITE IN :: ${headers.CamelFileName}")
            .unmarshal().jacksonXml();

        from("direct:process-ord-out")
                .process(e -> {
                    Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> commonValuesAndValues =
                            extractValuesFromIDOC(e, "EDI_DC40", "ZHKI_TARSISTILAUKSET", this::extractValues);
                    Map<String, Object> commonValues = commonValuesAndValues.getItem1();
                    List<LinkedHashMap<String, Object>> valueLines = commonValuesAndValues.getItem2();

                    // TODO: GJAHR + MONAT
                    String CREDAT = (String) commonValues.get("CREDAT"); //20240820
                    String yearAndMonth = CREDAT.substring(0, 6);

                    Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth = addToByYearAndMonthIfExistsOrCreate(e.getProperty("byYearAndMonth", Map.class), yearAndMonth, valueLines);
                    e.setProperty("byYearAndMonth", byYearAndMonth);

                    e.getMessage().setBody(byYearAndMonth);
                }).id("ProcessOrdOut");

        from("direct:tosite-csv-out").id("tositeAzureOut")
                .marshal(tositeCsvDataFormat)
                .to("direct:tosite-file-out");

        //.setProperty(Exchange.CHARSET_NAME, constant("ISO-8859-1"))
        from("direct:tosite-file-out").id("tositeFileOut")
                .log("File ${headers.CamelFileName} written");
    }

    //
    // TODO: split to several files
    public String getOutFileNamePrefix(String fileInName) {
        // FI_TOSITE ->
        // SAPACTUAL_BKPF_YYYY_M.csv
        // SAPACTUAL_BSEG_YYYY_M.csv
        // SAPACTUAL_FMGLFLEXA_YYYY_M.csv
        // SAPACTUAL_KNA1_YYYY_M.csv
        // SAPACTUAL_LFA1_YYYY_M.csv
        // SAPACTUAL_PRPS_YYYY_M.csv
        // SAPACTUAL_VIBDBE_YYYY_M.csv
        //

        return null;
    }
}

