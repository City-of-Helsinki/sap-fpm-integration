package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class CoToteumatRouteBuilder extends RouteBuilder implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    abstract public String[] createCsvHeader();
    public CsvDataFormat createCsvDataFormat() {
        return new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(createCsvHeader());
    }

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

