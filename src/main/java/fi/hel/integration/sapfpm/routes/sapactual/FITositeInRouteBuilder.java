package fi.hel.integration.sapfpm.routes.sapactual;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParamsWithExclude;

// BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.
// ID022_FI_TOSITE_OUT_ > ID022 SOTE
// ID025_FI_TOSITE_ -> ID025 Palke
// IDXXX_FI_TOSITE -> IDXXX Kasko
// YYYY_MM

// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class FITositeInRouteBuilder extends RouteBuilder {
    CsvDataFormat tositeCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
        "BUKRS","BELNR","CO_BELNR","GJAHR","POPER","BLART","BLDAT","BUDAT","CPUDT","TCODE","XBLNR","KUNNR","LIFNR","LIFNR_NAME1",
            "EBELN","Attachment","BUZEI","CO_BUZEI","RACCT","RCNTR","PRCTR","RFAREA","AUFNR","PS_PSPID","RASSC","SEGMENT","SGTXT","DRCRK","MWSKZ",
            "VAT_PERCENT","HSL","PPRCTR","MATNR","EBELP","LAST_CHANGE_DATETIME","AUGBL"
    });

    // E1FIKPF shared vals, E1FIKPF.E1FISEG receipt vals
    public LinkedHashMap<String, Object> extractValues(Map<String, Object> E1FIKPF, Map<String, Object> E1FISEG) {
        LinkedHashMap<String, Object> r = new LinkedHashMap<>(); // order matters
        Map<String, Object> E1FINBU = (Map<String, Object>) E1FISEG.get("E1FINBU");
        Map<String, Object> E1FISE2 = (Map<String, Object>) E1FISEG.get("E1FISE2");
        if (E1FINBU == null) E1FINBU = Map.of();
        if (E1FISE2 == null) E1FISE2 = Map.of();
        r.put("BUKRS", E1FIKPF.get("BUKRS")); // yritys
        r.put("BELNR", E1FIKPF.get("BELNR")); // tositenumero
        r.put("CO_BELNR", E1FIKPF.get("CO_BELNR")); // not in S4
        r.put("GJAHR", E1FIKPF.get("GJAHR")); // tilikausi
        r.put("POPER", E1FIKPF.get("POPER") == null ? E1FIKPF.get("MONAT") : E1FIKPF.get("POPER")); // kirjauskausi
        r.put("BLART", E1FIKPF.get("BLART"));
        r.put("BLDAT", E1FIKPF.get("BLDAT"));
        r.put("BUDAT", E1FIKPF.get("BUDAT")); // kirjauspvm
        r.put("CPUDT", E1FIKPF.get("CPUDT")); // not in s4
        r.put("TCODE", E1FIKPF.get("TCODE"));
        r.put("XBLNR", E1FIKPF.get("XBLNR")); // viitetositenumero (maksuviite)

        // väärin speksissä
        r.put("KUNNR", E1FINBU.get("KUNNR")); // asiakasnumero
        r.put("LIFNR", E1FINBU.get("LIFNR")); // toimittajanumero
        // TODO: recheck, doesn't make sense that LIFNR is in FINBU and name not!
        r.put("LIFNR_NAME1", E1FIKPF.get("LIFNR_NAME1")); // not in s4

        r.put("EBELN", E1FISEG.get("EBELN"));

        r.put("Attachment", E1FIKPF.get("Attachment") == null ? E1FIKPF.get("RESERVE") : E1FIKPF.get("Attachment"));

        r.put("BUZEI", E1FISEG.get("BUZEI"));

        r.put("CO_BUZEI", E1FISEG.get("CO_BUZEI")); // not in s4
        r.put("RACCT", E1FISEG.get("RACCT") == null ? E1FISEG.get("HKONT") : E1FISEG.get("RACCT"));
        r.put("RCNTR", E1FISEG.get("RCNTR") == null ? E1FISEG.get("KOSTL") : E1FISEG.get("RCNTR"));
        r.put("PRCTR", E1FISEG.get("PRCTR")); // tulosyksikkö

        r.put("RFAREA", E1FISE2.get("RFAREA") == null ? (E1FISE2.get("FKBER") == null ? E1FISE2.get("FKBER_LONG") : E1FISE2.get("FKBER")) : E1FISE2.get("RFAREA"));
        r.put("AUFNR", E1FISEG.get("AUFNR")); // sisäinen tilaus
        r.put("PS_PSPID", E1FISEG.get("PS_PSPID") == null ? E1FISEG.get("PROJK") : E1FISEG.get("PS_PSPID"));
        r.put("RASSC", E1FISEG.get("RASSC") == null ? E1FISEG.get("VBUND") : E1FISEG.get("RASSC"));
        r.put("SEGMENT", E1FISEG.get("SEGMENT")); // not in s4

        r.put("SGTXT", E1FISEG.get("SGTXT"));

        r.put("DRCRK", E1FISEG.get("DRCRK") == null ? E1FISEG.get("SHKZG") : E1FISEG.get("DRCRK"));
        r.put("MWSKZ", E1FISEG.get("MWSKZ"));

        r.put("VAT_PERCENT", E1FISEG.get("VAT_PERCENT")); // not in s4

        r.put("HSL", E1FISEG.get("HSL") == null ? E1FISEG.get("WRBTR") : E1FISEG.get("HSL"));

        r.put("PPRCTR", E1FISEG.get("PPRCTR") == null ? E1FISEG.get("PPRCT") : E1FISEG.get("PPRCTR"));

        r.put("MATNR", E1FISEG.get("MATNR"));
        r.put("EBELP", E1FISEG.get("EBELP"));

        r.put("LAST_CHANGE_DATE_TIME", E1FIKPF.get("LAST_CHANGE_DATE_TIME")); // not in s4
        r.put("AUGBL", E1FISEG.get("AUGBL"));
        return r;
    }

    final static String IN_FILE_EXCLUDE = "RAW(^(?!.*FI_TOSITE_).+)";
    @Override
    public void configure() throws Exception {
        // process(e -> create a new file first, then append to it in batches)
        // // TODO: batch and check ids in batches ?
//                // TODO: filter by year and month? i.e. the file.filter(this::receiptNotProcessedEarlier).toList();
        from("file:in?" + buildInParamsWithExclude(IN_FILE_EXCLUDE)).id("tositeIn")
            .to("direct:unmarshal-and-process-tosite")
            .aggregate((AggregationStrategy) (oldExchange, newExchange) -> {
                Set<String> oldProcessedReceiptIds;
                Map<String, List<LinkedHashMap<String, Object>>> newByYearAndMonth = newExchange.getMessage().getBody(Map.class);
                if (oldExchange == null) {
                    // db.put(getReceiptId(receipt), yearAndMonth);
                    // TODO: set somewhere?
                   // oldProcessedReceiptIds = newByYearAndMonth.values().stream().flatMap(yearMonth -> yearMonth.stream().map(this::getReceiptId)).collect(Collectors.toSet());
                    return newExchange;
                } else {
                    Map<String, List<LinkedHashMap<String, Object>>> oldByYearAndMonth = oldExchange.getMessage().getBody(Map.class);
                    oldProcessedReceiptIds = oldByYearAndMonth.values().stream().flatMap(yearMonth ->
                         yearMonth.stream().map(this::getReceiptId)
                    ).collect(Collectors.toSet());

                    newByYearAndMonth.forEach((yearMonthKey, newVals) -> {
                        List<LinkedHashMap<String, Object>> oldVals = oldByYearAndMonth.get(yearMonthKey);
                        if (oldVals == null) {
                            oldByYearAndMonth.put(yearMonthKey, newVals);
                        } else {
                            List<LinkedHashMap<String, Object>> filteredNewVals = newVals.stream().filter(newV -> {
                                boolean alreadyExists = oldProcessedReceiptIds.contains(getReceiptId(newV));
                                if (alreadyExists) {
                                    log.info("Receipt %s from %s already exists, ignoring".formatted(getReceiptId(newV), newExchange.getMessage().getHeader("CamelFileName", String.class)));
                                }
                                return !alreadyExists;
                            }).toList();
                            oldByYearAndMonth.put(yearMonthKey, concatNewLinesToOld(oldVals, filteredNewVals));
                        }
                    });

                    return oldExchange;
                }
            }).constant(true).completionFromBatchConsumer()
            .split(body()).process(e -> {
                // Map.entry -> each out YYYY_MM file out
                Map.Entry<String, List<Map<String, Object>>> yearAndMonthAndLines = e.getMessage().getBody(Map.Entry.class);
                String yearAndMonth = yearAndMonthAndLines.getKey();
                String year = yearAndMonth.substring(0, 4);
                String month = yearAndMonth.substring(4, 6);
                if (month.startsWith("0")) month = month.substring(1);

                e.getMessage().setHeader("CamelFileName", "SAPACTUAL" + "_" + year + "_" + month + ".csv");
                e.getMessage().setBody(yearAndMonthAndLines.getValue());
            })
            .log("Writing to Azure ${headers.CamelFileName}")
            .to("direct:tosite-csv-out");

        from("direct:unmarshal-and-process-tosite")
            .unmarshal().jacksonXml()
            .to("direct:process-tosite");

        // read from xml and process to a map by year and month, then in aggregation phase filter out
        from("direct:process-tosite")
            .process(e -> {
                // get each E1FIKPF, from them each E1FISEG and map those
                Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
                Map<String, Object> IDOC = xmlRoot.get("IDOC");
                Object valuesObj = IDOC.get("E1FIKPF");

                List<Map<String, Object>> allE1fikpf;

                if (valuesObj instanceof List valList) {
                    List<Map<String, Object>> vals = valList;
                    allE1fikpf = vals;
                } else {
                    Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
                    allE1fikpf = List.of(v);
                }
                // list of linkedhashmaps where each map -> csv line
                // E1FIKPF contains commonValues and E1FIKPF.E1FISET also some common stuff
                // E1FIKPF can contain multiple E1FISEG segments
                // E1FIKPF can maybe contain multiple E1FISET but usually only 1
                // E1FISEG can contain E1FISE2 or E1FINBU
                List<LinkedHashMap<String, Object>> receipts = allE1fikpf.stream().flatMap(e1Main -> {
                    Object receiptSegs = e1Main.get("E1FISEG");
                    if (receiptSegs instanceof List receiptValList) {
                        List<Map<String, Object>> receiptVals = receiptValList;
                        return receiptVals.stream().map(v -> extractValues(e1Main, v));
                    } else {
                        Map<String, Object> v = (LinkedHashMap<String, Object>) receiptSegs;
                        return Stream.of(extractValues(e1Main, v));
                    }
                }).toList();

                Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth = new HashMap<>();
                // take each line and map to GJAHR + POPER (MONAT)
                receipts.forEach(receipt -> {
                    String year = (String) receipt.get("GJAHR");
                    if (year.length() < 2) year = "0" + year;
                    String month = (String) receipt.get("POPER");
                    if (month.length() < 2) month = "0" + month;
                    String yearAndMonthKey = year + month;
                    addToByYearAndMonthIfExistsOrCreate(byYearAndMonth, yearAndMonthKey, List.of(receipt));
                });

                e.getMessage().setBody(byYearAndMonth);
            }).id("ProcessTositeOut");

        from("direct:tosite-csv-out").routeId("tositeAzureOut")
            .marshal(tositeCsvDataFormat)
            .to("direct:any-file-out");
    }

    public String getReceiptId(LinkedHashMap<String, Object> receipt) {
        return receipt.get("BUKRS") + "_" + receipt.get("BELNR") + "_" +
                receipt.get("GJAHR") + "_" + receipt.get("POPER");
    }

    // receipt id -> year + month (file name)
    Map<String, String> db = new HashMap<>();

    public boolean receiptNotProcessedEarlier(LinkedHashMap<String, Object> receipt) {
        String receiptId = getReceiptId(receipt);
        boolean wasProcessedEarlier = db.containsKey(receiptId);
        if (wasProcessedEarlier) {
            log.info("Receipt %s was processed earlier, excluding it".formatted(receiptId));
        }
        return !wasProcessedEarlier;
    }
}

