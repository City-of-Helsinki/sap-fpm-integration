package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.routes.ToteumatRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;


// BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.
@ApplicationScoped
public class FITositeInRouteBuilder extends ToteumatRouteBuilder {
    final static int DB_PAGE_LIMIT = 50000;

    public String[] createCsvHeader() {
        return new String[]{
                "BUKRS", "BELNR", "CO_BELNR", "GJAHR", "POPER", "BLART", "BLDAT", "BUDAT", "CPUDT", "TCODE", "XBLNR", "KUNNR", "LIFNR", "LIFNR_NAME1",
                "EBELN", "Attachment", "BUZEI", "CO_BUZEI", "RACCT", "RCNTR", "PRCTR", "RFAREA", "AUFNR", "PS_PSPID", "RASSC", "SEGMENT", "SGTXT", "DRCRK", "MWSKZ",
                "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL", "AWTYP"
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
        r.put("AWTYP", E1FIKPF.get("AWTYP")); // lisätty uutena 9.6

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

        r.put("HSL", E1FISEG.get("HSL") == null ? E1FISEG.get("DMBTR") : E1FISEG.get("HSL"));

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
        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-Tosite-%s".formatted(toimiala);
        from(marshalHeaderlessCsvURI).routeId("tositeHeaderlessCsv")
            .marshal(createCsvDataFormat().setSkipHeaderRecord(true));

        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-Tosite-%s".formatted(toimiala);
        from(marshalWithHeaderCsvURI).marshal(createCsvDataFormat().setSkipHeaderRecord(false));

        String initRouteUri = "direct:init-toteumat-route";
        from(initRouteUri).setHeader("toimiala", constant(toimiala)).to("direct:init-tositerivi-db");

        String processFileRouteUri = "direct:process-tosite-file";
        from(processFileRouteUri)
            .to("direct:unmarshal-xml")
            .to("direct:process-tosite-file-contents")
            .to("direct:insert-tosite-file-and-contents-into-db");

        from("file:trigger/tosite-write-" + toimiala + "?delete=true").to("direct:fetch-tositteet-from-db-and-write-to-azure-" + toimiala);

        String initDbFetchParamsAndFileNameUri = "direct:init-tosite-db-fetch-params";
        from(initDbFetchParamsAndFileNameUri)
            .process(e -> {
                LinkedHashMap<String, Object> row = e.getMessage().getBody(LinkedHashMap.class);
                String year = (String)row.get("GJAHR");
                String month = (String)row.get("POPER");
                e.getMessage().setHeader("GJAHR", year);
                e.getMessage().setHeader("POPER", month);
                String simpleMonth = month.replaceFirst("^0+", "");
                e.getMessage().setHeader(FileConstants.FILE_NAME, "SAPACTUAL_" + year + "_" + simpleMonth + ".csv");
            });

        String sendFileToAzureUri = "direct:enrich-and-send-file-to-azure-" + toimiala;
        String fetchToteumatRouteUri = "direct:fetch-tositteet-from-db-and-write-to-azure-" + toimiala;

        buildFileAppendingFromDbPageRoute(fetchToteumatRouteUri, toimiala,
            "direct:fetch-all-years-and-months-from-db", initDbFetchParamsAndFileNameUri,
            marshalWithHeaderCsvURI, "direct:fetch-years-and-months-count-from-db", DB_PAGE_LIMIT,
            "direct:fetch-tositerivit-from-db-by-year-and-month", marshalHeaderlessCsvURI, sendFileToAzureUri);

        buildFtpBatchingRoute(fileOrFtpIn, toimiala + "tositeIn", initRouteUri, processFileRouteUri, fetchToteumatRouteUri);
    }

    @Override
    public String getFilePrefix() {
        return ".*FI_TOSITE_";
    }

    @Override
    public String getFtpDir(String toimiala) {
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
}

