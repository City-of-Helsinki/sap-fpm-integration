package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.routes.ToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.hel.integration.sapfpm.IDOCParser.addToByYearAndMonthIfExistsOrCreate;
import static fi.hel.integration.sapfpm.IDOCParser.concatNewLinesToOld;

// BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.
// ID022_FI_TOSITE_OUT_ > ID022 SOTE
// ID025_FI_TOSITE_ -> ID025 Palke
// IDXXX_FI_TOSITE -> IDXXX Kasko
// YYYY_MM

// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
@ApplicationScoped
public class FITositeInRouteBuilderSteps extends ToteumatRouteBuilder {
    CsvDataFormat createTositeCsvDataFormat() {
        return new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[]{
                "BUKRS", "BELNR", "CO_BELNR", "GJAHR", "POPER", "BLART", "BLDAT", "BUDAT", "CPUDT", "TCODE", "XBLNR", "KUNNR", "LIFNR", "LIFNR_NAME1",
                "EBELN", "Attachment", "BUZEI", "CO_BUZEI", "RACCT", "RCNTR", "PRCTR", "RFAREA", "AUFNR", "PS_PSPID", "RASSC", "SEGMENT", "SGTXT", "DRCRK", "MWSKZ",
                "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL"
        });
    }
//
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

    Map<String, String> db = new HashMap<>();

    Map<String, String> processedReceiptIds = new HashMap<>();

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
     //   String csvHeader = tositeCsvDataFormat.getHeader().replace(',', tositeCsvDataFormat.getDelimiter());
        // receipt id -> year + month (file name)
        boolean initialStart = true;

        from(fileOrFtpIn).id(toimiala + "tositeIn")
            //if db is empty, clean dirs here: wip and out/)
           /* .onException(GenericFileOperationFailedException.class)
                .log("FTP read failed, retrying")
                .setProperty("errorThrown", constant(true))

                .maximumRedeliveries(10) //Exhausted after delivery attempt: 1 caught: org.apache.camel.component.file.GenericFileOperationFailedException: Cannot retrieve file:
            .end()*/
            .log("read ${headers.CamelFileName}")
            .setProperty("originalCamelFileName", header("CamelFileName"))
            .to("direct:unmarshal-xml")
            .to("direct:process-tosite")
            .process(e -> {
                e.getMessage().setHeader("CamelFileName", e.getMessage().getHeader("CamelFileName", String.class).replace(".xml", ".csv"));
            })
            // aggregate exchanges here?
            .setProperty("outDir", constant("wip/" + toimiala))
            .to("direct:tosite-csv-out")
            .log("batch size: ${exchangeProperty.CamelBatchSize}, i: ${exchangeProperty.CamelBatchIndex}, done: ${exchangeProperty.CamelBatchComplete}")
            .process(e -> e.getMessage().setBody(""))
                // aggregate all processed file names into list
            .aggregate((AggregationStrategy) (oldExchange, newExchange) -> {
                List<String> fileNames;
                if (newExchange.getException() != null) {
                    log.info("newEx had exc: " + newExchange.getException().getMessage());
                }
                if (oldExchange == null) {
                    fileNames = new ArrayList<>();
                } else {
                    fileNames = oldExchange.getMessage().getBody(List.class);
                }
                fileNames.add(newExchange.getMessage().getHeader("CamelFileName", String.class));
                newExchange.getMessage().setBody(fileNames);
                return newExchange;
            }).constant(true).completionFromBatchConsumer()
            .log("file name aggr done, files: ${body.size()}")
            .log("Done! Group and write unique csvs")
        // split each file name and process via pollEnrich
                .setProperty("keepReadingCsvs", constant(true))
                .process(e -> {
                    // all existing ids
                    e.setProperty("existingIds", new HashSet<String>());
                })
                .loopDoWhile(simple("${exchangeProperty.keepReadingCsvs}"))
                    .pollEnrich().simple("file:${exchangeProperty.outDir}?" +
                    "includeExt=csv&preSort=true&sortBy=file:name").aggregationStrategy((original, resource) -> {
                        if (resource == null) {
                            original.setProperty("keepReadingCsvs", false);
                            return original;
                        } else {
                            log.info("Enriched " + resource.getMessage().getHeader("CamelFileName"));
                        }
                        original.getIn().setBody(resource.getIn().getBody());
                        return original;
                    })
                    .to("direct:read-and-filter-csv")
                    .split(body()).process(e -> {
                        Map.Entry<String, List<ArrayList<String>>> body = e.getMessage().getBody(Map.Entry.class);
                        e.getMessage().setHeader("CamelFileName", "SAPACTUAL_" + body.getKey() + ".csv");
                        e.getMessage().setBody(body.getValue());
                    })
                    .marshal(createTositeCsvDataFormat().setSkipHeaderRecord(false))
                    .setProperty("fileExist", constant("Append"))
                    .setProperty("outDir", constant(toimiala))
                    .to("direct:any-file-out")
                .end()
            .end();
    }

    // TODO: is not unique per CSV LINE!!!! only per tosite
    public String getReceiptId(LinkedHashMap<String, Object> receipt) {
        return receipt.get("BUKRS") + "_" + receipt.get("BELNR") + "_" +
                receipt.get("GJAHR") + "_" + receipt.get("POPER");
    }

    public String getReceiptId(ArrayList<String> receiptCsvLine) {
        if (receiptCsvLine.size() <= 10) {
            log.info("csv line is under sized: " + String.join(";", receiptCsvLine));
            log.info("in file: ${headers.CamelFileName}");
            return String.join("_", receiptCsvLine); // use whole line as id
        }
        return String.join("_", receiptCsvLine); // use whole line as id
        /*return receiptCsvLine.get(0) + "_" +receiptCsvLine.get(1) + "_" +
                receiptCsvLine.get(3) + "_" + receiptCsvLine.get(4) + "_" +
                // TODO: CHECK IF CAN BE USED! XBLNR
                receiptCsvLine.get(10);*/
    }

    public String getYearAndMonth(ArrayList<String> receiptCsvLine) {
        if (receiptCsvLine.size() <= 4) {
            log.info("csv line is under sized 4: " + String.join(";", receiptCsvLine));
            return "";
        }
        return receiptCsvLine.get(4) + "_" + receiptCsvLine.get(3);
    }

    @Override
    public String getFilePrefix() {
        return ".*FI_TOSITE_";
    }

    @Override
    public String getFtpDir() {
        return ""; // no dir
    }

    @Override
    public void buildSupportingRoutes() {

        // 1 csv file -> file for each yyyy_mm
        // oldExchange body is a map, newExchange is a csv line
        from("direct:read-and-filter-csv")
            .unmarshal(createTositeCsvDataFormat().setSkipHeaderRecord(false)).split(body()).streaming()
                .aggregationStrategy((AggregationStrategy) (oldExchange, newExchange) -> {
                    Set<String> existingIds;
                    Map<String, List<ArrayList<String>>> aggregatedByYearAndMonth;
                    ArrayList<String> csvVals = newExchange.getMessage().getBody(ArrayList.class);
                    String id = getReceiptId(csvVals);
                    if (oldExchange == null) {
                        log.info("FIRST LINE: " + csvVals);
                        aggregatedByYearAndMonth = new HashMap<String, List<ArrayList<String>>>();
                        existingIds =  newExchange.getProperty("existingIds", Set.class);
                    } else {
                        aggregatedByYearAndMonth = oldExchange.getMessage().getBody(Map.class);
                        existingIds =  oldExchange.getProperty("existingIds", Set.class);
                        newExchange.setProperty("existingIds", existingIds);
                    }

                    if (!existingIds.contains(id)) {
                        existingIds.add(id);

                        String lineYearAndMonth = getYearAndMonth(csvVals);
                        List<ArrayList<String>> existingLines = aggregatedByYearAndMonth.get(lineYearAndMonth);
                        if (existingLines == null) {
                            existingLines = new ArrayList<>();
                            existingLines.add(csvVals);
                            aggregatedByYearAndMonth.put(lineYearAndMonth, existingLines);
                        } else {
                            existingLines.add(csvVals);
                        }
                    } else {
                        log.info("skipping already existing line from file " + newExchange.getMessage().getHeader("CamelFileName"));
                        //log.info(id);
                    }

                    newExchange.getMessage().setBody(aggregatedByYearAndMonth);

                    return newExchange;
                }).process(e -> {});

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

                e.getMessage().setBody(receipts);
            }).id("ProcessTositeOut");

        // writes each xml file into a corresponding csv file
        from("direct:tosite-csv-out").routeId("tositeAzureOut")
            .marshal(createTositeCsvDataFormat().setSkipHeaderRecord(true))
            .to("direct:any-file-out");
    }
}

