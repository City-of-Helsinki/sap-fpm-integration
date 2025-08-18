package fi.hel.integration.sapfpm.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
import org.slf4j.Logger;

public class DefaultErrorHandlerBuilder {
    public static DefaultErrorHandlerDefinition buildDefaultErrorHandler(RouteBuilder routeBuilder, Logger log) {
        return routeBuilder.defaultErrorHandler()
            .maximumRedeliveries(5)
            .redeliveryDelay(5000)
            .onExceptionOccurred(e -> {
                Integer redeliveries = e.getMessage().getHeader("CamelRedeliveryCounter", Integer.class),
                        maxRedeliveries = e.getMessage().getHeader("CamelRedeliveryMaxCounter", Integer.class);
                if (redeliveries != null && maxRedeliveries != null && redeliveries > maxRedeliveries) {
                    log.error("Exception occurred after retries (%s/%s): ".formatted(redeliveries, maxRedeliveries));
                    Exception exc = e.getException(Exception.class);
                    if (exc != null) {
                        log.error(exc.getMessage());
                    }
                }
            });
    }
}
