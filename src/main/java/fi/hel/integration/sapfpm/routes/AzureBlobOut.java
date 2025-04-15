package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.config.PalkeConfig;
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
    IsConfigEnabled mainConfig;

    @Inject
    Logger log;

    public void createAzureBlobUploadingRoute(String toimiala) {
        from("direct:upload-blob-to-azure-" + toimiala).id("upload-blob-to-azure")
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

    @Override
    public void configure() throws Exception {

        if (palkeConfig.azureAccountName().isPresent()) {
            log.info("palke azure uploading enabled");
            createAzureBlobUploadingRoute("palke");
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
                    .log("Appended ${exchangeProperty.originalFileName} to ${exchangeProperty.outDir}/${headers.CamelFileName}")
                .otherwise()
                    .log("Written ${exchangeProperty.outDir}/${headers.CamelFileName}");
    }
}

