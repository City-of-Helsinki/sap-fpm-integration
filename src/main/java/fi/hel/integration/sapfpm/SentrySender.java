package fi.hel.integration.sapfpm;

import io.quarkus.runtime.Startup;
import io.sentry.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@Startup
@ApplicationScoped
public class SentrySender {

    SentrySender(@ConfigProperty(name = "sentry-url") Optional<String> sentryUrlOpt, @ConfigProperty(name = "app-env") String appEnv) {
        Sentry.init(options -> {
            options.setDsn(sentryUrlOpt.orElse(""));
            options.setEnvironment(appEnv);
            options.setAttachStacktrace(false);
            options.setDebug(false);
            options.setTracesSampleRate(1.0);
        });
    }

    public void send(String sentryMsg, Exchange exchange) {
        String toimiala = exchange.getMessage().getHeader("toimiala", String.class);
        ITransaction transaction = Sentry.startTransaction(sentryMsg, "sendfile");
        transaction.setContext("toimiala", toimiala);
        transaction.finish(SpanStatus.OK);
    }

    public void sendException(Exchange exchange) {
        String toimiala = exchange.getMessage().getHeader("toimiala", String.class);
        Sentry.captureException(exchange.getException(), scope -> {
            if (toimiala != null) scope.setContexts("toimiala", toimiala);
            scope.setLevel(SentryLevel.ERROR);
        });
    }
}
