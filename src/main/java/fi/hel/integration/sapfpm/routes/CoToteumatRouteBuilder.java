package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.tositecommon.TositeRouteCommon;
import jakarta.inject.Inject;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class CoToteumatRouteBuilder extends TositeRouteCommon implements FtpOrFileRouteBuilder {
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
            buildMainRoute(ftpCoToteumatIn("palke"), "palke");
        }

        if (mainConfig.localCoToteumatEnabled()) {
            buildMainRoute( "file:in?" + buildLocalCoToteumatIn("palke", getFilePrefix()), "palke");
        }

        if (mainConfig.localOrFTPCoToteumatEnabled()) {
           buildSupportingRoutes();
        }

    }

}

