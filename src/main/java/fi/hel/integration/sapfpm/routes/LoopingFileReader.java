package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;

// reads files and aggregates
public abstract class LoopingFileReader extends RouteBuilder {

    public ProcessorDefinition<?> createLoopingFileReaderRoute(String routeId, String fileFilter, String processingRouteTo, String aggregatedPropertyName) {
        return from("timer:start?period=30000").routeId(routeId)
            .log("start")
            .setProperty("keepReading", simple("true", Boolean.class))
            .loopDoWhile(simple("${exchangeProperty.keepReading}"))
                .log("looping")
                .pollEnrich("file:in?include=RAW(" + fileFilter + "*.xml)", 500, new FileAggregationStrategy(aggregatedPropertyName))
                .log("polled file ${headers.CamelFileName}")
                .choice()
                    .when().simple("${exchangeProperty.noMoreFiles}")
                        .setProperty("keepReading", simple("false", Boolean.class))
                        .log("done, writing out")
                    .otherwise()
                        .log("enriched ${headers.CamelFileName}")
                        .to(processingRouteTo)
                    .end()
                .end()
            .end();
    }

}
