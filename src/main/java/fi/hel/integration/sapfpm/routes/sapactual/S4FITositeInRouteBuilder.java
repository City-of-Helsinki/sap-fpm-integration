package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.routes.FtpOrFileRouteBuilder;
import fi.hel.integration.sapfpm.tositecommon.TositeRouteCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static fi.hel.integration.sapfpm.routes.DefaultErrorHandlerBuilder.buildDefaultErrorHandler;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildLocalS4ToteumatIn;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildS4SFtpToteumatIn;

@ApplicationScoped
public class S4FITositeInRouteBuilder extends TositeRouteCommon implements FtpOrFileRouteBuilder {
    @ConfigProperty(name = "tosite-db.page-limit", defaultValue = "1000")
    int DB_PAGE_LIMIT;

    @Inject
    IsConfigEnabled mainConfig;

    public CsvDataFormat createCsvDataFormat() {
        return new CsvDataFormat().setQuoteDisabled(false).setDelimiter(';').setHeader(createCsvHeader());
    }

    public String s4SftpToteumatIn(String toimiala) {
        return buildS4SFtpToteumatIn(toimiala, getFtpDir(toimiala), getFilePrefix());
    }

    public String[] createCsvHeader() {
        return new String[]{
            "BUKRS", "BELNR", "CO_BELNR", "GJAHR", "POPER", "BLART", "BLDAT", "BUDAT", "CPUDT", "TCODE", "XBLNR", "KUNNR", "LIFNR", "LIFNR_NAME1",
            "EBELN", "Attachment", "BUZEI", "CO_BUZEI", "RACCT", "RCNTR", "PRCTR", "RFAREA", "AUFNR", "PS_PSPID", "RASSC", "SEGMENT", "SGTXT", "DRCRK", "MWSKZ",
            "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL", "AWTYP"
        };
    }

    public static final String palkeFetchECCAndS4ToteumatRouteUri = "direct:fetch-s4-tositteet-from-db-and-write-to-azure-palke";

    public static final String appendFromS4DbRouteUri = "direct:fetch-and-append-s4-tositteet-from-db";

    // <ROWS><ROW>...</ROW></ROWS>
    public LinkedHashMap<String, Object> extractValues(Map<String, Object> tositeRow) {
        LinkedHashMap<String, Object> r = new LinkedHashMap<>(); // order matters
        r.put("BUKRS", tositeRow.get("BUKRS")); // yritys
        r.put("BELNR", tositeRow.get("BELNR")); // tositenumero
        r.put("CO_BELNR", tositeRow.get("CO_BELNR"));
        r.put("GJAHR", tositeRow.get("GJAHR")); // tilikausi
        r.put("POPER", tositeRow.get("POPER") == null ? tositeRow.get("MONAT") : tositeRow.get("POPER")); // kirjauskausi
        r.put("BLART", tositeRow.get("BLART"));
        r.put("BLDAT", tositeRow.get("BLDAT"));
        r.put("BUDAT", tositeRow.get("BUDAT")); // kirjauspvm
        r.put("CPUDT", tositeRow.get("CPUDT"));
        r.put("TCODE", tositeRow.get("TCODE"));
        r.put("XBLNR", tositeRow.get("XBLNR")); // viitetositenumero (maksuviite)
        r.put("AWTYP", tositeRow.get("AWTYP")); // lisätty uutena 9.6

        r.put("KUNNR", tositeRow.get("KUNNR")); // asiakasnumero
        r.put("LIFNR", tositeRow.get("LIFNR")); // toimittajanumero
        r.put("LIFNR_NAME1", tositeRow.get("LIFNR_NAME1"));

        r.put("EBELN", tositeRow.get("EBELN"));

        r.put("Attachment", tositeRow.get("Attachment") == null ? tositeRow.get("RESERVE") : tositeRow.get("Attachment"));

        r.put("BUZEI", tositeRow.get("BUZEI"));

        r.put("CO_BUZEI", tositeRow.get("CO_BUZEI"));
        r.put("RACCT", tositeRow.get("RACCT") == null ? tositeRow.get("HKONT") : tositeRow.get("RACCT"));
        r.put("RCNTR", tositeRow.get("RCNTR") == null ? tositeRow.get("KOSTL") : tositeRow.get("RCNTR"));
        r.put("PRCTR", tositeRow.get("PRCTR")); // tulosyksikkö

        r.put("RFAREA", tositeRow.get("RFAREA") == null ? (tositeRow.get("FKBER") == null ? tositeRow.get("FKBER_LONG") : tositeRow.get("FKBER")) : tositeRow.get("RFAREA"));
        r.put("AUFNR", tositeRow.get("AUFNR")); // sisäinen tilaus
        r.put("PS_PSPID", tositeRow.get("PS_PSPID") == null ? tositeRow.get("PROJK") : tositeRow.get("PS_PSPID")); // Projekti, tyhjä?
        r.put("RASSC", tositeRow.get("RASSC") == null ? tositeRow.get("VBUND") : tositeRow.get("RASSC"));
        r.put("SEGMENT", tositeRow.get("SEGMENT")); // not in s4

        r.put("SGTXT", tositeRow.get("SGTXT"));

        r.put("DRCRK", tositeRow.get("DRCRK") == null ? tositeRow.get("SHKZG") : tositeRow.get("DRCRK"));
        r.put("MWSKZ", tositeRow.get("MWSKZ"));

        r.put("VAT_PERCENT", tositeRow.get("VAT_PERCENT")); // not in s4

        r.put("HSL", tositeRow.get("HSL") == null ? tositeRow.get("DMBTR") : tositeRow.get("HSL"));

        r.put("PPRCTR", tositeRow.get("PPRCTR") == null ? tositeRow.get("PPRCT") : tositeRow.get("PPRCTR"));

        r.put("MATNR", tositeRow.get("MATNR")); // nimike, empty in s4
        r.put("EBELP", tositeRow.get("EBELP"));

        r.put("LAST_CHANGE_DATETIME", tositeRow.get("LAST_CHANGE_DATETIME"));
        r.put("AUGBL", tositeRow.get("AUGBL")); // kuittaustosite

        r.put("DOCLN", tositeRow.get("DOCLN")); // excluded from CSV, but used as part of ID / primary key
        return r;
    }
    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        if (toimiala == null) {
            log.error("s4 tosite toimiala is null!");
            return;
        }

