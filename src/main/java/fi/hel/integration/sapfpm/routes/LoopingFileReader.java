package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;

// reads files and aggregates
public abstract class LoopingFileReader extends RouteBuilder {

    public ProcessorDefinition<?> createLoopingFileReaderRoute(String routeId, String pollEnrichIn, String fileFilter, String processingRouteTo, String aggregatedPropertyName) {
        // TODO:
        return from("timer:start " + routeId + " ?period=30000").routeId(routeId)
            .log("start, keepReading: ${exchangeProperty.keepReading}")
            .setProperty("keepReading", simple("true", Boolean.class))
            .loopDoWhile(simple("${exchangeProperty.keepReading}"))
                .log("looping")
                .pollEnrich(pollEnrichIn + "?include=RAW(" + fileFilter + "*.xml)", 500, new FileAggregationStrategy(aggregatedPropertyName))
                .log("polled file ${headers.CamelFileName}")
                // TODO: save into a list of processed files and move to arch after all processed
                .choice()
                    .when().simple("${exchangeProperty.noMoreFiles}")
                        .setProperty("keepReading", simple("false", Boolean.class))
                        .log("no more files, writing out")
                    .otherwise()
                        .log("enriched ${headers.CamelFileName}")
                        .to(processingRouteTo)
                    .end()
                .end()
            .end()
            .choice()
                .when(simple("${body} != null"));
    }

}
