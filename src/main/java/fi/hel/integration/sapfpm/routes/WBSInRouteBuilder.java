package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import fi.hel.integration.sapfpm.config.AppConfig;
import fi.hel.integration.sapfpm.model.output.SAPACTUAL.SAPACTUAL_PRPS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.DefaultExchange;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class WBSInRouteBuilder extends RouteBuilder {

    @Inject
    AppConfig appConfig;

    @Inject
    Logger log;

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public void configure() throws Exception {

        AggregationStrategy fileAggregationStrategy = new FileAggregationStrategy();

        from("timer:start?period=30000").routeId("WBS_IN")
                .log("start")
                .setProperty("keepReading", simple("true", Boolean.class))
            .loopDoWhile(simple("${headers.keepReading}"))
                .log("looping")
                .pollEnrich("file:in", 500, fileAggregationStrategy)
                .log("polled file ${headers.CamelFileName}")
                .choice()
                    .when().simple("${exchangeProperty[noMoreFiles]}")
                        .setProperty("keepReading", simple("false", Boolean.class))
                        .log("done, writing out")
                    .otherwise()
                        .log("enriched ${headers.CamelFileName}")
                        .to("direct:wbs-sap-files-in")
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
                e.getMessage().setHeader("OutFileName", "SAPACTUAL_PRPS_" + year + "_" + month + ".csv");
                e.getMessage().setBody(yearAndMonthAndLines.getValue());
            })
            .log("processed ${headers.CamelFileName}, writing to Azure ${headers.OutFileName}")
            .setHeader("CamelFileName", simple("${headers.OutFileName}"))
            .to("direct:azure-out");

        from("direct:wbs-sap-files-in")
            .log("WBS IN :: ${headers.CamelFileName}")
            .unmarshal().jacksonXml().to("direct:process-wbs-sap-without-pojo");

        from("direct:process-wbs-sap-without-pojo").id("ProcessPojolessWBSSAP")
            .process(e -> {
                Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
                Map<String, Object> IDOC = xmlRoot.get("IDOC");
                Map<String, Object> commonValues = (Map)IDOC.get("EDI_DC40");
                List<Map<String, Object>> values = (List)IDOC.get("ZHKI_PROJEKTIRAKENTEENOSA");
                List<Map<String, Object>> workBreakDownLines = values.subList(1, values.size()).stream().map(v -> {
                    Map<String, Object> project = new LinkedHashMap<>();
                    project.put("MANDT", commonValues.get("MANDT"));
                    project.put("POSID", v.get("POSID"));
                    project.put("POST1", v.get("POST1"));
                    project.put("PSPNR", v.get("PSPNR"));
                    project.put("USR03", v.get("USR03"));
                    return project;
                }).toList();

                Map<String, List<Map<String, Object>>> byYearAndMonth = e.getProperty("byYearAndMonth", Map.class);
                if (byYearAndMonth == null) byYearAndMonth = new HashMap<>(1);

                String CREDAT = (String) commonValues.get("CREDAT"); //20240820
                String yearAndMonth = CREDAT.substring(0, 6);

                List<Map<String, Object>> prevLines = byYearAndMonth.get(yearAndMonth);
                if (prevLines == null) {
                    byYearAndMonth.put(yearAndMonth, workBreakDownLines);
                } else {
                    byYearAndMonth.put(yearAndMonth, concatNewLinesToOld(prevLines, workBreakDownLines));
                }
                e.setProperty("byYearAndMonth", byYearAndMonth);

                e.getMessage().setBody(byYearAndMonth.entrySet());
            });
    }

    <T> List<T> concatNewLinesToOld(List<T> prevLines, List<T> workBreakDownLines) {
        return Stream.concat(prevLines.stream(), workBreakDownLines.stream()).toList();
    }
}

