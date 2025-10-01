package fi.hel.integration.sapfpm;

import io.quarkus.runtime.Startup;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
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
        });
    }

    public void send(String sentryMsg, Exchange exchange) {
        String toimiala = exchange.getMessage().getHeader("toimiala", String.class);
        Sentry.captureMessage(sentryMsg, scope -> {
            if (toimiala != null) scope.setContexts("toimiala", toimiala);
            scope.setLevel(SentryLevel.INFO);
        });
    }

    public void sendException(Exchange exchange) {
        String toimiala = exchange.getMessage().getHeader("toimiala", String.class);
        Sentry.captureException(exchange.getException(), scope -> {
            if (toimiala != null) scope.setContexts("toimiala", toimiala);
            scope.setLevel(SentryLevel.ERROR);
        });
    }
}
