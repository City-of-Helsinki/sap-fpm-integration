package fi.hel.integration.sapfpm.routes.sapactual;


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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.camel.builder.Builder.exchangeProperty;
import static org.apache.camel.builder.Builder.header;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(Profiles.TositeTestProfile.class)
public class TositeDBTest extends CamelQuarkusTestSupport {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jdbc:sapactual")
    private MockEndpoint mockJdbcSapActual;

    @EndpointInject("mock:mockFetchTositeRivitFromDb")
    private MockEndpoint mockFetchTositeRivitFromDb;

    @EndpointInject("mock:tositerivit-fetch-streamed")
    private MockEndpoint mockTositeRivitFetchStreamed;

    @EndpointInject("mock:any-file-out")
    private MockEndpoint mockAnyFileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        mockTositeRivitFetchStreamed.reset();
        mockJdbcSapActual.reset();
        mockAnyFileOut.reset();
        mockFetchTositeRivitFromDb.reset();

        producerTemplate.send("direct:init-tositerivi-db", new DefaultExchange(ctx));

        AdviceWith.adviceWith(ctx, "insertTositeSapFileIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").onWhen(header(FileConstants.FILE_NAME).contains("FI_TOSITE")).to(mockJdbcSapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertTositeIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual*").to(mockJdbcSapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "insertTositeRiviIntoDb", b -> {
            b.interceptSendToEndpoint("jdbc:sapactual?*").to(mockJdbcSapActual.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "fetchTositeAllYearsAndMonthsAndWriteToAzure-palke", b -> {
            b.interceptSendToEndpoint("direct:any-file-out").to(mockAnyFileOut.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "appendTositeFromDb-palke", b -> {
            b.interceptSendToEndpoint("direct:fetch-tositerivit-from-db-by-year-and-month").to(mockFetchTositeRivitFromDb.getEndpointUri());
            b.interceptSendToEndpoint("direct:any-file-out").to(mockAnyFileOut.getEndpointUri());
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream")
                    .to("direct:fetch-tositerivit-from-db-by-year-and-month")
                        .split(body()).to(mockTositeRivitFetchStreamed.getEndpointUri()).end();
            }
        };
    }

    @Test
    void duplicateTositePreventionTest() throws Exception {

        CamelContext ctx = producerTemplate.getCamelContext();

        // 1 file, tosite1 + 2 lines, tosite2 (which fails)
        mockJdbcSapActual.expectedMessageCount(5);

        String toimiala = "toimiala";
        String year = "GJAHR";
        String month = "POPER";

        Exchange ex = new DefaultExchange(ctx);
        String fileName1 = "ID022_FI_TOSITE_OUT_1.xml";
        ex.getMessage().setHeader("CamelFileName", fileName1);
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1 = createTositeMeta("BUKRS", "BELNR", year, month);
        LinkedHashMap<String, Object> tosite1Meta = createTositeMeta("BUKRS", "BELNR", year, month);
        String tosite1MetaBLDAT = "meta";
        tosite1Meta.put("BLDAT", tosite1MetaBLDAT);
        LinkedHashMap<String, Object> tosite2 = createTositeMeta("BUKRS", "BELNR", year, month);
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1, tosite1Meta), List.of(tosite2));
        ex.getMessage().setBody(tositteet);

        Exchange insertRes = producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);
        assertNull(insertRes.getException());

