package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.StreamListIterator;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.builder.Builder.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CoTositeDBTest extends CamelQuarkusTestSupport {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jdbc:sapactualco")
    private MockEndpoint mockJdbcSapActual;

    @EndpointInject("mock:cotositerivit-fetch-streamed")
    private MockEndpoint mockCoTositeRivitFetchStreamed;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:fetch-cotositerivit-from-db-by-year-and-month-and-stream")
                    .to("direct:fetch-cotositerivit-from-db-by-year-and-month")
                        .split(body()).streaming().to(mockCoTositeRivitFetchStreamed.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateCoTositePreventionTest() throws Exception {

        CamelContext ctx = producerTemplate.getCamelContext();

        AdviceWith.adviceWith(ctx,"insertSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(header(FileConstants.FILE_NAME).contains("CO_TOSITE")).to(mockJdbcSapActual.getEndpointUri());
        });

        AdviceWith.adviceWith(ctx, "insertCoTositeIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertTositeOrCoTositeRiviIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(exchangeProperty("DB_TABLE").isEqualTo("COTOSITERIVI")).to(mockJdbcSapActual.getEndpointUri());
        });

        mockJdbcSapActual.whenAnyExchangeReceived(e -> System.out.println("CO: " + e.getMessage().getBody(String.class)));
        // file + tosite1 + 2 meta lines, 1 file and tosite2 (which fails)
        mockJdbcSapActual.expectedMessageCount(5);

        String toimiala = "toimiala";
        String year = "GJAHR";
        String month = "PERIO";

        Exchange ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID016_CO_TOSITE_OUT_20250219-000123-456.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1 = createCoTositeMeta("BUKRS", "BELNR", year, month);
        LinkedHashMap<String, Object> tosite1Meta = createCoTositeMeta("BUKRS", "BELNR", year, month);
        String tosite1MetaBLDAT = "meta";
        tosite1Meta.put("BLDAT", tosite1MetaBLDAT);
        LinkedHashMap<String, Object> tosite2 = createCoTositeMeta("BUKRS", "BELNR", year, month);
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1, tosite1Meta), List.of(tosite2));
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:init-cotositerivi-db", new DefaultExchange(ctx));

        Exchange insertRes = producerTemplate.send("direct:insert-cotosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcSapActual.assertIsSatisfied();

        mockCoTositeRivitFetchStreamed.expectedMessageCount(2);

        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("PERIO", month);
        producerTemplate.send("direct:fetch-cotositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        mockCoTositeRivitFetchStreamed.assertIsSatisfied();

        List<Map<String, String>> receivedLines = mockCoTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        long tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        long tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
    }

    public LinkedHashMap<String, Object> createCoTositeMeta(String BUKRS, String BELNR, String GJAHR, String PERIO) {
        LinkedHashMap<String, Object> tosite = new LinkedHashMap<>();
        tosite.put("BUKRS", BUKRS);
        tosite.put("BELNR", BELNR);
        tosite.put("GJAHR", GJAHR);
        tosite.put("PERIO", PERIO);
        return tosite;
    }

}