        String processFileRouteUri = "direct:process-s4-tosite-file";
        from(processFileRouteUri)
            .to("direct:unmarshal-xml")
            .to("direct:process-s4-tosite-file-contents")
            .to("direct:insert-s4-tosite-file-and-contents-into-db");

        String initDbFetchParamsAndFileNameUri = "direct:init-s4-tosite-db-fetch-params";
        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-s4-Tosite-%s".formatted(toimiala);
        String sendFileToAzureUri = "direct:enrich-and-send-file-to-azure-" + toimiala;

        String fetchToteumatRouteUri = "direct:fetch-s4-tositteet-from-db-and-write-to-azure-" + toimiala;

        buildFetchingRoutes(toimiala);

        if ("palke".equals(toimiala)) {
            buildSS4AndECCFetchingRoute();
        } else {
            String fetchS4YearsAndMonthsUri = "direct:fetch-all-s4-years-and-months-from-db";

            buildFileAppendingFromDbPageRoute(fetchToteumatRouteUri, "fetchS4TositeAllYearsAndMonthsAndWriteToAzure-%s".formatted(toimiala), toimiala,
                    fetchS4YearsAndMonthsUri, initDbFetchParamsAndFileNameUri,
                    marshalWithHeaderCsvURI, "direct:fetch-s4-years-and-months-count-from-db", appendFromS4DbRouteUri, sendFileToAzureUri);
        }
        log.info("S4 Tosite DB page limit: " + DB_PAGE_LIMIT);

