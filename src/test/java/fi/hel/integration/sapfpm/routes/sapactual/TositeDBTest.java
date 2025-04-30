package fi.hel.integration.sapfpm.routes.sapactual;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.builder.Builder.exchangeProperty;
import static org.apache.camel.builder.Builder.header;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class TositeDBTest extends CamelQuarkusTestSupport {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jdbc:sapactual")
    private MockEndpoint mockJdbcSapActual;

    @EndpointInject("mock:tositerivit-fetch-streamed")
    private MockEndpoint mockTositeRivitFetchStreamed;

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream")
                    .to("direct:fetch-tositerivit-from-db-by-year-and-month")
                        .split(body()).streaming().to(mockTositeRivitFetchStreamed.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateTositePreventionTest() throws Exception {

        CamelContext ctx = producerTemplate.getCamelContext();

        AdviceWith.adviceWith(ctx, "insertSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(header(FileConstants.FILE_NAME).contains("FI_TOSITE")).to(mockJdbcSapActual.getEndpointUri());
        });

        AdviceWith.adviceWith(ctx, "insertTositeIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertTositeOrCoTositeRiviIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(exchangeProperty("DB_TABLE").isEqualTo("TOSITERIVI")).to(mockJdbcSapActual.getEndpointUri());
        });

        // 1 file, tosite1 + 2 lines, tosite2 (which fails)
        mockJdbcSapActual.expectedMessageCount(5);

        String toimiala = "toimiala";
        String year = "GJAHR";
        String month = "POPER";

        Exchange ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_1.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1 = createTositeMeta("BUKRS", "BELNR", year, month);
        LinkedHashMap<String, Object> tosite1Meta = createTositeMeta("BUKRS", "BELNR", year, month);
        String tosite1MetaBLDAT = "meta";
        tosite1Meta.put("BLDAT", tosite1MetaBLDAT);
        LinkedHashMap<String, Object> tosite2 = createTositeMeta("BUKRS", "BELNR", year, month);
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1, tosite1Meta), List.of(tosite2));
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:init-tositerivi-db", new DefaultExchange(ctx));

        mockJdbcSapActual.whenAnyExchangeReceived(e -> System.out.println("TOSITE ACTUAL: " + e.getMessage().getBody(String.class)));


        Exchange insertRes = producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcSapActual.assertIsSatisfied();
        mockTositeRivitFetchStreamed.expectedMessageCount(2);

        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        producerTemplate.send("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        mockTositeRivitFetchStreamed.assertIsSatisfied();

        List<Map<String, String>> receivedLines = mockTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        long tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        long tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);


        mockTositeRivitFetchStreamed.reset();
        mockJdbcSapActual.reset();

        // new file with duplicates + 1 new tosite
        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_2.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite3 = createTositeMeta("BUKRS_tosite3", "BELNR_tosite3", year, month);
        tositteet = List.of(List.of(tosite1, tosite1Meta), List.of(tosite2), List.of(tosite3));
        ex.getMessage().setBody(tositteet);

        // 1 file, tosite1 (which fails), tosite2 (which fails), tosite3 + rivi
        mockJdbcSapActual.expectedMessageCount(5);

        insertRes = producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcSapActual.assertIsSatisfied();

        mockTositeRivitFetchStreamed.expectedMessageCount( 3);

        fetchEx = new DefaultExchange(ctx);
        fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        producerTemplate.send("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        mockTositeRivitFetchStreamed.assertIsSatisfied();

        receivedLines = mockTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
        long tosite3Belnrs = receivedLines.stream().filter(l -> tosite3.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(1L, tosite3Belnrs);
    }

    public LinkedHashMap<String, Object> createTositeMeta(String BUKRS, String BELNR, String GJAHR, String POPER) {
        LinkedHashMap<String, Object> tosite = new LinkedHashMap<>();
        tosite.put("BUKRS", BUKRS);
        tosite.put("BELNR", BELNR);
        tosite.put("GJAHR", GJAHR);
        tosite.put("POPER", POPER);
        return tosite;
    }

}