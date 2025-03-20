package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "palke")
public interface PalkeConfig {
    @WithName("azure.sasToken")
    String azureSasToken();

    @WithName("azure.accountName")
    String azureAccountName();

    @WithName("azure.containerName")
    String azureContainerName();

    @WithName("ftp.host")
    Optional<String> ftpHost();

    @WithName("ftp.user_ID138")
    Optional<String> ftpUserId138();

    @WithName("ftp.password_ID138")
    Optional<String> ftpPasswordId138();

    @WithName("ftp.user_ID166")
    Optional<String> ftpUserId166();

    @WithName("ftp.password_ID166")
    Optional<String> ftpPasswordId166();


}
