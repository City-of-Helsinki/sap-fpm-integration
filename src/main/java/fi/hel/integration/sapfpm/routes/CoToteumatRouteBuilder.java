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
            buildMainRoute(ftpCoToteumatIn("palke"), "palke");
        }

        if (mainConfig.localCoToteumatEnabled()) {
            buildMainRoute("file:in?" + buildInParams(getFilePrefix()), null);
        }

        if (mainConfig.localOrFTPCoToteumatEnabled()) {
           buildSupportingRoutes();
        }

    }

}

