package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.routes.ToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

// BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.
@ApplicationScoped
public class FITositeInRouteBuilder extends ToteumatRouteBuilder {
    public String[] createCsvHeader() {
        return new String[]{
                "BUKRS", "BELNR", "CO_BELNR", "GJAHR", "POPER", "BLART", "BLDAT", "BUDAT", "CPUDT", "TCODE", "XBLNR", "KUNNR", "LIFNR", "LIFNR_NAME1",
                "EBELN", "Attachment", "BUZEI", "CO_BUZEI", "RACCT", "RCNTR", "PRCTR", "RFAREA", "AUFNR", "PS_PSPID", "RASSC", "SEGMENT", "SGTXT", "DRCRK", "MWSKZ",
                "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL"
        };
    }

    // Uusi tosite alkaa <E1FIKPF> jonka jälkeen tulee rivitiedot
    // Rivitieto <E1FISEG SEGMENT=""> eli tositteen liitetiedot
    // kantaan: tosite <- rivitiedot
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
    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        if (toimiala == null) {
            log.error("toimiala is null!");
            return;
        }

        CsvDataFormat csvDataFormatWithHeader = createCsvDataFormat().setSkipHeaderRecord(false);
        CsvDataFormat csvDataFormatWithoutHeader = createCsvDataFormat().setSkipHeaderRecord(true);

        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-Tosite-%s".formatted(toimiala);
        from(marshalHeaderlessCsvURI).routeId("tositeHeaderlessCsv")
            .marshal(csvDataFormatWithoutHeader);

        //   String csvHeader = tositeCsvDataFormat.getHeader().replace(',', tositeCsvDataFormat.getDelimiter());
        // receipt id -> year + month (file name)
        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        AtomicInteger processedFileAmount = new AtomicInteger(0);

        from(fileOrFtpIn).id(toimiala + "tositeIn")
            .setHeader("toimiala", constant(toimiala))
            .setProperty("originalBody", body())
            .to("direct:init-tositerivi-db")
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
            .to("direct:process-tosite-file-contents")
            .to("direct:insert-tosite-file-and-contents-into-db")
            .log("batch size: ${exchangeProperty.CamelBatchSize}, i: ${exchangeProperty.CamelBatchIndex}, done: ${exchangeProperty.CamelBatchComplete}")
            .process(e -> e.getMessage().setBody(""))
            .process(e -> {
                int newTotal = processedFileAmount.addAndGet(1);
                e.setProperty("processedFileAmount", newTotal);
                int initial = initialBatchSize.get();
                log.info("initial: " + initial + ", total: " + newTotal);
                if (newTotal >= initial) {
                    e.setProperty("writeOut", true);
                }
                e.setProperty("initialBatchSize", initial);
            })
            .choice().when(simple("${exchangeProperty.writeOut} == true"))
                .log("Done! Group and write unique csvs from db")
                .to("direct:fetch-tositteet-from-db-and-write-to-azure-" + toimiala)
                .process(e -> {
                    e.setProperty("writeOut", false);
                    processedFileAmount.set(0);
                    initialBatchSize.set(-1);
                })
            .end();

        from("file:trigger/tosite-write-" + toimiala + "?delete=true").to("direct:fetch-tositteet-from-db-and-write-to-azure-" + toimiala);

