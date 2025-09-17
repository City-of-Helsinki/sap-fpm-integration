package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static fi.hel.integration.sapfpm.routes.DefaultErrorHandlerBuilder.buildDefaultErrorHandler;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.*;


public abstract class PerustiedotRouteBuilder extends RouteBuilder implements FtpOrFileRouteBuilder {
    @Inject
    IsConfigEnabled mainConfig;

    @ConfigProperty(name = "default-route-redelivery-delay", defaultValue = "10000")
    int DEFAULT_REDELIVERY_DELAY;

    abstract public String[] createCsvHeader();

    public CsvDataFormat createCsvDataFormat() {
        return new CsvDataFormat().setDelimiter(';').setQuoteDisabled(false).setHeader(createCsvHeader());
    }

    public String getSortBy() {
        return "file:name";
    }

    public String ftpPerustiedotIn(String toimiala) {
        return buildFtpPerustiedotIn(toimiala, getFtpDir(toimiala), getFilePrefix(), getSortBy());
    }

    public String s4SftpPerustiedotIn(String toimiala) {
        return buildS4SFtpPerustiedotIn(toimiala, getFtpDir(toimiala), getFilePrefix(), getSortBy());
    }

    @Override
    public void configure() throws Exception {
        errorHandler(buildDefaultErrorHandler(this, log));

        if (mainConfig.palkeFTPPerustiedotEnabled()) {
            log.info("Starting palke ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("palke"), "palke");
        }

        if (mainConfig.kaskoFTPPerustiedotEnabled()) {
            //log.info("Kasko ftp perustiedot disabled!");
            log.info("Starting kasko ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeFTPPerustiedotEnabled()) {
            //log.info("Sotepe ftp perustiedot disabled!");
            log.info("Starting sotepe ftp perustiedot");
            buildMainRoute(ftpPerustiedotIn("sotepe"), "sotepe");
        }

        if (mainConfig.palkeS4SFTPPerustiedotEnabled()) {
            buildMainRoute(s4SftpPerustiedotIn("palke"), "palke");
        }

        if (mainConfig.kaskoS4SFTPPerustiedotEnabled()) {
            buildMainRoute(s4SftpPerustiedotIn("kasko"), "kasko");
        }

        if (mainConfig.sotepeS4SFTPPerustiedotEnabled()) {
            buildMainRoute(s4SftpPerustiedotIn("sotepe"), "sotepe");
        }

        if (mainConfig.localPerustiedotEnabled()) {
            log.info("Starting local perustiedot");
            buildMainRoute("file:in/kasko?" + buildLocalPerustiedotIn("kasko", getFilePrefix(), getSortBy()), "kasko");
            buildMainRoute("file:in/sotepe?" + buildLocalPerustiedotIn("sotepe", getFilePrefix(), getSortBy()), "sotepe");
            buildMainRoute("file:in/palke?" + buildLocalPerustiedotIn("palke", getFilePrefix(), getSortBy()), "palke");
        }

        if (mainConfig.localOrFTPPerustiedotEnabled()) {
           buildSupportingRoutes();
        }
    }

    public void buildFtpFileReadingRoute(String fromURI, String idPrefix, String toimiala, String processRouteURI, String outFinalFileName) {
        // an exception thrown during file processing will create a new batch, so keep track of initial batch size
        // and send only after the whole batch has been processed
        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        Set<String> batchProcessedFileNames = new HashSet<>();
        Set<String> allProcessedFileNames = new HashSet<>();
        CsvDataFormat csvDataFormatWithoutHeader = createCsvDataFormat().setSkipHeaderRecord(true);
        CsvDataFormat csvDataFormatWithHeader = createCsvDataFormat().setSkipHeaderRecord(false);

        String marshalHeaderlessCsvRouteURI = "direct:marshal-headerless-csv-" + idPrefix + "-" + toimiala;

        from(marshalHeaderlessCsvRouteURI).id(idPrefix + toimiala + "-marshalHeaderlessCsv")
            .marshal(csvDataFormatWithoutHeader);

        from("direct:init-csv-file-" + idPrefix + "-" + toimiala)
            .log("Creating initial file " + toimiala + "/" + outFinalFileName)
            .setProperty("fileExist", constant("Override")) // ignore so header gets written only once in the first batch
            .setProperty("outDir", constant(toimiala))
            .setBody(constant(""))
            .marshal(csvDataFormatWithHeader)
            .setHeader(FileConstants.FILE_NAME, constant(outFinalFileName))
            .to("direct:any-file-out");

        from(fromURI).id(idPrefix + "-" + toimiala)
            .onException(Exception.class)
                .maximumRedeliveries(10).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .log("%s %s read ${headers.CamelFileName}, batch: ${exchangeProperty.CamelBatchIndex}/${exchangeProperty.CamelBatchSize}, complete: ${exchangeProperty.CamelBatchComplete}".formatted(idPrefix, toimiala))
            .choice()
                .when(simple("${exchangeProperty.CamelBatchIndex} == 0"))
                    .process(e -> {
                        if (batchProcessedFileNames.isEmpty()) {
                            initialBatchSize.set(e.getProperty("CamelBatchSize", Integer.class));
                            log.info(idPrefix + "-" + toimiala + ": set initial batch size to " + initialBatchSize.get());
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
                    batchProcessedFileNames.add(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                    allProcessedFileNames.add(e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                    e.setProperty("allProcessedFileNames", allProcessedFileNames);

                    if (batchProcessedFileNames.size() == initialBatchSize.get()) {
                        e.setProperty("writeOut", true);
                        log.info(idPrefix + "-" + toimiala + ", processed all " + batchProcessedFileNames.size() + ", writing out");
                    } else if (e.getProperty("writeOut", Boolean.class) != null && e.getProperty("writeOut", Boolean.class)) {
                        log.info(idPrefix + " " + toimiala  + " writeOut was true after  processing " +
                                        batchProcessedFileNames.size() + " / " + initialBatchSize.get() + ": " +
                                e.getMessage().getHeader(FileConstants.FILE_NAME, String.class));
                    }
                })
                .choice().when(simple("${exchangeProperty.writeOut} == true"))
                    .to("direct:append-from-wip-dir-to-main-file-" + idPrefix + "-" + toimiala);

        from("direct:append-from-wip-dir-to-main-file-" + idPrefix + "-" + toimiala)
            .id("append-wip-main-" + idPrefix + "-" + toimiala)
            .log(idPrefix + " " + toimiala + ", writing out")
            .to("direct:init-csv-file-" + idPrefix + "-" + toimiala)
            .setBody(exchangeProperty("allProcessedFileNames"))
            .log("Appending wip/ files to main csv: ${body}")
            .setProperty("fileExist", constant("Append"))
            .split(body())
                .log(idPrefix + " " + toimiala + " appending ${body} to main csv")
                .to("direct:append-csv-to-main-csv")
            .end()
            .to("direct:enrich-and-send-file-to-azure-" + toimiala)
            .process(e -> {
                e.setProperty("writeOut", false);
                batchProcessedFileNames.clear();
                initialBatchSize.set(-1);
            });
    }

    public void buildLatestFileReadingRoute(String fromURI, String idPrefix, String toimiala, String processRouteURI, String outFinalFileName) {
        String processFileUri = "direct:process-and-send-to-azure-" + idPrefix + "-" + toimiala,
                processFileRouteId = idPrefix + "-" + toimiala + "-process";

        from(fromURI).id(idPrefix + "-" + toimiala)
            .onException(Exception.class)
                .maximumRedeliveries(10).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .log("%s %s read ${headers.CamelFileName}, last modified ${headers.CamelFileLastModified}".formatted(idPrefix, toimiala))
            .process(e -> {
                String lastSentFile = e.getVariable("route:%s:lastSentFile".formatted(processFileRouteId), String.class);
                if (lastSentFile != null) {
                    String fileName = e.getMessage().getHeader(FileConstants.FILE_NAME, String.class);
                    if (lastSentFile.compareTo(fileName) > 0) {
                        log.info("Last sent file %s is newer than currently processed %s, not processing it".formatted(lastSentFile, fileName));
                        e.setProperty("lastSentFileWasNewer", true);
                    }
                }
            })
            .choice()
            .when(exchangeProperty("lastSentFileWasNewer").isNotEqualTo(true))
            .to(processFileUri);

        // csv with header as only 1 file is read and written instead of appending from multiple files into one
        String marshalUri = "direct:marshal-csv-" + idPrefix + "-" + toimiala;
        from(marshalUri).marshal(createCsvDataFormat().setSkipHeaderRecord(false));

        from(processFileUri)
            .id(processFileRouteId)
            .to("direct:unmarshal-xml")
            .to(processRouteURI)
            .to(marshalUri)
            .setHeader("readFileName", header(FileConstants.FILE_NAME))
            .setHeader(FileConstants.FILE_NAME, constant(outFinalFileName))
            .setProperty("outDir", constant(toimiala))
            .to("direct:any-file-out")
            .to("direct:enrich-and-send-file-to-azure-" + toimiala)
            .setVariable("route:lastSentFile", header("readFileName"));

    }

}

