package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.DevConfig;
import fi.hel.integration.sapfpm.config.IsConfigEnabled;
import fi.hel.integration.sapfpm.config.PalkeConfig;
import fi.hel.integration.sapfpm.config.SotepeConfig;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class DevFakeAzureUpload extends AzureBlobOut {
    @Inject
    Logger log;

    @Inject
    DevConfig devConfig;

    @Inject
    IsConfigEnabled mainConfig;

    public void createUploadLocalFileToFakeFtpRoute(String toimiala) {
        createUploadLocalFileRoute(toimiala, "ftp://{{dev.ftp-upload.user}}@{{dev.ftp-upload.host}}/%s?password={{dev.ftp-upload.password}}".formatted(toimiala));
    }

    @Override
    public void configure() throws Exception {
        if (devConfig.ftpUploadHost().isPresent()) {
            if (mainConfig.kaskoFTPPerustiedotEnabled() || mainConfig.kaskoFTPToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("kasko");
            }
            if (mainConfig.sotepeFTPPerustiedotEnabled() || mainConfig.sotepeFTPToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("sotepe");
            }
            if (mainConfig.palkeFTPPerustiedotEnabled() || mainConfig.palkeFTPToteumatEnabled() || mainConfig.palkeFTPCoToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("palke");
            }
        }

        if (mainConfig.localCoToteumatEnabled() || mainConfig.localToteumatEnabled() || mainConfig.localPerustiedotEnabled()) {
            createUploadLocalFileRoute("palke", "direct:any-file-out");
        }
    }
}

