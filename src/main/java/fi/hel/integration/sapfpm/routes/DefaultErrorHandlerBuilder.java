package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.SentrySender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
import org.slf4j.Logger;

@ApplicationScoped
public class DefaultErrorHandlerBuilder {
    @Inject
    SentrySender sentrySender;



    public DefaultErrorHandlerDefinition buildDefaultErrorHandler(RouteBuilder routeBuilder, Logger log) {
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
                        sentrySender.sendException(e);
                    }
                }
            });
    }
}
