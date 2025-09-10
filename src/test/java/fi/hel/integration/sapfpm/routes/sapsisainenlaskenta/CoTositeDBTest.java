package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;


import fi.hel.integration.sapfpm.Profiles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
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

    String testFetchCoTositeRivitUri = "direct:fetch-cotositerivit-from-db-by-year-and-month-and-stream";

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        mockJdbcSapActualCo.reset();
        mockCoTositeRivitFetchStreamed.reset();

        producerTemplate.send("direct:init-cotositerivi-db", new DefaultExchange(ctx));

        AdviceWith.adviceWith(ctx,"insertCoTositeSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActualCo.getEndpointUri());
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
                from(testFetchCoTositeRivitUri)
                    .to("direct:fetch-cotositerivit-from-db-by-year-and-month")
                        .split(body()).to(mockCoTositeRivitFetchStreamed.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateCoTositePreventionTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        // file + tosite1 + 2 meta lines, tosite2 (which fails)
        mockJdbcSapActualCo.expectedMessageCount(5);
        mockJdbcSapActualCo.whenAnyExchangeReceived(e -> {
            String sqlBody = e.getMessage().getBody(String.class);
            Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
        });

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
        producerTemplate.send(testFetchCoTositeRivitUri, fetchEx);

        mockCoTositeRivitFetchStreamed.assertIsSatisfied();

        List<Map<String, String>> receivedLines = mockCoTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        long tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        long tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
    }

    @Test
    void transactedFileAndContentsInsertTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        // 1 file, tosite1 + tosite1row, tosite2 + tosite2row + duplicate tosite2row, tosite3 + meta + meta that is then retried 10 times, no file or tositteet should be inserted due to db exception
        mockJdbcSapActualCo.expectedMessageCount(1 + 2 + 3 + 2 + 11);

        String toimiala = "coexcep";
        String year = "GJAHR";
        String month = "POPER";

        Exchange ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID016_CO_TOSITE_OUT_transaction.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1 = createCoTositeMeta("BUKRS", "BELNR1", year, month);
        LinkedHashMap<String, Object> tosite2 = createCoTositeMeta("BUKRS", "BELNR2", year, month);


        LinkedHashMap<String, Object> tosite3 = createCoTositeMeta("BUKRS", "BELNR3", year, month);
        LinkedHashMap<String, Object> tosite3Meta = createCoTositeMeta("BUKRS", "BELNR3", year, month);
        tosite3Meta.put("BLDAT", "tosite3RiviBLDAT");

        // tosite2 has 1 duplicate row
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1), List.of(tosite2, tosite2), List.of(tosite3, tosite3Meta));
        ex.getMessage().setBody(tositteet);

        SQLException thrownException = new SQLException("sql exception!");
        // inserting tosite2 fails, tosite3 should not be inserted (and tosite1 should be rolled back as well)
        mockJdbcSapActualCo.whenAnyExchangeReceived(e -> {
            String sqlBody = e.getMessage().getBody(String.class);
            Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
            if (sqlBody.contains("COTOSITERIVI") && tosite3Meta.get("BLDAT").equals(jdbcParams.get("BLDAT"))) {
                e.setException(thrownException);
            }
        });

        Exchange insertRes = producerTemplate.send("direct:insert-cotosite-file-and-contents-into-db", ex);
        // 2 splits
        assertEquals(thrownException, insertRes.getException().getCause().getCause());

        mockJdbcSapActualCo.assertIsSatisfied();
        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        fetchMsg.setHeader("pageLimit", 100);

        producerTemplate.send(testFetchCoTositeRivitUri, fetchEx);

        List<Map<String, String>> receivedLines = mockCoTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        assertTrue(receivedLines.isEmpty());

        fetchEx = new DefaultExchange(ctx);
        fetchEx.getMessage().setBody("SELECT fileName FROM COTOSITESAPFILE WHERE toimiala = '" + toimiala + "'");
        producerTemplate.send("jdbc:sapactual", fetchEx);
        List<Map<String, String>> fileNameRes = fetchEx.getMessage().getBody(List.class);
        assertTrue(fileNameRes.isEmpty());
    }

    @Test
    void fetchAllYearsAndMonthsTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "fetchAllCo";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);

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