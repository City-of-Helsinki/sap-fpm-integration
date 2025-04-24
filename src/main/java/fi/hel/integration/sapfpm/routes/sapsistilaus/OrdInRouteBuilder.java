package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// /203/ORD_OUT_*.xml _> SAPSISTILAUS.csv
// kaikki filut yhteen tiedostoon?
// Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto)
// tiedosto per sisäinen tilaus <- saattaa olla myös useita per tiedosto
// tuplat: FPM hoitaa
@ApplicationScoped
public class OrdInRouteBuilder extends PerustiedotRouteBuilder {
    public String[] createCsvHeader() {
        return new String[] { "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT" };
    }

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> ord = new LinkedHashMap<>(); // order matters
        ord.put("BUKRS", valuesLine.get("BUKRS")); // maksupiste
        ord.put("AUART", valuesLine.get("AUART")); // tilauslaji
        ord.put("AUFNR", valuesLine.get("AUFNR")); // tilausnumero
        ord.put("KTEXT", valuesLine.get("KTEXT")); // lyhytteksti
        ord.put("STTXT", valuesLine.get("STTXT")); // tilakoodit
        return ord;
    }

    @Override
    public String getFilePrefix() {
        return "ORD_OUT_";
    }

    @Override
    public String getFtpDir() {
        return "203";
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        buildFtpFileReadingRoute(fileOrFtpIn, "Ord",
                toimiala, "direct:process-ord", "SAPSISTILAUS.csv");
    }

    public void buildSupportingRoutes() {
        from("direct:process-ord").id("ProcessOrd")
                .setBody(e -> extractValuesFromIDOC(e, "ZHKI_TARSISTILAUKSET", this::extractValues));
    }
}

