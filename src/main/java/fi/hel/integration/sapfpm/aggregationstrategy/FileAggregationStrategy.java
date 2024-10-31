package fi.hel.integration.sapfpm.aggregationstrategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import java.util.Map;

public class FileAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            System.out.println("new is null");
            oldExchange.setProperty("noMoreFiles", Boolean.TRUE);
            oldExchange.setProperty("keepReading", Boolean.FALSE);

            return oldExchange;
        } else {
            System.out.println("byYearAndMonth oldexchange: " + oldExchange.getProperty("byYearAndMonth"));
            newExchange.setProperty("byYearAndMonth", oldExchange.getProperty("byYearAndMonth", Map.class));
            if (newExchange.getProperty("byYearAndMonth") != null) {
                System.out.println("SIZE: " + newExchange.getProperty("byYearAndMonth", Map.class).size());
            }
        }
        newExchange.setProperty("keepReading", newExchange.getProperty("keepReading"));

        System.out.println(oldExchange.getMessage().getHeader("CamelFileName"));
        System.out.println("new: " + newExchange.getMessage().getHeader("CamelFileName"));

        return newExchange;
    }
}
