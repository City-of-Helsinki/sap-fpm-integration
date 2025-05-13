package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "kasko")
public interface KaskoConfig {
    @WithName("azure.directory")
    Optional<String> azureDirectory();

    @WithName("azure.sasToken")
    Optional<String> azureSasToken();

    @WithName("azure.accountName")
    Optional<String> azureAccountName();

    @WithName("azure.containerName")
    Optional<String> azureContainerName();

    @WithName("ftp.host")
    Optional<String> ftpHost();

    @WithName("ftp.passiveMode")
    Optional<String> ftpPassiveMode();

    @WithName("ftp.perustiedot.user")
    Optional<String> ftpUserPerustiedot();

    @WithName("ftp.perustiedot.password")
    Optional<String> ftpPasswordPerustiedot();

    @WithName("ftp.toteumat.user")
    Optional<String> ftpUserToteumat();

    @WithName("ftp.toteumat.password")
    Optional<String> ftpPasswordToteumat();
}
