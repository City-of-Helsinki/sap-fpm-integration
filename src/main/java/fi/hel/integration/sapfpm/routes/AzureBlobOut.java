package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.SentrySender;
import fi.hel.integration.sapfpm.config.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.component.file.FileConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@ApplicationScoped
public class AzureBlobOut extends RouteBuilder {
    @Inject
    PalkeConfig palkeConfig;

    @Inject
    SotepeConfig sotepeConfig;

    @Inject
    KaskoConfig kaskoConfig;

    @Inject
    Logger log;

    @Inject
    public SentrySender sentrySender;

    @ConfigProperty(name = "default-route-redelivery-delay", defaultValue = "10000")
    public int DEFAULT_REDELIVERY_DELAY;

    @ConfigProperty(name = "default-route-max-redeliveries", defaultValue = "120")
    public int DEFAULT_MAX_REDELIVERIES;

    public void createAzureBlobUploadingRoute(String toimiala) {
        // check for local here
        from("direct:upload-blob-to-azure-" + toimiala).id("upload-blob-to-azure-" + toimiala)
            .onException(Exception.class)
                .maximumRedeliveries(DEFAULT_MAX_REDELIVERIES).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .log("uploading ${headers.CamelFileName} to Azure {{%s.azure.directory}}".formatted(toimiala))
            .setHeader(BlobConstants.BLOB_NAME, simple("{{%s.azure.directory}}/${header.CamelFileName}".formatted(toimiala)))
            .toD("azure-storage-blob://{{%s.azure.accountName}}/{{%s.azure.containerName}}".formatted(toimiala, toimiala) +
                    "?sasToken=RAW({{%s.azure.sasToken}})".formatted(toimiala) +
                    "&credentialType=AZURE_SAS" +
                    "&operation=uploadBlockBlob")
            .log("uploaded ${headers.CamelFileName} to %s Azure {{%s.azure.directory}}".formatted(toimiala, toimiala));
    }

    public void createUploadLocalFileRoute(String toimiala, String uploadUri) {
        from("direct:enrich-and-send-file-to-azure-" + toimiala)
            .id("enrichAndSendToAzure-" + toimiala)
            .setHeader("toimiala", constant(toimiala)) // for sentry sending
            .log(toimiala + " enriching ${exchangeProperty.outDir}/${headers.CamelFileName}")
            .pollEnrich()
            .simple("file:${exchangeProperty.outDir}?fileName=RAW(${headers.CamelFileName})&autoCreate=false&noop=true&idempotent=false")
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
                    .log("SENDING ${headers.CamelFileName} to %s AZURE!".formatted(toimiala))
                    .setProperty("fileExist", constant("Override"))
                    .to(uploadUri)
                    .log("Uploading done! ${headers.toimiala} ${headers.CamelFileName} was sent to AZURE!")
                    .process(e -> sentrySender.send("%s %s".formatted(e.getMessage().getHeader("toimiala", String.class), e.getMessage().getHeader(FileConstants.FILE_NAME, String.class)), e))
                .end()
            .end();

    }

    public void createUploadLocalFileToAzureRoute(String toimiala) {
        createUploadLocalFileRoute(toimiala, "direct:upload-blob-to-azure-" + toimiala);
    }

    @Override
    public void configure() throws Exception {

        if (palkeConfig.azureAccountName().isPresent()) {
            log.info("palke azure uploading enabled");
            createAzureBlobUploadingRoute("palke");
            createUploadLocalFileToAzureRoute("palke");
        }

        if (sotepeConfig.azureAccountName().isPresent()) {
            log.info("sotepe azure uploading enabled");
            createAzureBlobUploadingRoute("sotepe");
            createUploadLocalFileToAzureRoute("sotepe");
        }

        if (kaskoConfig.azureAccountName().isPresent()) {
            log.info("kasko azure uploading enabled");
            createAzureBlobUploadingRoute("kasko");
            createUploadLocalFileToAzureRoute("kasko");
        }
    }
}

