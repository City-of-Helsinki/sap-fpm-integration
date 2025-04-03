package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;

import fi.hel.integration.sapfpm.aggregationstrategy.AggregateLinesWithoutStacking;
import fi.hel.integration.sapfpm.routes.CoToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;
import java.util.stream.Stream;


// ID***_CO_TOSITE_***20250217-000705-001
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class COTositeInRouteBuilder extends CoToteumatRouteBuilder {
    CsvDataFormat coTositeCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
        "BELNR", "BLDAT", "BUDAT", "CPUDT", "BLART",
        "REFBN", "VERSN", "AWTYP", "AWORG", "BUZEI",
        "PERIO", "WOGBTR", "OBJNR", "OBJ_TYPE", "TYPE_NR",
        "PRCTR", "GJAHR", "KSTAR", "BEKNZ", "BUKRS", "SGTXT", "FKBER"
    });

    // ZCODCMT shared vals, ZCODCMT.BUZEI receipt vals
    public LinkedHashMap<String, Object> extractValues(Map<String, Object> ZCODCMT, Map<String, Object> BUZEI) {
        LinkedHashMap<String, Object> r = new LinkedHashMap<>(); // order matters
        r.put("BELNR", ZCODCMT.get("BELNR"));
        r.put("BLDAT", ZCODCMT.get("BLDAT"));
        r.put("BUDAT", ZCODCMT.get("BUDAT"));
        r.put("CPUDT", ZCODCMT.get("CPUDT"));
        r.put("BLART", ZCODCMT.get("BLART"));
        r.put("REFBN", ZCODCMT.get("REFBN"));
        r.put("VERSN", ZCODCMT.get("VERSN"));
        r.put("AWTYP", ZCODCMT.get("AWTYP"));
        r.put("AWORG", ZCODCMT.get("AWORG"));

        r.put("BUZEI", BUZEI.get("BUZEI"));
        r.put("PERIO", BUZEI.get("PERIO"));
        r.put("WOGBTR", BUZEI.get("WOGBTR"));
        r.put("OBJNR", BUZEI.get("OBJNR"));
        r.put("OBJ_TYPE", BUZEI.get("OBJ_TYPE"));
        r.put("TYPE_NR", BUZEI.get("TYPE_NR"));
        r.put("PRCTR", BUZEI.get("PRCTR"));
        r.put("GJAHR", BUZEI.get("GJAHR"));
        r.put("KSTAR", BUZEI.get("KSTAR"));
        r.put("BEKNZ", BUZEI.get("BEKNZ"));
        r.put("BUKRS", BUZEI.get("BUKRS"));
        r.put("SGTXT", BUZEI.get("SGTXT"));
        r.put("FKBER", BUZEI.get("FKBER"));
        return r;
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        // process(e -> create a new file first, then append to it in batches)
        from(fileOrFtpIn).id((toimiala == null ? "" : toimiala) + "CoTositeIn")
            .to("direct:unmarshal-xml")
            .to("direct:process-co-tosite")
            .aggregate(new AggregateLinesWithoutStacking()).constant(true).completionFromBatchConsumer()
            // SAPSISLASKENTA ?
            .setHeader("CamelFileName", constant("SAPSISAINENLASKENTA.csv"))
            .setProperty("outDir", constant(toimiala))
            .to("direct:co-tosite-csv-out");
    }

    @Override
    public void buildSupportingRoutes() {
        // read from xml and process to a map by year and month, then in aggregation phase filter out
        from("direct:process-co-tosite")
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
                List<LinkedHashMap<String, Object>> receipts = allZcodcmts.stream().flatMap(zcodcmtMain -> {
                    Object receiptSegs = zcodcmtMain.get("BUZEI");
                    if (receiptSegs instanceof List receiptValList) {
                        List<Map<String, Object>> receiptVals = receiptValList;
                        return receiptVals.stream().map(v -> extractValues(zcodcmtMain, v));
                    } else {
                        Map<String, Object> v = (LinkedHashMap<String, Object>) receiptSegs;
                        return Stream.of(extractValues(zcodcmtMain, v));
                    }
                }).toList();

                e.getMessage().setBody(receipts);
            }).id("ProcessCoTositeOut");

        from("direct:co-tosite-csv-out").routeId("coTositeAzureOut")
            .marshal(coTositeCsvDataFormat)
            .to("direct:any-file-out");
    }

    @Override
    public String getFilePrefix() {
        return ".*CO_TOSITE_";
    }

    @Override
    public String getFtpDir() {
        return "201"; // TODO: check: /201/COS_OUT_...xml
    }
}

