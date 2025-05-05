package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class PerustiedotRouteBuilder extends RouteBuilder implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    abstract public String[] createCsvHeader();

    public CsvDataFormat createCsvDataFormat() {
        return new CsvDataFormat().setDelimiter(';').setQuoteDisabled(true).setHeader(createCsvHeader());
    }

    public String ftpPerustiedotIn(String toimiala) {
        return buildFtpPerustiedotIn(toimiala, getFtpDir(), getFilePrefix());
    }

    @Override
    public void configure() throws Exception {
        if (mainConfig.palkeFTPPerustiedotEnabled()) {
            log.info("Starting palke ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("palke"), "palke");
        }
        // TODO: enable when kasko token added
/*
        if (mainConfig.kaskoFTPPerustiedotEnabled()) {
            log.info("Starting kasko ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("kasko"), "kasko");
        }*/

        if (mainConfig.sotepeFTPPerustiedotEnabled()) {
            log.info("Starting sotepe ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("sotepe"), "sotepe");
        }

        if (mainConfig.localPerustiedotEnabled()) {
            log.info("Starting local perustiedot");
            buildMainRoute("file:in?" + buildLocalPerustiedotIn("palke", getFilePrefix()), "palke");
        }

        if (mainConfig.localOrFTPPerustiedotEnabled()) {
           buildSupportingRoutes();
        }

    }


    public void buildFtpFileReadingRoute(String fromURI, String idPrefix, String toimiala, String processRouteURI, String outFinalFileName) {
        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        Set<String> processedFileNames = new HashSet<>();

        CsvDataFormat csvDataFormatWithoutHeader = createCsvDataFormat().setSkipHeaderRecord(true);
        CsvDataFormat csvDataFormatWithHeader = createCsvDataFormat().setSkipHeaderRecord(false);

        String marshalHeaderlessCsvRouteURI = "direct:marshal-headerless-csv-" + idPrefix + "-" + toimiala;

        from(marshalHeaderlessCsvRouteURI).id(idPrefix + toimiala + "-marshalHeaderlessCsv")
            .marshal(csvDataFormatWithoutHeader);

        from(fromURI).id(idPrefix + "-" + toimiala)
            .log("%s %s read ${headers.CamelFileName}".formatted(idPrefix, toimiala))
            .log("batch size: ${exchangeProperty.CamelBatchSize}, i: ${exchangeProperty.CamelBatchIndex}, done: ${exchangeProperty.CamelBatchComplete}")
            .choice()
            .when(simple("${exchangeProperty.CamelBatchIndex} == 0"))
                .process(e -> {
                    if (processedFileNames.isEmpty()) {
                        initialBatchSize.set(e.getProperty("CamelBatchSize", Integer.class));
                        log.info("set initial batch size to " + initialBatchSize.get());
                    }
                })
            .end()
                .to("direct:unmarshal-xml")
                .to(processRouteURI)
                .to(marshalHeaderlessCsvRouteURI)
                .setProperty("fileExist", constant("Override"))
                .setProperty("wipFileDir", constant("wip/" + toimiala))
                .setProperty("outDir", exchangeProperty("wipFileDir"))
                .process(e -> {
                    e.getMessage().setHeader(FileConstants.FILE_NAME, e.getMessage().getHeader(FileConstants.FILE_NAME, String.class).replaceFirst(".xml", ".csv"));
                })
                .to("direct:any-file-out")
                .setBody(constant(""))
                .process(e -> {
                    processedFileNames.add(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                    if (processedFileNames.size() == initialBatchSize.get()) {
                        e.setProperty("writeOut", true);
                        e.setProperty("processedFileNames", processedFileNames);
                    }
                })
                .choice().when(simple("${exchangeProperty.writeOut} == true"))
                .setProperty("fileExist", constant("Override"))
                .setProperty("outDir", constant(toimiala))
                .setBody(constant(""))
                .marshal(csvDataFormatWithHeader)
                .setHeader(FileConstants.FILE_NAME, constant(outFinalFileName))
                .setProperty("outDir", constant(toimiala))
                .to("direct:any-file-out")
                .setBody(exchangeProperty("processedFileNames"))
                .setProperty("fileExist", constant("Append"))
                .split(body())
                .to("direct:append-csv-to-main-csv")
                .end()
                .process(e -> {
                    e.setProperty("writeOut", false);
                    processedFileNames.clear();
                    initialBatchSize.set(-1);
                })
                .to("direct:enrich-and-send-file-to-azure-" + toimiala);

    }


}

