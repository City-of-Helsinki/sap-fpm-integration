package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// PART_OUT_ _> SAPKUMPPANI
// kaikki kumppanit, yksi tiedosto per päivä
// eli tässä ei tarvita kaikkien lukemista, viimeisin riittää
@ApplicationScoped
public class PartInRouteBuilder extends PerustiedotRouteBuilder {
    public String[] createCsvHeader() {
        return new String[] { "RCOMP", "NAME1" };
    }

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    // PART_OUT_167_SOTE
    // PALKE: PART_OUT_138_9500
    @Override
    public String getFilePrefix() {
        return "PART_OUT_";
    }

    @Override
    public String getFtpDir() {
        return "210";
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        buildFtpFileReadingRoute(fileOrFtpIn, "Part",
                toimiala, "direct:process-part", "SAPKUMPPANI.csv");
    }

    @Override
    public void buildSupportingRoutes() {
        from("direct:process-part").id("ProcessPartOut").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        );
    }
}