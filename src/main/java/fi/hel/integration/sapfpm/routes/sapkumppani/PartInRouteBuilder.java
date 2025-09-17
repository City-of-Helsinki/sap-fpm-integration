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
        buildLatestFileReadingRoute(fileOrFtpIn, "Part", toimiala, "direct:process-part", "SAPKUMPPANI.csv");
    }

    public void buildSupportingRoutes() {
        from("direct:process-part").id("ProcessPartOut").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        );
    }
}