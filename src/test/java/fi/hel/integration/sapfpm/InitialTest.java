package fi.hel.integration.sapfpm;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.EndpointInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@QuarkusTest
@ApplicationScoped
class InitialTest {
    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:out")
    private MockEndpoint mockFileOut;

    @Test
    void testFtp() throws InterruptedException, Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
    }
}
