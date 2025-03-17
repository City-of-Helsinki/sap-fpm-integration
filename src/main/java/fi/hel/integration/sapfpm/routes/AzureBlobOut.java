package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.PalkeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.jboss.logging.Logger;


@ApplicationScoped
public class AzureBlobOut extends RouteBuilder {
    @Inject
    PalkeConfig palkeConfig;

    @Inject
    Logger log;

    @Override
    public void configure() throws Exception {
        String azureParams = "operation=uploadBlockBlob&credentialType=AZURE_SAS&sasToken=RAW(%s)".formatted(palkeConfig.azureSasToken());

        from("file:testaz").to("direct:upload-blob-to-azure");

        from("direct:upload-blob-to-azure").id("upload-blob-to-azure")
            .process(e -> {
                e.getIn().setHeader(BlobConstants.BLOB_NAME, "testblobname");
            })
            .to("azure-storage-blob://%s/%s?%s".formatted(palkeConfig.azureAccountName(), palkeConfig.azureContainerName(), azureParams))
                .log("uploaded");
    }
}

