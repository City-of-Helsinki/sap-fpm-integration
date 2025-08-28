package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;


import fi.hel.integration.sapfpm.Profiles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.builder.Builder.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(Profiles.CoTositeTestProfile.class)
public class CoTositeDBTest extends CamelQuarkusTestSupport {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jdbc:sapactualco")
    private MockEndpoint mockJdbcSapActualCo;

    @EndpointInject("mock:cotositerivit-fetch-streamed")
    private MockEndpoint mockCoTositeRivitFetchStreamed;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        mockJdbcSapActualCo.reset();
        mockCoTositeRivitFetchStreamed.reset();

        AdviceWith.adviceWith(ctx,"insertCoTositeSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(header(FileConstants.FILE_NAME).contains("CO_TOSITE")).to(mockJdbcSapActualCo.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertCoTositeIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActualCo.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertCoTositeRiviIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActualCo.getEndpointUri());
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:fetch-cotositerivit-from-db-by-year-and-month-and-stream")
                    .to("direct:fetch-cotositerivit-from-db-by-year-and-month")
                        .split(body()).to(mockCoTositeRivitFetchStreamed.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateCoTositePreventionTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        // file + tosite1 + 2 meta lines, 1 file and tosite2 (which fails)
        mockJdbcSapActualCo.expectedMessageCount(5);

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

        mockJdbcSapActualCo.assertIsSatisfied();

        mockCoTositeRivitFetchStreamed.expectedMessageCount(2);

        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("PERIO", month);
        fetchMsg.setHeader("pageLimit", 100);
        producerTemplate.send("direct:fetch-cotositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        mockCoTositeRivitFetchStreamed.assertIsSatisfied();

        List<Map<String, String>> receivedLines = mockCoTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        long tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        long tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
    }

    @Test
    void fetchAllYearsAndMonthsTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "fetchAllCo";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);

        producerTemplate.send("direct:init-cotositerivi-db", new DefaultExchange(ctx));

        LinkedHashMap<String, Object> tosite1 = createCoTositeMeta("BUKRS", "BELNR", "2025", "12");
        LinkedHashMap<String, Object> tosite2 = createCoTositeMeta("BUKRS", "BELNR", "2025", "01");
        LinkedHashMap<String, Object> tosite3 = createCoTositeMeta("BUKRS", "BELNR", "2026", "01");
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1), List.of(tosite2), List.of(tosite3));
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:insert-cotosite-file-and-contents-into-db", ex);

        Exchange resCsv = producerTemplate.send("direct:fetch-all-cotosite-years-and-months-from-db", ex);
        List<LinkedHashMap<String, Object>> resBody = resCsv.getMessage().getBody(List.class);
        List<String> resYears = resBody.stream().map(r -> (String)r.get("GJAHR")).toList();
        List<String> resMonths = resBody.stream().map(r -> (String)r.get("PERIO")).toList();

        List<String> expYears = List.of("2026", "2025", "2025");
        List<String> expMonths = List.of("01", "12", "01");
        assertIterableEquals(expYears, resYears);
        assertIterableEquals(expMonths, resMonths);
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