package fi.hel.integration.sapfpm.routes.sapsistilaus;

import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// /203/ORD_OUT_*.xml _> SAPSISTILAUS.csv
// kaikki filut yhteen tiedostoon?
// Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto)
// tiedosto per sisäinen tilaus <- saattaa olla myös useita per tiedosto
// tuplat: FPM hoitaa
@ApplicationScoped
public class OrdInRouteBuilder extends PerustiedotRouteBuilder {
    @Inject
    Logger log;

    public CsvDataFormat csvDataFormat() {
        return new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
                "BUKRS", "AUART", "AUFNR", "KTEXT", "STTXT"
        });
    }

    CsvDataFormat withHeader = csvDataFormat().setSkipHeaderRecord(false);
    CsvDataFormat withoutHeader = csvDataFormat().setSkipHeaderRecord(true);

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
        buildFtpFileReadingRoute(from(fileOrFtpIn).id("OrdIn" + (toimiala == null ? "" : toimiala)),
                toimiala, "direct:process-ord",
                withoutHeader,
                withHeader, "SAPSISTILAUS.csv");
    }

    public void buildSupportingRoutes() {
        from("direct:process-ord").id("ProcessOrd")
                .setBody(e -> extractValuesFromIDOC(e, "ZHKI_TARSISTILAUKSET", this::extractValues));
    }
}

