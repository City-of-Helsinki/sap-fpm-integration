package fi.hel.integration.sapfpm.aggregationstrategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import java.io.File;
import java.util.Map;

public class FileAggregationStrategy implements AggregationStrategy {

    public final String aggregatedPropertyName;
    public FileAggregationStrategy(String aggregatedPropertyName) {
        this.aggregatedPropertyName = aggregatedPropertyName;
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            System.out.println("new is null");
            oldExchange.setProperty("noMoreFiles", Boolean.TRUE);
            oldExchange.setProperty("keepReading", Boolean.FALSE);

            return oldExchange;
        } else {
            System.out.println("oldexchange: " + oldExchange.getProperty(aggregatedPropertyName));
            newExchange.setProperty(aggregatedPropertyName, oldExchange.getProperty(aggregatedPropertyName));
        }
        newExchange.setProperty("keepReading", oldExchange.getProperty("keepReading"));

        System.out.println(oldExchange.getMessage().getHeader("CamelFileName"));
        System.out.println("new: " + newExchange.getMessage().getHeader("CamelFileName"));

        return newExchange;
    }
}
