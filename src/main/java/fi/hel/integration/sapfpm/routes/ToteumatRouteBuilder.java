package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.tositecommon.TositeRouteCommon;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class ToteumatRouteBuilder extends TositeRouteCommon implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    @Inject
    DefaultErrorHandlerBuilder defaultErrorHandlerBuilder;

    abstract public String[] createCsvHeader();
    public CsvDataFormat createCsvDataFormat() {
        return new CsvDataFormat().setQuoteDisabled(false).setDelimiter(';').setHeader(createCsvHeader());
    }

    public String ftpToteumatIn(String toimiala) {
        return buildFtpToteumatIn(toimiala, getFtpDir(toimiala), getFilePrefix());
    }

    @Override
    public void configure() throws Exception {
        errorHandler(defaultErrorHandlerBuilder.buildDefaultErrorHandler(this, log));

        if (mainConfig.palkeFTPToteumatEnabled()) {
            buildMainRoute(ftpToteumatIn("palke"), "palke");
        }

        if (mainConfig.kaskoFTPToteumatEnabled()) {
            buildMainRoute(ftpToteumatIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeFTPToteumatEnabled()) {
            buildMainRoute(ftpToteumatIn("sotepe"), "sotepe");
        }

        if (mainConfig.localToteumatEnabled()) {
            buildMainRoute("file:in?" + buildLocalToteumatIn("palke", getFilePrefix()), "palke");
        }

        if (mainConfig.localOrFTPToteumatEnabled()) {
           buildSupportingRoutes();
        }

    }

}

