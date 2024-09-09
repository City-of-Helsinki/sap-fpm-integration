package fi.hel.integration.sapfpm;

import fi.hel.integration.sapfpm.routes.InRouteBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@ApplicationScoped
class InitialTest {
    @Inject
    InRouteBuilder in;

    @Inject
    ProducerTemplate producerTemplate;


    @Test
    void test() throws InterruptedException, Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        ex.getIn().setHeader("CamelFileName", "test.xml");
        ex.getIn().setBody("");

        AdviceWith.adviceWith(ctx, "ProcessFPM", a -> {
           // a.mockEndpointsAndSkip("direct:azure-out");
        });

        Exchange res = producerTemplate.send("direct:fpm-files-in", ex);

        assertEquals(res.getMessage().getHeader("ok"), "ok");
    }

}
