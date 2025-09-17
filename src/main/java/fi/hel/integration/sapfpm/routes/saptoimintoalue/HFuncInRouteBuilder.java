package fi.hel.integration.sapfpm.routes.saptoimintoalue;

import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fi.hel.integration.sapfpm.IDOCParser.*;

// H_FUNC_OUT _> SAPTOIMINTOALUE
// Lukee viimeisimmän H_FUNC_OUT tiedoston FTP:ltä, prosessoi sen SAPTOIMINTOALUE.csv tiedostoksi ja lähettää tiedoston Azuren Blob Storageen.
// Jos FTP:ltä luettu tiedosto on nimen perusteella aikaisempi kuin viimeksi lähetetty tiedosto, tiedostoa ei käsitellä, sillä lähetetty tiedosto sisälsi jo uudemmat tiedot
@ApplicationScoped
public class HFuncInRouteBuilder extends PerustiedotRouteBuilder {

    public String[] createCsvHeader() {
        return new String[] { "Nimi", "Hierarkiataso", "Joukko", "Ylahierarkia", "Toiminto-alue", "Kuvaus", "alku", "Loppu" };
    }

    @Override
    public String getSortBy() { return "reverse:file:name"; }

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> f = new LinkedHashMap<>(); // order matters
        f.put("Nimi", valuesLine.get("Nimi"));
        f.put("Hierarkiataso", valuesLine.get("Hierarkiataso"));
        f.put("Joukko", valuesLine.get("Joukko"));
        f.put("Ylahierarkia", valuesLine.get("Ylahierarkia"));
        f.put("Toiminto-alue", valuesLine.get("Toiminto-alue"));
        f.put("Kuvaus", valuesLine.get("Kuvaus"));
        f.put("alku", valuesLine.get("alku"));
        f.put("Loppu", valuesLine.get("Loppu"));
        return f;
    }

    public String getFilePrefix() {
        return "H_FUNC_OUT_";
    }

    public String getFtpDir(String toimiala) {
        return "213";
    }

    public boolean includeToimintoalue(String toimiala, Object toimintoalueVal) {
        if (toimintoalueVal == null) return false;
        String toimintoalue = (String) toimintoalueVal;

        if (toimintoalue.startsWith("10")) {
            return true;
        }

        if ("palke".equals(toimiala)) {
            return toimintoalue.startsWith("95");
        } else if ("kasko".equals(toimiala)) {
            return toimintoalue.startsWith("14");
        } else if ("sotepe".equals(toimiala)) {
            return toimintoalue.startsWith("39") || toimintoalue.startsWith("70"); // 70 pelastuslaitos
        }

        return false;
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        String processRouteUri = "direct:process-hfunc-%s".formatted(toimiala);
        from(processRouteUri).id("ProcessHFuncOut%s".formatted(toimiala))
            .setBody(e ->
                extractValuesFromFunctionalArea(e).stream().filter(v -> includeToimintoalue(toimiala, v.get("Toiminto-alue"))).toList()
            );

        buildLatestFileReadingRoute(fileOrFtpIn, "HFunc", toimiala, processRouteUri, "SAPTOIMINTOALUE.csv");
    }

    public List<LinkedHashMap<String, Object>> extractValuesFromFunctionalArea(Exchange e) {
        Map<String, Map<String, Object>> funcAreaOut = e.getIn().getBody(Map.class);
        Map<String, Object> functionalArea = funcAreaOut.get("Functional_area");

        return extractValuesFromValueOrList(functionalArea.get("Line"), this::extractValues);
    }

    public void buildSupportingRoutes() {}
}