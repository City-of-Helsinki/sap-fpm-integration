package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import fi.hel.integration.sapfpm.routes.CoToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

// ID***_CO_TOSITE_***20250217-000705-001
@ApplicationScoped
public class COTositeInRouteBuilder extends CoToteumatRouteBuilder {
    @ConfigProperty(name = "tosite-db.page-limit", defaultValue = "1000")
    int DB_PAGE_LIMIT;

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
        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-Cotosite-%s".formatted(toimiala);
        from(marshalWithHeaderCsvURI).marshal(csvDataFormatWithHeader);

        String initRouteUri = "direct:init-cototeumat-route";
        from(initRouteUri).setHeader("toimiala", constant(toimiala)).to("direct:init-cotositerivi-db");

        String processFileRouteUri = "direct:process-cotosite-file";
        from(processFileRouteUri)
            .to("direct:unmarshal-xml")
            .to("direct:process-cotosite-file-contents")
            .to("direct:insert-cotosite-file-and-contents-into-db");

        // from timer, if no starting fetch time or if last time sent > x
        // set starting fetch time, then set last sent time
       // from("timer://").to("direct:fetch-cotositteet-from-db-and-write-to-azure-" + toimiala);

        String initDbFetchParamsAndFileNameUri = "direct:init-cotosite-db-fetch-params";
        from(initDbFetchParamsAndFileNameUri)
            .process(e -> {
                LinkedHashMap<String, Object> row = e.getMessage().getBody(LinkedHashMap.class);
                String year = (String)row.get("GJAHR");
                String month = (String)row.get("PERIO");
                e.getMessage().setHeader("GJAHR", year);
                e.getMessage().setHeader("PERIO", month);
                String simpleMonth = month.replaceFirst("^0+", "");
                e.getMessage().setHeader(FileConstants.FILE_NAME, "SAPSISAINENLASKENTA_" + year + "_" + simpleMonth + ".csv");
            });

        String sendFileToAzureUri = "direct:enrich-and-send-file-to-azure-" + toimiala;
        String fetchCoToteumatRouteUri = "direct:fetch-cotositteet-from-db-and-write-to-azure-" + toimiala;

        buildFileAppendingFromDbPageRoute(fetchCoToteumatRouteUri,  "fetchCoTositeAllYearsAndMonthsAndWriteToAzure-%s".formatted(toimiala), toimiala,
                "direct:fetch-all-cotosite-years-and-months-from-db", initDbFetchParamsAndFileNameUri,
                marshalWithHeaderCsvURI, "direct:fetch-cotosite-years-and-months-count-from-db", DB_PAGE_LIMIT,
                "direct:fetch-cotositerivit-from-db-by-year-and-month", marshalHeaderlessCsvURI, sendFileToAzureUri);

        buildFtpBatchingRoute(fileOrFtpIn, toimiala + "cotositeIn", initRouteUri, processFileRouteUri, fetchCoToteumatRouteUri);

        from("file:trigger/cotosite-write-" + toimiala + "?delete=true").to(fetchCoToteumatRouteUri);
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
    public String getFtpDir(String toimiala) {
        return ""; // no subdir
    }
}

