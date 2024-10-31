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
    void fpmShouldReadAndParse_ID022_XMLFiles() throws InterruptedException, Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        ex.getIn().setHeader("CamelFileName", "ID022_FI_TOSITE_20240829-113059-336.xml");
        ex.getIn().setBody("<FIDCCP02 xmlns:prx=\"urn:sap.com:proxy:P10:/1SAI/TAS1453E8F459338B01509F:740\">\n" +
            "<IDOC>\n" +
                "<E1FIKPF>\n" +
                    "<BUKRS>3900</BUKRS>\n" +
                    "<BELNR>0000000001</BELNR>\n" +
                    "<GJAHR>2023</GJAHR>\n" +
                    "<BLART>6S</BLART>\n" +
                    "<BLDAT>20230227</BLDAT>\n" +
                    "<BUDAT>20230227</BUDAT>\n" +
                    "<MONAT>02</MONAT>\n" +
                "</E1FIKPF>\n" +
                "<E1FIKPF>\n" +
                    "<BUKRS>3900</BUKRS>\n" +
                    "<BELNR>0000000002</BELNR>\n" +
                    "<GJAHR>2023</GJAHR>\n" +
                    "<BLART>6S</BLART>\n" +
                    "<BLDAT>20230227</BLDAT>\n" +
                    "<BUDAT>20230227</BUDAT>\n" +
                    "<MONAT>02</MONAT>\n" +
                "</E1FIKPF>\n" +
            "</IDOC>\n" +
        "</FIDCCP02>");



        AdviceWith.adviceWith(ctx, "ProcessFPM", a -> {
            // a.mockEndpointsAndSkip("direct:azure-out");
        });

        Exchange res = producerTemplate.send("direct:fpm-files-in", ex);

        assertEquals("ok", res.getMessage().getHeader("ok"));
    }

}
