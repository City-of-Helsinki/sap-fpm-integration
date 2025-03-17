package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "palke")
public interface PalkeConfig {
    @WithName("azure.sasToken")
    String azureSasToken();

    @WithName("azure.accountName")
    String azureAccountName();

    @WithName("azure.containerName")
    String azureContainerName();
}
