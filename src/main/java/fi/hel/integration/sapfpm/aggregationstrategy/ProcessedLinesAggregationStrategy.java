package fi.hel.integration.sapfpm.aggregationstrategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static fi.hel.integration.sapfpm.IDOCParser.concatNewLinesToOld;

public class ProcessedLinesAggregationStrategy implements AggregationStrategy {

    public static String PROCESSED_FILES_PROPERTY = "processedFiles";

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String processedFileName =  newExchange.getMessage().getHeader("CamelFileName", String.class);
        if (oldExchange == null) {
            List<String> processedFiles = new ArrayList<>();
            processedFiles.add(processedFileName);
            newExchange.setProperty(PROCESSED_FILES_PROPERTY, processedFiles);
            return newExchange;
        } else {
            List<LinkedHashMap<String, Object>> newValues = newExchange.getMessage().getBody(List.class);
            List<LinkedHashMap<String, Object>> oldValues = oldExchange.getMessage().getBody(List.class);
            List<LinkedHashMap<String, Object>> all = concatNewLinesToOld(oldValues, newValues);
            oldExchange.getMessage().setBody(all);
            List<String> processedFiles = oldExchange.getProperty(PROCESSED_FILES_PROPERTY, List.class);
            processedFiles.add(processedFileName);
            return oldExchange;
        }
    }
}
