package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.config.PalkeConfig;
import fi.hel.integration.sapfpm.config.SotepeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.component.file.FileConstants;
import org.jboss.logging.Logger;


@ApplicationScoped
public class AzureBlobOut extends RouteBuilder {
    @Inject
    PalkeConfig palkeConfig;

    @Inject
    SotepeConfig sotepeConfig;

    @Inject
    IsConfigEnabled mainConfig;

    @Inject
    Logger log;

    public void createAzureBlobUploadingRoute(String toimiala) {
        // check for local here

        from("direct:upload-blob-to-azure-" + toimiala).id("upload-blob-to-azure-" + toimiala)
            .log("uploading ${header.CamelFileName} to Azure ${exchangeProperty.uploadFileDir}")
            .process(e -> {
                e.getMessage().setHeader(BlobConstants.BLOB_NAME,
                e.getProperty("uploadFileDir") + "/" +
                        e.getMessage().getHeader(FileConstants.FILE_NAME));
            })
            .toD("azure-storage-blob://{{%s.azure.accountName}}/{{%s.azure.containerName}}".formatted(toimiala, toimiala) +
                    "?sasToken=RAW({{%s.azure.sasToken}})".formatted(toimiala) +
                    "&credentialType=AZURE_SAS" +
                    "&operation=uploadBlockBlob")
            .log("uploaded ${header.CamelFileName} to %s Azure ${exchangeProperty.uploadFileDir}".formatted(toimiala));
    }

    public void createLocalFileToAzureUploadingRoute(String toimiala, boolean isLocal) {
        from("direct:enrich-and-send-file-to-azure-" + toimiala)
            .pollEnrich()
            .simple("file:${exchangeProperty.outDir}?fileName=RAW(${headers.CamelFileName})&autoCreate=false")
            .aggregationStrategy((oldExchange, readFileExchange) -> {
                if (readFileExchange == null) {
                    log.error("READ FILE IS NULL!");
                    oldExchange.getMessage().setBody(null);
                } else {
                    oldExchange.getMessage().setBody(readFileExchange.getMessage().getBody());
                }
                return oldExchange;
            })
            .choice()
                .when(body().isNull())
                    .log("Not sending empty file to Azure! ${headers.CamelFileName}")
                .otherwise()
                    .choice()
                    // TODO: enable for other services as well
                        .when(simple("${exchangeProperty.outDir} == 'palke' || ${exchangeProperty.outDir} == 'sotepe'"))
                        .setProperty("uploadFileDir", constant("SAP/TEST"))
                        .log("SENDING ${exchangeProperty.outDir}/${headers.CamelFileName} to %s AZURE!".formatted(toimiala))
                        .setProperty("fileExist", constant("Override"))
                        .to(isLocal ? "direct:any-file-out" : "direct:upload-blob-to-azure-" + toimiala)
                    .end()
                .log("Done!");
    }

    @Override
    public void configure() throws Exception {

        if (palkeConfig.azureAccountName().isPresent()) {
            log.info("palke azure uploading enabled");
            createAzureBlobUploadingRoute("palke");
            createLocalFileToAzureUploadingRoute("palke", false);
        }

        if (sotepeConfig.azureAccountName().isPresent()) {
            log.info("sotepe azure uploading enabled");
            createAzureBlobUploadingRoute("sotepe");
            createLocalFileToAzureUploadingRoute("sotepe", false);
        }

        // TODO: replace with dev
        if (mainConfig.localCoToteumatEnabled() || mainConfig.localToteumatEnabled() || mainConfig.localPerustiedotEnabled()) {
            createLocalFileToAzureUploadingRoute("palke", true);
        }

        from("direct:any-file-out").id("AnyFileOut")
            .choice()
                .when(simple("${exchangeProperty.outDir} == null"))
                    .setProperty("outDir", constant("out"))
            .end()
            .choice()
                .when(simple("${exchangeProperty.fileExist} == null"))
                .setProperty("fileExist", constant("Override"))
            .end()
            .onException(Exception.class)
                .maximumRedeliveries(10).redeliveryDelay(1000)
                .log("Failed to write the file to Azure: ${exchangeProperty.CamelExceptionCaught}")
            .end()
            .toD("file:${exchangeProperty.outDir}?fileExist=${exchangeProperty.fileExist}")
                .choice().when(simple("${exchangeProperty.processedFiles} != null"))
                    .log("Processed ${exchangeProperty.processedFiles.size()} files: ")
                    .log("${exchangeProperty.processedFiles}")
                .end()
                .choice()
                    .when(simple("${exchangeProperty.fileExist} == 'Append'"))
                    //.log("Appended ${exchangeProperty.originalFileName} to ${exchangeProperty.outDir}/${headers.CamelFileName}")
                .otherwise()
                    .log("Written ${exchangeProperty.outDir}/${headers.CamelFileName}");
    }
}

