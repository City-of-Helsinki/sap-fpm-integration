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
    public Exchange aggregate(Exchange original, Exchange polled) {
        if (polled == null) {
            original.setProperty("pollWasEmpty", Boolean.TRUE);
            original.getMessage().setBody(original.getProperty(aggregatedPropertyName));
            return original;
        } else {
            polled.setProperty(aggregatedPropertyName, original.getProperty(aggregatedPropertyName));
            polled.setProperty("pollWasEmpty", Boolean.FALSE);
        }

        return polled; // so the polled body can be processed and aggregated into
    }
}
