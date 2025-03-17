package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.RouteDefinition;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

// WBS:n käsittelee Projektit ja projektin rakenneosat  (SAPPROJEKTI_PRPS, WBS_OUT.xml)
// ORD_OUT_ _> SAPSISTILAUS
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class WBSInRouteBuilder extends RouteBuilder {
    @Inject
    Logger log;

    @Inject
    ProducerTemplate producerTemplate;

    CsvDataFormat ordCsvDataFormat = new CsvDataFormat().setDelimiter(';').setHeader(new String[] {
        "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
    });

    CsvDataFormat wbsCsvDataFormat = new CsvDataFormat().setDelimiter(';').setHeader(new String[] {
        "MANDT", "PSPNR", "POSID", "POST1", "USR03"
    });

    RouteDefinition byYearAndMonthRoute(String fromUri, String commonValueKey, String valuesKey,
                                        BiFunction<Map<String, Object>, Map<String, Object>, LinkedHashMap<String, Object>> valuesHashMap) {
        return from(fromUri)
            .process(e -> {
                Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
                Map<String, Object> IDOC = xmlRoot.get("IDOC");
                Map<String, Object> commonValues = (Map)IDOC.get(commonValueKey);
                Object valuesObj = IDOC.get(valuesKey);
                List<LinkedHashMap<String, Object>> valueLines;

                if (valuesObj instanceof List valList) {
                    List<Map<String, Object>> vals = valList;
                    valueLines = vals.stream().map(v -> valuesHashMap.apply(commonValues, v)).toList();
                } else {
                    Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
                    valueLines = List.of(valuesHashMap.apply(commonValues, v));
                }

                Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth = e.getProperty("byYearAndMonth", Map.class);
                if (byYearAndMonth == null) byYearAndMonth = new HashMap<>(1);

                String CREDAT = (String) commonValues.get("CREDAT"); //20240820
                String yearAndMonth = CREDAT.substring(0, 6);

                List<LinkedHashMap<String, Object>> prevLines = byYearAndMonth.get(yearAndMonth);
                if (prevLines == null) {
                    byYearAndMonth.put(yearAndMonth, valueLines);
                } else {
                    byYearAndMonth.put(yearAndMonth, concatNewLinesToOld(prevLines, valueLines));
                }
                e.setProperty("byYearAndMonth", byYearAndMonth);

                e.getMessage().setBody(byYearAndMonth);
            });
    }

    @Override
    public void configure() throws Exception {

        AggregationStrategy fileAggregationStrategy = new FileAggregationStrategy();

        // TODO: own for each file type
        from("timer:start?period=30000").routeId("WBS_IN")
                .log("start")
                .setProperty("keepReading", simple("true", Boolean.class))
            .loopDoWhile(simple("${exchangeProperty.keepReading}"))
                .log("looping")
                // TODO: read all X files, then Y files, etc?
                .pollEnrich("file:in", 500, fileAggregationStrategy)
                .log("polled file ${headers.CamelFileName}")
                .choice()
                    .when().simple("${exchangeProperty.noMoreFiles}")
                        .setProperty("keepReading", simple("false", Boolean.class))
                        .log("done, writing out")
                    .otherwise()
                        .log("enriched ${headers.CamelFileName}")
                        .to("direct:any-sap-file-in")
                    .end()
                .end()
            .end()
            .end()
            .split(body()).process(e -> {
                Map.Entry<String, List<Map<String, Object>>> yearAndMonthAndLines = e.getMessage().getBody(Map.Entry.class);
                String yearAndMonth = yearAndMonthAndLines.getKey();
                String year = yearAndMonth.substring(0, 4);
                String month = yearAndMonth.substring(4, 6);
                if (month.startsWith("0")) month = month.substring(1);
                // SAPACTUAL_PRPS_YYYY_M.csv
                String prefix = getOutFileNamePrefix(e.getMessage().getHeader("CamelFileName", String.class));
                e.getMessage().setHeader("OutFileName", prefix + "_" + year + "_" + month + ".csv");
                e.getMessage().setBody(yearAndMonthAndLines.getValue());
            })
            .log("processed ${headers.CamelFileName}, writing to Azure ${headers.OutFileName}")
            .setHeader("CamelFileName", simple("${headers.OutFileName}"))
            .to("direct:ord-azure-out");

        from("direct:any-sap-file-in")
            .log("WBS IN :: ${headers.CamelFileName}")
            .unmarshal().jacksonXml()
                .choice()
                    .when(simple("${headers.CamelFileName} startsWith 'WBS_OUT_'"))
                        .to("direct:process-wbs-sap-without-pojo")
                    .when(simple("${headers.CamelFileName} startsWith 'ORD_OUT_'"))
                        .to("direct:process-ord-out")
                    .otherwise()
                        .log("unknown file ${headers.CamelFileName}")
                .end();

        byYearAndMonthRoute("direct:process-wbs-sap-without-pojo",
"EDI_DC40", "ZHKI_PROJEKTIRAKENTEENOSA",
                (commonValues, valuesLine) -> {
                    LinkedHashMap<String, Object> project = new LinkedHashMap<>();
                    project.put("MANDT", commonValues.get("MANDT"));
                    project.put("PSPNR", valuesLine.get("PSPNR"));
                    project.put("POSID", valuesLine.get("POSID"));
                    project.put("POST1", valuesLine.get("POST1"));
                    project.put("USR03", valuesLine.get("USR03"));
                    return project;
                }).id("ProcessPojolessWBSSAP");

        // ORD_OUT_167_SOTE*.xml
        // SAPSISTILAUS
        byYearAndMonthRoute("direct:process-ord-out",
                "EDI_DC40", "ZHKI_TARSISTILAUKSET",
                (commonValues, valuesLine) -> {
                    LinkedHashMap<String, Object> project = new LinkedHashMap<>(); // order matters
                    project.put("BUKRS", valuesLine.get("BUKRS")); // maksupiste
                    project.put("AUART", valuesLine.get("AUART")); // tilauslaji
                    project.put("AUFNR", valuesLine.get("AUFNR")); // tilausnumero
                    project.put("KTEXT", valuesLine.get("KTEXT")); // lyhytteksti
                    project.put("STTXT", valuesLine.get("STTXT")); // tilakoodit
                    return project;
                }).id("ProcessOrdOut");

        from("direct:ord-azure-out").id("ordAzureOut")
            .marshal(ordCsvDataFormat)
            .to("direct:file-out");

        from("direct:file-out").id("GenericFileOut")
            .to("file:out").log("File ${headers.CamelFileName} written");
    }

    <T> List<T> concatNewLinesToOld(List<T> prevLines, List<T> workBreakDownLines) {
        return Stream.concat(prevLines.stream(), workBreakDownLines.stream()).toList();
    }

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

        // SAPPROJEKTI ei näytä tarvitsevan vuoden / kuukauden mukaan kokoamista
        // tarviiko kuitenkin tarkistaa vuosi / kuukausi jos esim. vanhoja filuja ensimmäisessä latauksessa
        // Projektit ja projektin rakenneosat  (SAPPROJEKTI_PRPS, WBS_OUT.xml)
        if (fileInName.startsWith("WBS_OUT_")) {
            return "SAPPROJEKTI_PRPS"; // vaiko SAPPROJEKTI.csv ???
        } else if (fileInName.startsWith("ORD_OUT_")) {
            // actually just "SAPSISTILAUS.csv"
            return "SAPSISTILAUS";//"SAPSISTILAUS_TO_FPM_";
        }
        return "unknown_file";
    }
}

