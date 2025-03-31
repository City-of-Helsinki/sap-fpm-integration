package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.PalkeConfig;
import org.apache.camel.builder.RouteBuilder;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;



/*Sapista FPM:lle:
BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.

PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin*/

// ORD_OUT -> sisäisen tilauksen käsittelyyn SAPSISTILAUS
// ID167/203   ORD_OUT* Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per sisäinen tilaus

// PART_OUT -> SAPKUMPPANI
// ID167/210   PART_OUT* Kumppanit (kumppanitulosyksiköt) kaikki kumppanit, yksi tiedosto per päivä

// WBS_OUT -> SAPPROJEKTI
// // ID167/204   WBS_OUT* Projektit ja projektin rakenneosat, uudet ja muuttuneet (myös esim. lukitustiedosto) yksi tiedosto per päivä tiedosto sisältää kaikki uudet ja muuttuneet projektit

// ID022_FI_TOSITE -> SAPACTUAL

// CO_OUT -> ID166_CO_TOSITE_ ->

// Toimintoalueell ei nähdä tarvetta, se ei ole käytössä Palkella (eikä Kaskolla) ja SOTEPE voi ylläpitää toistaiseksi käsin (jos hekään oikeasti käyttävät budjetoinnissa toimintoaluetta)
// Samoin en näe tarvetta pääkirjatililataukselle, sen voi viedä suoraan FPM yhtenä latauksena sillä muutoksia tulee harvakseltaan


@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    public static String buildInParamsWithExclude(String excludeRegexp) {
       System.out.println("exclude: " + excludeRegexp);
        return "includeExt=xml&exclude=" + excludeRegexp + "&noop=true&" +
                "sortBy=file:name&charset=ISO-8859-1";
    }

    public static String buildInParams(String filePrefix) {
        return buildInParamsWithExclude("RAW(^(?!" + filePrefix + "){1}.*.xml)");
    }

    // TODO: move to mainConfig
    public static String buildFtpParams(String filePrefix) {
        return buildInParamsWithExclude("RAW((?!" + filePrefix + "){1}.*.xml)") +
                "&passiveMode=true";
    }

    @Inject
    Logger log;

    @Override
    public void configure() throws Exception {

        log.info("Profile: {{smallrye.config.profile}}");

        //SOTEPE ID167 perustiedot
        //  Samoin en näe tarvetta pääkirjatililataukselle, sen voi viedä suoraan FPM yhtenä latauksena sillä muutoksia tulee harvakseltaan
/*ID167/200   GLMAST*   Pääkirjatilit, uudet ja muuttuneet, yksi tiedosto per pääkirjatili

ID167/202   PRC_OUT* Tulosyksiköt, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per tulosyksikkö
ID167/203   ORD_OUT* Sisäiset tilaukset, uudet ja muuttuneet (myös esim. lukitustieto) yksi tiedosto per sisäinen tilaus
ID167/204   WBS_OUT* Projektit ja projektin rakenneosat, uudet ja muuttuneet (myös esim. lukitustiedosto) yksi tiedosto per päivä tiedosto sisältää kaikki uudet ja muuttuneet projektit
ID167/210   PART_OUT* Kumppanit (kumppanitulosyksiköt) kaikki kumppanit, yksi tiedosto per päivä

Toimintoalueell ei nähdä tarvetta, se ei ole käytössä Palkella (eikä Kaskolla) ja SOTEPE voi ylläpitää toistaiseksi käsin (jos hekään oikeasti käyttävät budjetoinnissa toimintoaluetta)
ID167/213  H_FUNC_OUT*  Toimintoalueet, kaikki toimintoalueet, yksi tiedosto per päivä*/

        // SOTEPE ID022 tositteet

        // KASKO-ID015 tyhjä ???
        // KASKO-ID137 perustiedot
        // KASKO ID023 toteumatositteet

        // PALKE ID166 tyhjä ??? <-- toteumatositteet, palkelle myös CO toteutamatositteet
        // PALKE ID138 perustiedot
        // PALKE ID025 toteumatositteet
    }
}
