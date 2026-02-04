package fi.hel.integration.sapfpm.routes.sapactual;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.routes.DefaultErrorHandlerBuilder;
import fi.hel.integration.sapfpm.routes.FtpOrFileRouteBuilder;
import fi.hel.integration.sapfpm.tositecommon.TositeRouteCommon;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildLocalS4ToteumatIn;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildS4SFtpToteumatIn;

@ApplicationScoped
public class S4FITositeInRouteBuilder extends TositeRouteCommon implements FtpOrFileRouteBuilder {
    @ConfigProperty(name = "tosite-db.page-limit", defaultValue = "1000")
    int DB_PAGE_LIMIT;

    @Inject
    IsConfigEnabled mainConfig;

    @Inject
    DefaultErrorHandlerBuilder defaultErrorHandlerBuilder;

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
            "VAT_PERCENT", "HSL", "PPRCTR", "MATNR", "EBELP", "LAST_CHANGE_DATETIME", "AUGBL", "AWTYP",
            "PS_POSID" // new
        };
    }

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
        r.put("PS_PSPID", tositeRow.get("PS_PSPID") == null ? tositeRow.get("PROJK") : tositeRow.get("PS_PSPID")); // projekti
        r.put("PS_POSID", tositeRow.get("PS_POSID")); // projektirivi
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

        String fetchToteumatRouteUri = getFetchToteumatRouteUri(toimiala);

        buildFetchingRoutes(toimiala);

        String fetchS4YearsAndMonthsUri = "direct:fetch-latest-s4-changed-years-and-months-from-db";

        String initDbFetchParamsAndFileNameUri = "direct:init-s4-tosite-db-fetch-params";
        String sendFileToAzureUri = "direct:enrich-and-send-file-to-azure-" + toimiala;
        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-s4-Tosite-%s".formatted(toimiala);

        buildFileAppendingFromDbPageRoute(fetchToteumatRouteUri, "fetchS4TositeAllYearsAndMonthsAndWriteToAzure-%s".formatted(toimiala), toimiala,
                fetchS4YearsAndMonthsUri, initDbFetchParamsAndFileNameUri,
                marshalWithHeaderCsvURI, "direct:fetch-s4-years-and-months-count-from-db", appendFromS4DbRouteUri, sendFileToAzureUri);

        log.info("S4 Tosite DB page limit: " + DB_PAGE_LIMIT);

        buildFtpBatchingRoute(fileOrFtpIn, toimiala + "s4TositeIn", toimiala, "direct:init-s4-tositerivi-db", processFileRouteUri, fetchToteumatRouteUri);

        from("file:trigger/s4-tosite-write-" + toimiala + "?delete=true").to(fetchToteumatRouteUri);
    }

    //
    public void buildFetchingRoutes(String toimiala) {
        String marshalHeaderlessCsvURI = "direct:marshal-headerless-csv-s4-Tosite-%s".formatted(toimiala);
        from(marshalHeaderlessCsvURI).routeId("s4tositeHeaderlessCsv")
            .marshal(createCsvDataFormat().setSkipHeaderRecord(true));
        buildAppendDbToExistingFileRoute(appendFromS4DbRouteUri,"appendS4TositeFromDb-%s".formatted(toimiala), DB_PAGE_LIMIT,"direct:fetch-s4-tositerivit-from-db-by-year-and-month",  marshalHeaderlessCsvURI);

        String marshalWithHeaderCsvURI = "direct:marshal-with-header-csv-s4-Tosite-%s".formatted(toimiala);
        from(marshalWithHeaderCsvURI).marshal(createCsvDataFormat().setSkipHeaderRecord(false));

        String initDbFetchParamsAndFileNameUri = "direct:init-s4-tosite-db-fetch-params";
        buildDbFetchAndFileNameInitializer(initDbFetchParamsAndFileNameUri, "s4InitTositeDbFetchParams-%s".formatted(toimiala), "POPER", "SAPACTUAL");
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
        errorHandler(defaultErrorHandlerBuilder.buildDefaultErrorHandler(this, log));

        if (mainConfig.palkeS4SFTPToteumatEnabled()) {
            buildMainRoute(s4SftpToteumatIn("palke"), "palke");
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

    public static String getFetchToteumatRouteUri(String toimiala) {
        return "direct:fetch-s4-tositteet-from-db-and-write-to-azure-" + toimiala;
    }
}