        mockJdbcSapActual.assertIsSatisfied();
        mockTositeRivitFetchStreamed.expectedMessageCount(2);

        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        fetchMsg.setHeader("pageLimit", 100);
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
        String fileName2 = "ID022_FI_TOSITE_OUT_2.xml";
        ex.getMessage().setHeader("CamelFileName", fileName2);
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
        fetchMsg.setHeader("pageLimit", 100);
        producerTemplate.send("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        mockTositeRivitFetchStreamed.assertIsSatisfied();

        receivedLines = mockTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        tosite1Belnrs = receivedLines.stream().filter(l -> tosite1Meta.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(2L, tosite1Belnrs);
        tosite1Bldats = receivedLines.stream().filter(l -> tosite1Meta.get("BLDAT").equals(l.get("BLDAT"))).count();
        assertEquals(1L, tosite1Bldats);
        long tosite3Belnrs = receivedLines.stream().filter(l -> tosite3.get("BELNR").equals(l.get("BELNR"))).count();
        assertEquals(1L, tosite3Belnrs);

        fetchEx = new DefaultExchange(ctx);
        fetchEx.getMessage().setBody("SELECT fileName FROM TOSITESAPFILE WHERE toimiala = '" + toimiala + "'");
        producerTemplate.send("jdbc:sapactual", fetchEx);
        List<Map<String, String>> fileNameRes = fetchEx.getMessage().getBody(List.class);
        assertTrue(fileNameRes.stream().anyMatch(f -> f.get("fileName").equals(fileName1)));
        assertTrue(fileNameRes.stream().anyMatch(f -> f.get("fileName").equals(fileName2)));
    }

    @Test
    void transactedFileAndContentsInsertTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();

        // 1 file, tosite1 + tosite1meta + tosite2meta which fails and then retried 10 times, no file or tositteet should be inserted due to db exception
        mockJdbcSapActual.expectedMessageCount(1 + 2 + 12);

        String toimiala = "excep";
        String year = "GJAHR";
        String month = "POPER";

        Exchange ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_transaction.xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        LinkedHashMap<String, Object> tosite1 = createTositeMeta("BUKRS", "BELNR1", year, month);
        LinkedHashMap<String, Object> tosite1Meta1 = createTositeMeta("BUKRS", "BELNR1", year, month);
        tosite1Meta1.put("BLDAT", "excBLDAT1");
        LinkedHashMap<String, Object> tosite1Meta2 = createTositeMeta("BUKRS", "BELNR1", year, month);
        tosite1Meta2.put("BLDAT", "excBLDAT2");
        LinkedHashMap<String, Object> tosite2 = createTositeMeta("BUKRS", "BELNR2", year, month);
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1, tosite1Meta1, tosite1Meta2), List.of(tosite2));
        ex.getMessage().setBody(tositteet);

        SQLException thrownException = new SQLException("sql exception!");
        mockJdbcSapActual.whenAnyExchangeReceived(e -> {
            String sqlBody = e.getMessage().getBody(String.class);
            Map<String, String> jdbcParams = e.getMessage().getHeader(JdbcConstants.JDBC_PARAMETERS, Map.class);
            assertNotEquals(tosite2.get("BELNR"), jdbcParams.get("BELNR")); // shouldn't proceed to inserting tosite2
            if (sqlBody.contains("TOSITERIVI") && tosite1Meta2.get("BLDAT").equals(jdbcParams.get("BLDAT"))) {
                e.setException(thrownException);
            }
        });

        Exchange insertRes = producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);
        // split + split
        assertEquals(thrownException, insertRes.getException().getCause().getCause());

        mockJdbcSapActual.assertIsSatisfied();
        Exchange fetchEx = new DefaultExchange(ctx);
        Message fetchMsg = fetchEx.getMessage();
        fetchMsg.setHeader("toimiala", toimiala);
        fetchMsg.setHeader("GJAHR", year);
        fetchMsg.setHeader("POPER", month);
        fetchMsg.setHeader("pageLimit", 100);
        producerTemplate.send("direct:fetch-tositerivit-from-db-by-year-and-month-and-stream", fetchEx);

        List<Map<String, String>> receivedLines = mockTositeRivitFetchStreamed.getExchanges().stream().map(e -> (Map<String, String>)e.getMessage().getBody(Map.class)).toList();
        assertTrue(receivedLines.isEmpty());

        fetchEx = new DefaultExchange(ctx);
        fetchEx.getMessage().setBody("SELECT fileName FROM TOSITESAPFILE WHERE toimiala = '" + toimiala + "'");
        producerTemplate.send("jdbc:sapactual", fetchEx);
        List<Map<String, String>> fileNameRes = fetchEx.getMessage().getBody(List.class);
        assertTrue(fileNameRes.isEmpty());

        fetchEx = new DefaultExchange(ctx);
        fetchEx.getMessage().setBody("SELECT * FROM TOSITE WHERE toimiala = '" + toimiala + "'");
        producerTemplate.send("jdbc:sapactual", fetchEx);
        List<Map<String, String>> tositeRes = fetchEx.getMessage().getBody(List.class);
        assertTrue(tositeRes.isEmpty());
    }

    @Test
    void fetchAllYearsAndMonthsTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "fetchAllYe";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);

        LinkedHashMap<String, Object> tosite1 = createTositeMeta("BUKRS", "BELNR", "2025", "12");
        LinkedHashMap<String, Object> tosite2 = createTositeMeta("BUKRS", "BELNR", "2025", "01");
        LinkedHashMap<String, Object> tosite3 = createTositeMeta("BUKRS", "BELNR", "2025", "05");
        LinkedHashMap<String, Object> tosite4 = createTositeMeta("BUKRS", "BELNR", "2026", "01");
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1), List.of(tosite2), List.of(tosite3), List.of(tosite4));
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);

        Exchange resCsv = producerTemplate.send("direct:fetch-all-years-and-months-from-db", ex);
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

        mockFetchTositeRivitFromDb.whenExchangeReceived(1, e -> e.setException(thrownException));

        String POPER = "02",
                GJAHR = "2027",
                tosite1BELNR = "BELNR123",
                tosite2BELNR = "BELNR234";
        LinkedHashMap<String, Object> tosite1 = createTositeMeta("BUKRS", tosite1BELNR, GJAHR, POPER);
        LinkedHashMap<String, Object> tosite2 = createTositeMeta("BUKRS", tosite2BELNR, GJAHR, POPER);
        List<List<LinkedHashMap<String, Object>>> tositteet = List.of(List.of(tosite1), List.of(tosite2));
        Exchange ex = new DefaultExchange(ctx);
        String toimiala = "palke";
        ex.getMessage().setHeader("CamelFileName", toimiala + ".xml");
        ex.getMessage().setHeader("toimiala", toimiala);
        ex.getMessage().setBody(tositteet);

        producerTemplate.send("direct:insert-tosite-file-and-contents-into-db", ex);

        AtomicInteger tosite1Count = new AtomicInteger(0),
                tosite2Count = new AtomicInteger(0);
        mockAnyFileOut.whenAnyExchangeReceived(e -> {
            String body = e.getMessage().getBody(String.class);
            if (body.contains(tosite1BELNR)) {
                tosite1Count.incrementAndGet();
            } else if (body.contains(tosite2BELNR)) {
                tosite2Count.incrementAndGet();
            }
        });
        mockAnyFileOut.expectedMessageCount(3); // header + two tosite
        mockFetchTositeRivitFromDb.expectedMessageCount(4);

        producerTemplate.send("direct:fetch-tositteet-from-db-and-write-to-azure-palke", new DefaultExchange(ctx));
        mockAnyFileOut.assertIsSatisfied();
        mockFetchTositeRivitFromDb.assertIsSatisfied();
        assertEquals(1, tosite1Count.get());
        assertEquals(1, tosite2Count.get());

        mockAnyFileOut.reset();
        mockFetchTositeRivitFromDb.reset();

        mockFetchTositeRivitFromDb.whenAnyExchangeReceived(e -> e.setException(thrownException));

        mockAnyFileOut.expectedMessageCount(1);
        mockFetchTositeRivitFromDb.expectedMessageCount(11);

        ex = new DefaultExchange(ctx);
        producerTemplate.send("direct:fetch-tositteet-from-db-and-write-to-azure-palke", ex);

        mockAnyFileOut.assertIsSatisfied();
        mockFetchTositeRivitFromDb.assertIsSatisfied();
        assertNotNull(ex.getException());
        assertEquals(thrownException, ex.getException(SQLException.class));
    }


    @Test
    void fetchLatestCreatedTimestampTest() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        String toimiala = "latest";

        Exchange ex = new DefaultExchange(ctx);
        List<String> createdTimestamps = List.of("2025-08-01 10:10:11.000001", "2025-01-01 00:00:00.000001", "2025-08-01 10:10:13.000001");

        ex.getMessage().setBody("INSERT INTO TOSITERIVI(createdTimestamp, fileName, toimiala, BUKRS, BELNR, GJAHR, POPER) VALUES (" +
            createdTimestamps.stream().map(t -> "'" + t + "','file','" + toimiala + "','BUKRS','BELNR','GJAHR','POPER'").collect(Collectors.joining("), ("))
                + ");");
        producerTemplate.send("jdbc:sapactual", ex);

        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("toimiala", toimiala);
        Exchange resCsv = producerTemplate.send("direct:fetch-latest-createdtimestamp-from-db", ex);
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
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 15, 0, 0), "POPER1", "GJAHR1"),
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 14, 0, 0), "POPER2", "GJAHR2"),
            new TsWithPOPERGJAHR(LocalDateTime.of(2025, 1, 1, 0, 0), "POPER3", "GJAHR3")
        );

        ex.getMessage().setBody("INSERT INTO TOSITERIVI(createdTimestamp, fileName, toimiala, BUKRS, BELNR, GJAHR, POPER) VALUES (" +
                created.stream().map(t -> "'" + t.creationTimestamp + "','file','" + toimiala + "','BUKRS','BELNR','" + t.GJAHR + "','" + t.POPER + "'").collect(Collectors.joining("), ("))
                + ");");
        producerTemplate.send("jdbc:sapactual", ex);

        ex = new DefaultExchange(ctx);
        ex.getMessage().setHeader("toimiala", toimiala);
        TsWithPOPERGJAHR ts = created.getFirst(); // latest
        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 0);
        Exchange resCsv = producerTemplate.send("direct:fetch-changed-years-and-months-from-db", ex);
        List<LinkedHashMap<String, Object>> resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(1, resBody.size());
        assertEquals(ts.POPER(), resBody.getFirst().get("POPER").toString());
        assertEquals(ts.GJAHR(), resBody.getFirst().get("GJAHR").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 1);
        resCsv = producerTemplate.send("direct:fetch-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(2, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString()); // ordered by DESC POPER + GJAHR
        assertEquals(created.get(1).POPER(), resBody.getFirst().get("POPER").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 13);
        resCsv = producerTemplate.send("direct:fetch-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(2, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString());
        assertEquals(created.get(1).POPER(), resBody.getFirst().get("POPER").toString());

        ex.getMessage().setHeader("latestCreatedTimestamp", toTimestamp(ts.creationTimestamp));
        ex.getMessage().setHeader("daysToSubtract", 14);
        resCsv = producerTemplate.send("direct:fetch-changed-years-and-months-from-db", ex);
        resBody = resCsv.getMessage().getBody(List.class);
        assertEquals(3, resBody.size());
        assertEquals(created.getFirst().POPER(), resBody.getLast().get("POPER").toString());
        assertEquals(created.get(1).POPER(), resBody.get(1).get("POPER").toString());
        assertEquals(created.getLast().POPER(), resBody.getFirst().get("POPER").toString());
    }

    public LinkedHashMap<String, Object> createTositeMeta(String BUKRS, String BELNR, String GJAHR, String POPER) {
        LinkedHashMap<String, Object> tosite = new LinkedHashMap<>();
        tosite.put("BUKRS", BUKRS);
        tosite.put("BELNR", BELNR);
        tosite.put("GJAHR", GJAHR);
        tosite.put("POPER", POPER);
        return tosite;
    }

    public Timestamp toTimestamp(LocalDateTime t) {
        return Timestamp.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }

    record TsWithPOPERGJAHR(LocalDateTime creationTimestamp, String POPER, String GJAHR) {}
}