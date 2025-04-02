package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildFtpPerustiedotIn;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;


public abstract class PerustiedotRouteBuilder extends RouteBuilder implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    public String ftpPerustiedotIn(String toimiala) {
        return buildFtpPerustiedotIn(toimiala, getFtpDir(), getFilePrefix());
    }

    @Override
    public void configure() throws Exception {
        if (mainConfig.palkeFTPPerustiedotEnabled()) {
            buildMainRoute(ftpPerustiedotIn("palke"), "palke");
        }

        if (mainConfig.kaskoFTPPerustiedotEnabled()) {
            buildMainRoute(ftpPerustiedotIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeFTPPerustiedotEnabled()) {
            buildMainRoute(ftpPerustiedotIn("sotepe"), "sotepe");
        }

        if (mainConfig.localPerustiedotEnabled()) {
            buildMainRoute("file:in?" + buildInParams(getFilePrefix()), null);
        }

        if (mainConfig.localOrFTPPerustiedotEnabled()) {
           buildSupportingRoutes();
        }

    }

}

