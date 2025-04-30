package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import fi.hel.integration.sapfpm.routes.CoToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// CO_TOSITE MYÖS PER KAUDET !!! eli
// yksilöivät BUKRS BELNR GJAHR PERIO (PERIO on MONAT)
//

// ID***_CO_TOSITE_***20250217-000705-001
@ApplicationScoped
public class COTositeInRouteBuilder extends CoToteumatRouteBuilder {
    final static int DB_PAGE_LIMIT = 100000;

    public String[] createCsvHeader() {
        return new String[] {
                "BELNR", "BLDAT", "BUDAT", "CPUDT", "BLART",
                "REFBN", "VERSN", "AWTYP", "AWORG", "BUZEI",
                "PERIO", "WOGBTR", "OBJNR", "OBJ_TYPE", "TYPE_NR",
                "PRCTR", "GJAHR", "KSTAR", "BEKNZ", "BUKRS", "SGTXT", "FKBER"
        };
    }

    // ZCODCMT shared vals, ZCODCMT.BUZEI receipt vals
    public LinkedHashMap<String, Object> extractValues(Map<String, Object> ZCODCMT, Map<String, Object> BUZEI) {
        LinkedHashMap<String, Object> r = new LinkedHashMap<>(); // order matters
        r.put("BELNR", ZCODCMT.get("BELNR")); // osa id:tä jos tarvitaan stac
        r.put("BLDAT", ZCODCMT.get("BLDAT")); //
        r.put("BUDAT", ZCODCMT.get("BUDAT"));
        r.put("CPUDT", ZCODCMT.get("CPUDT"));
        r.put("BLART", ZCODCMT.get("BLART"));
        r.put("REFBN", ZCODCMT.get("REFBN"));
        r.put("VERSN", ZCODCMT.get("VERSN"));
        r.put("AWTYP", ZCODCMT.get("AWTYP"));
        r.put("AWORG", ZCODCMT.get("AWORG"));

        r.put("BUZEI", BUZEI.get("BUZEI"));
        r.put("PERIO", BUZEI.get("PERIO"));    // osa id:tä, kirjauskuukausi
        r.put("WOGBTR", BUZEI.get("WOGBTR"));
        r.put("OBJNR", BUZEI.get("OBJNR"));
        r.put("OBJ_TYPE", BUZEI.get("OBJ_TYPE"));
        r.put("TYPE_NR", BUZEI.get("TYPE_NR"));
        r.put("PRCTR", BUZEI.get("PRCTR"));
        r.put("GJAHR", BUZEI.get("GJAHR"));    // osa id:tä
        r.put("KSTAR", BUZEI.get("KSTAR"));
        r.put("BEKNZ", BUZEI.get("BEKNZ"));
        r.put("BUKRS", BUZEI.get("BUKRS"));
        r.put("SGTXT", BUZEI.get("SGTXT"));
        r.put("FKBER", BUZEI.get("FKBER"));
        return r;
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {

        CsvDataFormat csvDataFormatWithHeader = createCsvDataFormat().setSkipHeaderRecord(false);
        CsvDataFormat csvDataFormatWithoutHeader = createCsvDataFormat().setSkipHeaderRecord(true);

        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-Cotosite-%s".formatted(toimiala);
        from(marshalHeaderlessCsvURI).routeId("cotositeHeaderlessCsv")
                .marshal(csvDataFormatWithoutHeader);

        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        AtomicInteger processedFileAmount = new AtomicInteger(0);

        from(fileOrFtpIn).id(toimiala + "cotositeIn")
            .setHeader("toimiala", constant(toimiala))
            .setProperty("originalBody", body())
            .to("direct:init-cotositerivi-db")
            // choice, if processed, skip
            .choice()
                .when(simple("${exchangeProperty.CamelBatchIndex} == 0"))
                .process(e -> {
                    if (initialBatchSize.get() == -1) {
                        initialBatchSize.set(e.getProperty("CamelBatchSize", Integer.class));
                        log.info("set initial batch size to " + initialBatchSize.get());
                    }
                })
                .end()
                .log("read ${headers.CamelFileName}")
                .setProperty("originalCamelFileName", header("CamelFileName"))
                .to("direct:unmarshal-xml")
                .to("direct:process-cotosite-file-contents")
                .to("direct:insert-cotosite-file-and-contents-into-db")
                .log("batch size: ${exchangeProperty.CamelBatchSize}, i: ${exchangeProperty.CamelBatchIndex}, done: ${exchangeProperty.CamelBatchComplete}")
                .process(e -> e.getMessage().setBody(""))
                .process(e -> {
                    int newTotal = processedFileAmount.addAndGet(1);
                    e.setProperty("processedFileAmount", newTotal);
                    int initial = initialBatchSize.get();
                    if (newTotal == initial) {
                        e.setProperty("writeOut", true);
                    }
                    e.setProperty("initialBatchSize", initial);
                })
                .choice().when(simple("${exchangeProperty.writeOut} == true"))
                    .log("CoTosite Done! Write unique csvs from db")
                    .to("direct:fetch-cotositteet-from-db-and-write-to-azure-" + toimiala)
                    .process(e -> {
                        e.setProperty("writeOut", false);
                        processedFileAmount.set(0);
                        initialBatchSize.set(-1);
                    })
                .end();

        from("file:trigger/cotosite-write-" + toimiala + "?delete=true").to("direct:fetch-cotositteet-from-db-and-write-to-azure-" + toimiala);

        from("direct:fetch-cotositteet-from-db-and-write-to-azure-" + toimiala)
            .setHeader("toimiala", constant(toimiala))
            .log("Writing cotosite db out to azure for ${headers.toimiala}")
            .to("direct:fetch-all-cotosite-years-and-months-from-db")
            .split(body())
                .process(e -> {
                    LinkedHashMap<String, Object> row = e.getMessage().getBody(LinkedHashMap.class);
                    String year = (String)row.get("GJAHR");
                    String month = (String)row.get("PERIO");
                    e.getMessage().setHeader("GJAHR", year);
                    e.getMessage().setHeader("PERIO", month);
                    String simpleMonth = month.replaceFirst("^0+", "");
                    e.getMessage().setHeader(FileConstants.FILE_NAME, "SAPSISAINENLASKENTA_" + year + "_" + simpleMonth + ".csv");
                })
                .setProperty("fileExist", constant("Override"))
                .setProperty("outDir", constant(toimiala))
                .setBody(constant(""))
                .marshal(csvDataFormatWithHeader)
                .to("direct:any-file-out")
                .setProperty("fileExist", constant("Append"))
                // create file first by writing only the header into the file, then stream and append
                .setHeader("pageLimit", constant(DB_PAGE_LIMIT))
                .setProperty("dbHasMoreResults", constant(true))
                .loopDoWhile(exchangeProperty("dbHasMoreResults").isEqualTo(true))
                    .to("direct:fetch-cotositerivit-from-db-by-year-and-month")
                    .process(e -> {
                        List<LinkedHashMap<String, Object>> res = e.getMessage().getBody(List.class);
                        if (res == null || res.isEmpty() || res.size() < DB_PAGE_LIMIT) {
                            e.removeProperty("dbHasMoreResults");
                            e.getMessage().removeHeader("lastId");
                        } else {
                            e.getMessage().setHeader("lastId", res.getLast().get("ID"));
                        }
                    })
                    .to(marshalHeaderlessCsvURI)
                    .to("direct:any-file-out")
                    .setBody(constant(""))
                .end()
                .log("appending done, enriching and sending to azure")
                .to("direct:enrich-and-send-file-to-azure-" + toimiala)
                .setBody(constant(""))
            .end().log("done, all months and years written");
    }

    @Override
    public void buildSupportingRoutes() {
        from("direct:process-cotosite-file-contents")
            .process(e -> {
                // get each ZCODCMT, from them each BUZEI and map those
                Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
                Object valuesObj = xmlRoot.get("ZCODCMT");

                List<Map<String, Object>> allZcodcmts;

                if (valuesObj instanceof List valList) {
                    allZcodcmts = valList;
                } else {
                    allZcodcmts = List.of((LinkedHashMap<String, Object>) valuesObj);
                }
                // list of linkedhashmaps where each map -> csv line
                // ZCODCMT contains commonValues (otsikkotaso) and ZCODCMT.BUZEI receipt values (rivitaso)
                List<List<LinkedHashMap<String, Object>>> receipts = allZcodcmts.stream().map(zcodcmtMain -> {
                    Object receiptSegs = zcodcmtMain.get("BUZEI");
                    if (receiptSegs instanceof List receiptValList) {
                        List<Map<String, Object>> receiptVals = receiptValList;
                        return receiptVals.stream().map(v -> extractValues(zcodcmtMain, v)).toList();
                    } else {
                        Map<String, Object> v = (LinkedHashMap<String, Object>) receiptSegs;
                        return List.of(extractValues(zcodcmtMain, v));
                    }
                }).toList();

                e.getMessage().setBody(receipts);
            }).id("ProcessCoTositeOut");
    }

    @Override
    public String getFilePrefix() {
        return ".*CO_TOSITE_";
    }

    @Override
    public String getFtpDir() {
        return ""; // no subdir
    }
}

