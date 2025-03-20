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

KNA1 = asiakkaat, tälle ei perustietoliittymää eikä tule sotepelle käyttöön   (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
LFA1 = toimittajat, tälle on perustietoliittymä mutta ei tule sopete käyttöön (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin*/

// ORD_OUT -> sisäisen tilauksen käsittelyyn SAPSISTILAUS
// PART_OUT -> SAPKUMPPANI
// WBS_OUT -> SAPPROJEKTI
// ID022_FI_TOSITE -> SAPACTUAL
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
            from("ftp://{{palke.ftp.user_ID138}}@{{palke.ftp.host}}?password={{palke.ftp.password_ID138}}&noop=true&download=false&ftpClient.dataTimeout=5000&fileName=NON_EXISTING_FILE.txt")
                .routeId("readKaskoFtp")
                .log("ftp ${headers.CamelFileName}");
        }
    }
}
