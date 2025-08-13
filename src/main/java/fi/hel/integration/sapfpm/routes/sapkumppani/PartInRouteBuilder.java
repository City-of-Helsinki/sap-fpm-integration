package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.DefaultErrorHandlerBuilder.buildDefaultErrorHandler;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildFtpPerustiedotIn;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildLocalPerustiedotIn;

// PART_OUT_ _> SAPKUMPPANI
// Lukee viimeisimmän PART_OUT tiedoston FTP:ltä, prosessoi sen SAPKUMPPANI.csv tiedostoksi ja lähettää tiedoston Azuren Blob Storageen.
// Jos FTP:ltä luettu tiedosto on nimen perusteella aikaisempi kuin viimeksi lähetetty tiedosto, tiedostoa ei käsitellä, sillä lähetetty tiedosto sisälsi jo uudemmat tiedot
@ApplicationScoped
public class PartInRouteBuilder extends RouteBuilder {
    public String[] createCsvHeader() {
        return new String[] { "RCOMP", "NAME1" };
    }

    @Inject
    IsConfigEnabled mainConfig;

    public CsvDataFormat createCsvDataFormatWithHeader() {
        return new CsvDataFormat().setDelimiter(';').setQuoteDisabled(false).setHeader(createCsvHeader()).setSkipHeaderRecord(false);
    }

    public String ftpPerustiedotIn(String toimiala) {
        return buildFtpPerustiedotIn(toimiala, getFtpDir(toimiala), getFilePrefix(), "reverse:file:name");
    }

    @Override
    public void configure() throws Exception {
        errorHandler(buildDefaultErrorHandler(this, log));

        if (mainConfig.palkeFTPPerustiedotEnabled()) {
            log.info("Starting palke ftp perustiedot Part");
            buildMainRoute(ftpPerustiedotIn("palke"), "palke");
        }

        if (mainConfig.kaskoFTPPerustiedotEnabled()) {
            //log.info("Kasko ftp perustiedot part disabled!");
            log.info("Starting kasko ftp perustiedot Part");
            buildMainRoute(ftpPerustiedotIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeFTPPerustiedotEnabled()) {
            //log.info("Sotepe ftp perustiedot part disabled!");
            log.info("Starting sotepe ftp perustiedot Part");
            buildMainRoute(ftpPerustiedotIn("sotepe"), "sotepe");
        }

        if (mainConfig.localPerustiedotEnabled()) {
            log.info("Starting local perustiedot Part");
            buildMainRoute("file:in/kasko?" + buildLocalPerustiedotIn("kasko", getFilePrefix(),"reverse:file:name"), "kasko");
            buildMainRoute("file:in/sotepe?" + buildLocalPerustiedotIn("sotepe", getFilePrefix(), "reverse:file:name"), "sotepe");
            buildMainRoute("file:in/palke?" + buildLocalPerustiedotIn("palke", getFilePrefix(), "reverse:file:name"), "palke");
        }

        if (mainConfig.localOrFTPPerustiedotEnabled()) {
            buildSupportingRoutes();
        }
    }

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    public String getFilePrefix() {
        return "PART_OUT_";
    }

    public String getFtpDir(String toimiala) {
        return "210";
    }

    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        String idPrefix = "Part";
        String processFileUri = "direct:process-part-and-send-to-azure-" + idPrefix + "-" + toimiala,
                processFileRouteId = idPrefix + "-" + toimiala + "-process";

        from(fileOrFtpIn).id(idPrefix + "-" + toimiala)
            .onException(Exception.class)
                .maximumRedeliveries(10).redeliveryDelay(10000)
            .end()
            .log("%s %s read ${headers.CamelFileName}, last modified ${headers.CamelFileLastModified}".formatted(idPrefix, toimiala))
            .process(e -> {
                String lastSentFile = e.getVariable("route:%s:lastSentFile".formatted(processFileRouteId), String.class);
                if (lastSentFile != null) {
                    String fileName = e.getMessage().getHeader(FileConstants.FILE_NAME, String.class);
                    if (lastSentFile.compareTo(fileName) > 0) {
                        log.info("Last sent file %s is newer than currently processed %s, not processing it".formatted(lastSentFile, fileName));
                        e.setProperty("lastSentFileWasNewer", true);
                    }
                }
            })
            .choice()
                .when(exchangeProperty("lastSentFileWasNewer").isNotEqualTo(true))
                    .to(processFileUri);

        from("direct:marshal-csv-" + idPrefix + "-" + toimiala).marshal(createCsvDataFormatWithHeader());

        from(processFileUri)
            .id(processFileRouteId)
            .to("direct:unmarshal-xml")
            .to("direct:process-part")
            .to("direct:marshal-csv-" + idPrefix + "-" + toimiala)
            .setHeader("readFileName", header(FileConstants.FILE_NAME))
            .setHeader(FileConstants.FILE_NAME, constant("SAPKUMPPANI.csv"))
            .setProperty("outDir", constant(toimiala))
            .to("direct:any-file-out")
            .to("direct:enrich-and-send-file-to-azure-" + toimiala)
            .setVariable("route:lastSentFile", header("readFileName"));
    }

    public void buildSupportingRoutes() {
        from("direct:process-part").id("ProcessPartOut").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        );
    }
}