package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.PalkeConfig;
import fi.hel.integration.sapfpm.model.ID022_FI_TOSITE.FIDCCP02;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


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

// CO_OUT ->

// Toimintoalueell ei nähdä tarvetta, se ei ole käytössä Palkella (eikä Kaskolla) ja SOTEPE voi ylläpitää toistaiseksi käsin (jos hekään oikeasti käyttävät budjetoinnissa toimintoaluetta)
// Samoin en näe tarvetta pääkirjatililataukselle, sen voi viedä suoraan FPM yhtenä latauksena sillä muutoksia tulee harvakseltaan


@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    @Inject
    Logger log;

    @Inject
    PalkeConfig palkeConfig;

    @Override
    public void configure() throws Exception {

        log.info("Profile: {{smallrye.config.profile}}");

        // palke ftp ID138
        if (palkeConfig.ftpHost().isPresent()) {
            log.info("starting sftp");
            // TODO: preSort by name and consume oldest first
            from("ftp://{{palke.ftp.user_ID138}}@{{palke.ftp.host}}?password={{palke.ftp.password_ID138}}&noop=true&download=false&ftpClient.dataTimeout=5000&passiveMode=true&includeExt=xml")
                .routeId("readKaskoFtp")
                .log("ftp ${headers.CamelFileName}");
        }
    }
}
