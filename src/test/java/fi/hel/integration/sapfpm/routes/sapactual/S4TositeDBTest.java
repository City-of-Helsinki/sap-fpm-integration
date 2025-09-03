package fi.hel.integration.sapfpm.routes.sapactual;


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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.camel.builder.Builder.header;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(Profiles.S4TositeTestProfile.class)
public class S4TositeDBTest extends CamelQuarkusTestSupport {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jdbc:s4Sapactual")
    private MockEndpoint mockJdbcS4SapActual;

    @EndpointInject("mock:mockFetchS4TositeRivitFromDb")
    private MockEndpoint mockFetchS4TositeRivitFromDb;

    @EndpointInject("mock:s4-tositerivit-fetch")
    private MockEndpoint mockS4TositeRivitFetch;

    @EndpointInject("mock:any-s4-file-out")
    private MockEndpoint mockAnyS4FileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        producerTemplate.send("direct:init-s4-tositerivi-db", new DefaultExchange(producerTemplate.getCamelContext()));

        mockS4TositeRivitFetch.reset();
        mockJdbcS4SapActual.reset();
        mockAnyS4FileOut.reset();
        mockFetchS4TositeRivitFromDb.reset();

        AdviceWith.adviceWith(ctx, "insertS4TositeSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(header(FileConstants.FILE_NAME).contains("FI_TOSITE")).to(mockJdbcS4SapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertS4TositeRiviIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual?*").to(mockJdbcS4SapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "fetchS4TositeAllYearsAndMonthsAndWriteToAzure-palke", b -> {
            b.interceptSendToEndpoint("direct:fetch-s4-tositerivit-from-db-by-year-and-month").to(mockFetchS4TositeRivitFromDb.getEndpointUri());
            b.interceptSendToEndpoint("direct:any-file-out").to(mockAnyS4FileOut.getEndpointUri());
        });
    }

    String testFetchTositeS4RivitUri = "direct:fetch-s4-tositerivit-from-db-by-year-and-month-and-split";

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(testFetchTositeS4RivitUri)
                    .to("direct:fetch-s4-tositerivit-from-db-by-year-and-month")
                        .split(body()).to(mockS4TositeRivitFetch.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateTositeRowPreventionTest() throws Exception {

        CamelContext ctx = producerTemplate.getCamelContext();

        // 1 file, 2 tosite1 rows
        mockJdbcS4SapActual.expectedMessageCount(3);

        String toimiala = "toimiala";
        String year = "GJAHR";
        String month = "POPER";

        Exchange ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_1.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1Row1 = createS4TositeRow("BUKRS", "BELNR", year, month, "DOCLN1");
        tosite1Row1.put("BLDAT", "tosite1BLDAT");
        LinkedHashMap<String, Object> tosite1Row2 = createS4TositeRow("BUKRS", "BELNR", year, month, "DOCLN2");
        List<LinkedHashMap<String, Object>> tositteet = List.of(tosite1Row1, tosite1Row2);
        ex.getMessage().setBody(tositteet);

        Exchange insertRes = producerTemplate.send("direct:insert-s4-tosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcS4SapActual.assertIsSatisfied();
        mockS4TositeRivitFetch.expectedMessageCount(2);

        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        fetchMsg.setHeader("pageLimit", 100);
        producerTemplate.send(testFetchTositeS4RivitUri, fetchEx);

        mockS4TositeRivitFetch.assertIsSatisfied();

        List<Map<String, String>> receivedLines = mockS4TositeRivitFetch.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        long tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Row1.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        assertTrue(receivedLines.stream().anyMatch(l -> tosite1Row1.get("DOCLN").equals(l.get("DOCLN"))));
        assertTrue(receivedLines.stream().anyMatch(l -> tosite1Row2.get("DOCLN").equals(l.get("DOCLN"))));
        long tosite1Bldats = receivedLines.stream().filter(l -> tosite1Row1.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);


        mockS4TositeRivitFetch.reset();
        mockJdbcS4SapActual.reset();

        // new file with duplicates + 1 new tosite row
        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_2.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        // same DOCLN as tosite1Row1
        LinkedHashMap<String, Object> tosite3Row = createS4TositeRow("BUKRS_tosite3", "BELNR_tosite3", year, month, "DOCLN1");
        tositteet = List.of(tosite1Row1, tosite1Row2, tosite3Row);
        ex.getMessage().setBody(tositteet);
        // 1 new file, tosite1 (which fails), tosite2 (which fails), tosite3 row
        mockJdbcS4SapActual.expectedMessageCount(4);

        insertRes = producerTemplate.send("direct:insert-s4-tosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcS4SapActual.assertIsSatisfied();

        mockS4TositeRivitFetch.expectedMessageCount( 3);

        fetchEx = new DefaultExchange(ctx);
        fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        fetchMsg.setHeader("pageLimit", 100);
        producerTemplate.send(testFetchTositeS4RivitUri, fetchEx);

        mockS4TositeRivitFetch.assertIsSatisfied();

        receivedLines = mockS4TositeRivitFetch.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Row1.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        tosite1Bldats = receivedLines.stream().filter(l -> tosite1Row1.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
        long tosite3Belnrs = receivedLines.stream().filter(l -> tosite3Row.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(1L, tosite3Belnrs);
        assertTrue(receivedLines.stream().anyMatch(l -> tosite1Row1.get("DOCLN").equals(l.get("DOCLN"))));
        assertTrue(receivedLines.stream().anyMatch(l -> tosite1Row2.get("DOCLN").equals(l.get("DOCLN"))));
        // tosite1Row1 and tosite3Row share DOCLN
        assertEquals(2L, receivedLines.stream().filter(l -> tosite3Row.get("DOCLN").equals(l.get("DOCLN"))).count());
    }

    @Test
    void fetchAllYearsAndMonthsTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "fetchAllYe";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);

        LinkedHashMap<String, Object> tosite1 = createS4TositeRow("BUKRS", "BELNR", "2025", "12", "DOCLN1");
        LinkedHashMap<String, Object> tosite2 = createS4TositeRow("BUKRS", "BELNR", "2025", "01", "DOCLN2");
        LinkedHashMap<String, Object> tosite3 = createS4TositeRow("BUKRS", "BELNR", "2025", "05", "DOCLN3");
        LinkedHashMap<String, Object> tosite4 = createS4TositeRow("BUKRS", "BELNR", "2026", "01", "DOCLN4");
        List<LinkedHashMap<String, Object>> tositteet = List.of(tosite1, tosite2, tosite3, tosite4);
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:insert-s4-tosite-file-and-contents-into-db", ex);

        Exchange resCsv = producerTemplate.send("direct:fetch-all-s4-years-and-months-from-db", ex);
        List<LinkedHashMap<String, Object>> resBody = resCsv.getMessage().getBody(List.class);
        List<String> resYears = resBody.stream().map(r -> (String)r.get("GJAHR")).toList();
        List<String> resMonths = resBody.stream().map(r -> (String)r.get("POPER")).toList();

        List<String> expYears = List.of("2026", "2025", "2025", "2025");
        List<String> expMonths = List.of("01", "12", "05", "01");
        assertIterableEquals(expYears, resYears);
        assertIterableEquals(expMonths, resMonths);
    }

    @Test
    void fetchTositeRivitFromDbErrorTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        SQLException thrownException = new SQLException("sql exception!");

        mockFetchS4TositeRivitFromDb.whenExchangeReceived(1, e -> e.setException(thrownException));

        String POPER = "02",
                GJAHR = "2027",
                tosite1BELNR = "BELNR123",
                tosite1DOCLN = "DOCLN1",
                tosite2BELNR = "BELNR234",
                tosite2DOCLN = "DOCLN2";
        LinkedHashMap<String, Object> tosite1 = createS4TositeRow("BUKRS", tosite1BELNR, GJAHR, POPER, tosite1DOCLN);
        LinkedHashMap<String, Object> tosite2 = createS4TositeRow("BUKRS", tosite2BELNR, GJAHR, POPER, tosite2DOCLN);
        List<LinkedHashMap<String, Object>> tositteet = List.of(tosite1, tosite2);
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "palke";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:insert-s4-tosite-file-and-contents-into-db", ex);

        AtomicInteger tosite1Count = new AtomicInteger(0),
                tosite2Count = new AtomicInteger(0);
        mockAnyS4FileOut.whenAnyExchangeReceived(e -> {
            String body = e.getMessage().getBody(String.class);
            if (body.contains(tosite1BELNR)) {
                tosite1Count.incrementAndGet();
            } else if (body.contains(tosite2BELNR)) {
                tosite2Count.incrementAndGet();
            }
        });
        mockAnyS4FileOut.expectedMessageCount(3); // header + two tosite
        mockFetchS4TositeRivitFromDb.expectedMessageCount(4);

        producerTemplate.send("direct:fetch-s4-tositteet-from-db-and-write-to-azure-palke", new DefaultExchange(ctx));
        mockAnyS4FileOut.assertIsSatisfied();
        mockFetchS4TositeRivitFromDb.assertIsSatisfied();
        assertEquals(1, tosite1Count.get());
        assertEquals(1, tosite2Count.get());

        mockAnyS4FileOut.reset();
        mockFetchS4TositeRivitFromDb.reset();

        mockFetchS4TositeRivitFromDb.whenAnyExchangeReceived(e -> e.setException(thrownException));

        mockAnyS4FileOut.expectedMessageCount(1);
        mockFetchS4TositeRivitFromDb.expectedMessageCount(11);

        ex = new DefaultExchange(ctx);
        producerTemplate.send("direct:fetch-s4-tositteet-from-db-and-write-to-azure-palke", ex);

        mockAnyS4FileOut.assertIsSatisfied();
        mockFetchS4TositeRivitFromDb.assertIsSatisfied();
        assertNotNull(ex.getException());
        assertEquals(thrownException, ex.getException(SQLException.class));
    }


    @Test
    void fetchLatestCreatedTimestampTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        String toimiala = "latest";

        Exchange ex = new DefaultExchange(ctx);
        List<String> createdTimestamps = List.of("2025-08-01 10:10:11.000001", "2025-01-01 00:00:00.000001", "2025-08-01 10:10:13.000001");
        ex.getMessage().setBody("INSERT INTO S4TOSITERIVI(createdTimestamp, fileName, toimiala, BUKRS, BELNR, GJAHR, POPER, DOCLN) VALUES (" +
            createdTimestamps.stream().map(t -> "'" + t + "','file','" + toimiala + "','BUKRS','BELNR','GJAHR','POPER','DOCLN_" + t + "'").collect(Collectors.joining("), ("))
                + ");");
        producerTemplate.send("jdbc:sapactual", ex);

        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("toimiala", toimiala);
        Exchange resCsv = producerTemplate.send("direct:fetch-latest-s4-createdtimestamp-from-db", ex);
        LinkedHashMap<String, Object> resBody = (LinkedHashMap<String, Object>)resCsv.getMessage().getBody(List.class).get(0);
        Timestamp createdTimestamp = (Timestamp)resBody.get("createdTimestamp");
        assertEquals(createdTimestamps.get(2), createdTimestamp.toString());
    }

    @Test
    void fetchLatelyChangedYearsAndMonthsTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        String toimiala = "lately";

        Exchange ex = new DefaultExchange(ctx);
        List<TsWithPOPERGJAHR> created = List.of(
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 15, 0, 0), "POPER1", "GJAHR1", "DOCLN_1"),
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 14, 0, 0), "POPER2", "GJAHR2", "DOCLN_2"),
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 1, 0, 0), "POPER3", "GJAHR3", "DOCLN_3")
        );

        ex.getMessage().setBody("INSERT INTO S4TOSITERIVI(createdTimestamp, fileName, toimiala, BUKRS, BELNR, GJAHR, POPER, DOCLN) VALUES (" +
                created.stream().map(t -> "'" + t.creationTimestamp + "','file','" + toimiala + "','BUKRS','BELNR','" + t.GJAHR + "','" + t.POPER + "','" + t.DOCLN + "'").collect(Collectors.joining("), ("))
                + ");");
        producerTemplate.send("jdbc:sapactual", ex);

        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("toimiala", toimiala);
        TsWithPOPERGJAHR ts = created.getFirst(); // latest
        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 0);
        Exchange resCsv = producerTemplate.send("direct:fetch-s4-changed-years-and-months-from-db", ex);
        List<LinkedHashMap<String, Object>> resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(1, resBody.size());
        assertEquals(ts.POPER(), resBody.getFirst().get("POPER").toString());
        assertEquals(ts.GJAHR(), resBody.getFirst().get("GJAHR").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 1);
        resCsv = producerTemplate.send("direct:fetch-s4-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(2, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString()); // ordered by DESC POPER + GJAHR
        assertEquals(created.get(1).POPER(), resBody.getFirst().get("POPER").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 13);
        resCsv = producerTemplate.send("direct:fetch-s4-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(2, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString());
        assertEquals(created.get(1).POPER(), resBody.getFirst().get("POPER").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 14);
        resCsv = producerTemplate.send("direct:fetch-s4-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(3, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString());
        assertEquals(created.get(1).POPER(), resBody.get(1).get("POPER").toString());
        assertEquals(created.getLast().POPER(), resBody.getFirst().get("POPER").toString());
    }

    public LinkedHashMap<String, Object> createS4TositeRow(String BUKRS, String BELNR, String GJAHR, String POPER, String DOCLN) {
        LinkedHashMap<String, Object> tosite = new LinkedHashMap<>();
        tosite.put("BUKRS", BUKRS);
        tosite.put("BELNR", BELNR);
        tosite.put("GJAHR", GJAHR);
        tosite.put("POPER", POPER);
        tosite.put("DOCLN", DOCLN);
        return tosite;
    }

    public Timestamp toTimestamp(LocalDateTime t) {
        return Timestamp.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }

    record TsWithPOPERGJAHR(LocalDateTime creationTimestamp, String POPER, String GJAHR, String DOCLN) {}
}