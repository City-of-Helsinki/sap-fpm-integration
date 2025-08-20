package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// PART_OUT_ _> SAPKUMPPANI
// Lukee viimeisimmän PART_OUT tiedoston FTP:ltä, prosessoi sen SAPKUMPPANI.csv tiedostoksi ja lähettää tiedoston Azuren Blob Storageen.
// Jos FTP:ltä luettu tiedosto on nimen perusteella aikaisempi kuin viimeksi lähetetty tiedosto, tiedostoa ei käsitellä, sillä lähetetty tiedosto sisälsi jo uudemmat tiedot
@ApplicationScoped
public class PartInRouteBuilder extends PerustiedotRouteBuilder {
    public String[] createCsvHeader() {
        return new String[] { "RCOMP", "NAME1" };
    }

    @Inject
    IsConfigEnabled mainConfig;

    public CsvDataFormat createCsvDataFormatWithHeader() {
        return new CsvDataFormat().setDelimiter(';').setQuoteDisabled(false).setHeader(createCsvHeader()).setSkipHeaderRecord(false);
    }

    @Override
    public String getSortBy() { return "reverse:file:name"; }

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

    @Override
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