        String initDbUri = "direct:init-s4-toteumat-route";
        buildFtpBatchingRoute(fileOrFtpIn, toimiala + "s4TositeIn", toimiala, initDbUri, processFileRouteUri, fetchToteumatRouteUri);
    }

    //
    public void buildFetchingRoutes(String toimiala) {
        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-s4-Tosite-%s".formatted(toimiala);
        from(marshalHeaderlessCsvURI).routeId("s4tositeHeaderlessCsv")
            .marshal(createCsvDataFormat().setSkipHeaderRecord(true));

        buildAppendDbToExistingFileRoute(appendFromS4DbRouteUri,"appendS4TositeFromDb-%s".formatted(toimiala), DB_PAGE_LIMIT,"direct:fetch-s4-tositerivit-from-db-by-year-and-month",  marshalHeaderlessCsvURI);

        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-s4-Tosite-%s".formatted(toimiala);
        from(marshalWithHeaderCsvURI).marshal(createCsvDataFormat().setSkipHeaderRecord(false));

        String initDbUri = "direct:init-s4-toteumat-route";
        from(initDbUri)
            .choice().when(constant(mainConfig.palkeS4SFTPToteumatEnabled() || mainConfig.localOrFTPToteumatEnabled()))
                .to("direct:init-tositerivi-db")
            .end()
            .to("direct:init-s4-tositerivi-db");

        String initDbFetchParamsAndFileNameUri = "direct:init-s4-tosite-db-fetch-params";
        buildDbFetchAndFileNameInitializer(initDbFetchParamsAndFileNameUri, "s4InitTositeDbFetchParams-%s".formatted(toimiala), "POPER", "SAPACTUAL");
    }

    // fetches from both ECC and S4 and creates and sends SAPACTUAL_ files from them
    public void buildSS4AndECCFetchingRoute() {
        String toimiala = "palke";
        String fetchToteumatRouteUri = "direct:fetch-s4-tositteet-from-db-and-write-to-azure-" + toimiala;
        String appendFromECCDbRouteUri = "direct:fetch-and-append-ecc-tositteet-from-db";

        String initDbFetchParamsAndFileNameUri = "direct:init-s4-tosite-db-fetch-params";
        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-s4-Tosite-%s".formatted(toimiala);
        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-s4-Tosite-%s".formatted(toimiala);
        String sendFileToAzureUri = "direct:enrich-and-send-file-to-azure-" + toimiala;

        buildAppendDbToExistingFileRoute(appendFromECCDbRouteUri, "appendECCTositeFromDb-%s".formatted(toimiala), DB_PAGE_LIMIT, "direct:fetch-tositerivit-from-db-by-year-and-month", marshalHeaderlessCsvURI);

        String appendFromBothS4AndEccRouteUri = "direct:fetch-and-append-s4-and-ecc-tositteet-from-db";
        from(appendFromBothS4AndEccRouteUri)
            .errorHandler(noErrorHandler())
            .to(appendFromECCDbRouteUri)
            .to(appendFromS4DbRouteUri);

        String fetchS4YearsAndMonthsUri = "direct:fetch-all-s4-years-and-months-from-db";

        String fetchECCYearsAndMonthsUri = "direct:fetch-all-years-and-months-from-db";
        String fetchECCANDS4YearsAndMonthsUri = "direct:fetch-ecc-and-s4-years-and-months-from-db";
        from(fetchECCANDS4YearsAndMonthsUri)
            .to(fetchS4YearsAndMonthsUri)
            .setProperty("s4YearsAndMonths", body())
            .to(fetchECCYearsAndMonthsUri)
            .process(e -> {
                List<LinkedHashMap<String, Object>> eccYearsAndMonths = e.getMessage().getBody(List.class);
                List<LinkedHashMap<String, Object>> s4YearsAndMonths = e.getProperty("s4YearsAndMonths", List.class);
                List<LinkedHashMap<String, Object>> all = eccYearsAndMonths == null ? new ArrayList<>() : new ArrayList<>(eccYearsAndMonths);
                if (s4YearsAndMonths != null) {
                    s4YearsAndMonths.forEach(y -> {
                        Optional<LinkedHashMap<String, Object>> found = all.stream().filter(a ->
                                y.get("GJAHR").equals(a.get("GJAHR")) && y.get("POPER").equals(a.get("POPER"))).findFirst();
                        if (found.isEmpty()) {
                            all.add(y);
                        }
                    });
                }
                e.getMessage().setBody(all);
                e.removeProperty("s4YearsAndMonths");
            });

        buildFileAppendingFromDbPageRoute(fetchToteumatRouteUri, "fetchS4TositeAllYearsAndMonthsAndWriteToAzure-%s".formatted(toimiala), toimiala,
                fetchECCANDS4YearsAndMonthsUri, initDbFetchParamsAndFileNameUri,
                marshalWithHeaderCsvURI, "direct:fetch-s4-years-and-months-count-from-db", appendFromBothS4AndEccRouteUri, sendFileToAzureUri);
    }

    @Override
    public String getFilePrefix() {
        return ".*FI_TOSITE_";
    }

    @Override
    public String getFtpDir(String toimiala) {
        return "";
    }

    @Override
    public void buildSupportingRoutes() {

        from("direct:process-s4-tosite-file-contents")
            .process(e -> {
                Map<String, Object> ROWS = e.getIn().getBody(Map.class);
                Object valuesObj = ROWS.get("ROW");
                List<LinkedHashMap<String, Object>> receiptRows;

                if (valuesObj instanceof List valList) {
                    List<Map<String, Object>> vals = valList;
                    receiptRows = vals.stream().map(this::extractValues).toList();
                } else {
                    Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
                    receiptRows = List.of(extractValues(v));
                }

                e.getMessage().setBody(receiptRows);
            }).id("ProcessS4TositeOut");
    }

    @Override
    public void configure() throws Exception {
        errorHandler(buildDefaultErrorHandler(this, log));

        if (mainConfig.palkeS4SFTPToteumatEnabled()) {
            buildMainRoute(s4SftpToteumatIn("palke"), "palke");
        } else if (mainConfig.palkeFTPToteumatEnabled()) {
            // ECC needs to write both S4 and ECC files from db
            buildFetchingRoutes("palke");
            buildSS4AndECCFetchingRoute();
        }

        if (mainConfig.kaskoS4SFTPToteumatEnabled()) {
            buildMainRoute(s4SftpToteumatIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeS4SFTPToteumatEnabled()) {
            buildMainRoute(s4SftpToteumatIn("sotepe"), "sotepe");
        }

        if (mainConfig.localS4ToteumatEnabled()) {
            buildMainRoute("file:in/s4?" + buildLocalS4ToteumatIn("palke", getFilePrefix()), "palke");
        }

        if (mainConfig.localOrSFTPS4ToteumatEnabled()) {
            buildSupportingRoutes();
        }

    }
}

