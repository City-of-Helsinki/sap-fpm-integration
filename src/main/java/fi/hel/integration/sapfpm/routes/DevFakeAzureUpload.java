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

// on dev env, will upload the result files back to the ftp (ftp setup by robot tests)
@ApplicationScoped
public class DevFakeAzureUpload extends AzureBlobOut {
    @Inject
    Logger log;

    @Inject
    DevConfig devConfig;

    @Inject
    IsConfigEnabled mainConfig;

    public void createUploadLocalFileToFakeFtpRoute(String perustiedotOrToteumat, String toimiala) {
        createUploadLocalFileRoute(toimiala, "ftp://{{%s.ftp.%s.user}}@{{%s.ftp.host}}?password={{%s.ftp.%s.password}}&passiveMode=false&ftpClient.remoteVerificationEnabled=false".formatted(toimiala, perustiedotOrToteumat, toimiala, toimiala, perustiedotOrToteumat));
    }

    @Override
    public void configure() throws Exception {
        if (devConfig.ftpUploadEnabled().isPresent() && "true".equals(devConfig.ftpUploadEnabled().get())) {
            if (mainConfig.kaskoFTPPerustiedotEnabled()) {
                createUploadLocalFileToFakeFtpRoute("perustiedot", "kasko");
            }
            if (mainConfig.kaskoFTPToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("toteumat", "kasko");
            }
            if (mainConfig.sotepeFTPPerustiedotEnabled()) {
                createUploadLocalFileToFakeFtpRoute("perustiedot", "sotepe");
            }
            if (mainConfig.sotepeFTPToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("toteumat", "sotepe");
            }
            if (mainConfig.palkeFTPPerustiedotEnabled()) {
                createUploadLocalFileToFakeFtpRoute("perustiedot", "palke");
            }
            // use the same ftp account for toteumat & cototeumat to mimic azure account
            if (mainConfig.palkeFTPToteumatEnabled() || mainConfig.palkeFTPCoToteumatEnabled()) {
                createUploadLocalFileToFakeFtpRoute("toteumat", "palke");
            }
        }

        if (mainConfig.localCoToteumatEnabled() || mainConfig.localToteumatEnabled() || mainConfig.localPerustiedotEnabled()) {
            createUploadLocalFileRoute("palke", "direct:any-file-out");
            createUploadLocalFileRoute("sotepe", "direct:any-file-out");
            createUploadLocalFileRoute("kasko", "direct:any-file-out");
        }
    }
}