        from("direct:fetch-tositteet-from-db-and-write-to-azure-" + toimiala)
            .setHeader("toimiala", constant(toimiala))
            .log("Writing tosite db out to azure for ${headers.toimiala}")
            .to("direct:fetch-all-years-and-months-from-db")
            .split(body())
                .process(e -> {
                    LinkedHashMap<String, Object> row = e.getMessage().getBody(LinkedHashMap.class);
                    String year = (String)row.get("GJAHR");
                    String month = (String)row.get("POPER");
                    e.getMessage().setHeader("GJAHR", year);
                    e.getMessage().setHeader("POPER", month);
                    String simpleMonth = month.replaceFirst("^0+", "");
                    e.getMessage().setHeader(FileConstants.FILE_NAME, "SAPACTUAL_" + year + "_" + simpleMonth + ".csv");
                })
                .setProperty("fileExist", constant("Override"))
                .setProperty("outDir", constant(toimiala))
                .setBody(constant(""))
                .marshal(csvDataFormatWithHeader)
                .to("direct:any-file-out")
                .setProperty("fileExist", constant("Append"))
                // create file first by writing only the header into the file, then stream and append
                .to("direct:fetch-tositerivit-from-db-by-year-and-month")
                .split(body()).streaming()
                    .to(marshalHeaderlessCsvURI)
                    .to("direct:any-file-out")
                    .log("written tosite rivi")
                    .setBody(constant(""))
                .end()
                .log("appending done, enriching and sending to azure")
                .to("direct:enrich-and-send-file-to-azure-" + toimiala)
                .setBody(constant(""))
            .end();
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

        from("direct:process-tosite-file-contents")
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

                // receipt -> receipt metadata
                // E1FIKPF contains commonValues and E1FIKPF.E1FISET also some common stuff
                // E1FIKPF can contain multiple E1FISEG segments
                // E1FIKPF can maybe contain multiple E1FISET but usually only 1
                // E1FISEG can contain E1FISE2 or E1FINBU
                List<List<LinkedHashMap<String, Object>>> receipts = allE1fikpf.stream().map(e1Main -> {
                    Object receiptSegs = e1Main.get("E1FISEG");
                    if (receiptSegs instanceof List receiptValList) {
                        List<Map<String, Object>> receiptVals = receiptValList;
                        return receiptVals.stream().map(v -> extractValues(e1Main, v)).toList();
                    } else {
                        Map<String, Object> v = (LinkedHashMap<String, Object>) receiptSegs;
                        return List.of(extractValues(e1Main, v));
                    }
                }).toList();

                e.getMessage().setBody(receipts);
            }).id("ProcessTositeOut");
    }

    public String getReceiptIdWithoutTime(Map<String, Object> receipt) {
        return receipt.get("BUKRS") + "_" + receipt.get("BELNR");
    }

    public String getReceiptId(Map<String, Object> receipt) {
        return getReceiptIdWithoutTime(receipt) + "_" +
                receipt.get("GJAHR") + "_" + receipt.get("POPER");
    }

    public List<Map<String, Object>> filterUniqueRows(List<Map<String, Object>> rows) {
        // id -> filename -> List of rows
        Map<String, Map<String, List<Map<String, Object>>>> idToFileName = new HashMap<>();
        // or order by file name and keep track of what was added to the file
        rows.stream().forEach(row -> {
            String receiptId = getReceiptIdWithoutTime(row);
            String fileName = (String)row.get("fileName");
            Map<String, List<Map<String, Object>>> perFileName = idToFileName.get(receiptId);
            if (perFileName == null) {
                perFileName = new HashMap<>();
                List<Map<String, Object>> fileNameRows = new ArrayList<>();
                fileNameRows.add(row);
                perFileName.put(fileName, fileNameRows);
                idToFileName.put(receiptId, perFileName);
            } else {
                List<Map<String, Object>> fileNameRows = perFileName.computeIfAbsent(fileName, k -> new ArrayList<>());
                fileNameRows.add(row);
            }
        });

        var filtered = idToFileName.values().stream().flatMap(perFileName -> {
            Optional<String> latestFileNameOpt;
            if (perFileName.size() == 1) {
                latestFileNameOpt = perFileName.keySet().stream().findFirst();
            } else {
                latestFileNameOpt = perFileName.keySet().stream().max(Comparator.naturalOrder());
                log.info("the row is in files %s as a duplicate, selecting the latest file %s".formatted(perFileName.keySet(), latestFileNameOpt));
            }
            String latestFileName = latestFileNameOpt.get();
            return perFileName.get(latestFileName).stream();
        }).toList();

        log.info("filtered rows size: " + filtered.size());
        return filtered;
    }
}

