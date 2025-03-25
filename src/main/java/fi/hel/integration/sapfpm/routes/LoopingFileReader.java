package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;

import java.util.ArrayList;
import java.util.List;

// reads files and aggregates
public abstract class LoopingFileReader extends RouteBuilder {

    long pollingPeriod = 600000;
    long enrichTimeout = 1000;

    public ProcessorDefinition<?> createLoopingFileReaderRoute(String routeId, String pollEnrichIn, String fileFilter, String processingRouteTo, String aggregatedPropertyName) {
        // TODO: make sure only 1 is started at a time
        return from("timer:start-" + routeId + " ?period=" + pollingPeriod + "&delay=0").routeId(routeId)

            .setProperty("keepReading", simple("true", Boolean.class))
            .setProperty("pollEmptyCount", simple("0"))
            .process(e -> e.setProperty("processedFiles", new ArrayList<String>()))
            .loopDoWhile(simple("${exchangeProperty.pollEmptyCount} < 2"))
                .pollEnrich(pollEnrichIn + "?charset=ISO-8859-1&include=RAW(" + fileFilter + ".*.xml)",
                        enrichTimeout, new FileAggregationStrategy(aggregatedPropertyName))
                // TODO: save into a list of processed files and move to arch after all processed
                .choice()
                    .when().simple("${exchangeProperty.pollWasEmpty}")
                        .setProperty("pollEmptyCount", simple("${exchangeProperty.pollEmptyCount}++"))
                    .otherwise()
                        .log("enriched ${headers.CamelFileName}")
                        .setProperty("processedFileName", header("CamelFileName"))
                        .to(processingRouteTo)
                        .process(e -> e.getProperty("processedFiles", List.class).add(e.getProperty("processedFileName")))
                    .end()
                .end()
            .end()
            .process(e -> e.setProperty("processedFilesNotEmpty", !e.getProperty("processedFiles", List.class).isEmpty()))
            .choice()
                .when(simple("${exchangeProperty.processedFilesNotEmpty}"))
                .process(e -> {
                    log.info("Processed files: ");
                    log.info(String.join(", ", e.getProperty("processedFiles", List.class)));
                });
    }

}
