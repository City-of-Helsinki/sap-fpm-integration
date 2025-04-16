package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class CoToteumatRouteBuilder extends RouteBuilder implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    public String ftpCoToteumatIn(String toimiala) {
        return buildFtpCoToteumatIn(toimiala, getFtpDir(), getFilePrefix());
    }

    @Override
    public void configure() throws Exception {
        if (mainConfig.palkeFTPCoToteumatEnabled()) {
            buildMainRoute(false, ftpCoToteumatIn("palke"), "palke");
        }

        if (mainConfig.localCoToteumatEnabled()) {
            buildMainRoute(true, "file:in?" + buildLocalToteumatIn("palke", getFilePrefix()), "palke");
        }

        if (mainConfig.localOrFTPCoToteumatEnabled()) {
           buildSupportingRoutes();
        }

    }

}

