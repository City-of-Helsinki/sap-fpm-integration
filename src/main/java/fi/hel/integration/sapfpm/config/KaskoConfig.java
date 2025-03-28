package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "kasko")
public interface KaskoConfig {
    @WithName("azure.sasToken")
    Optional<String> azureSasToken();

    @WithName("azure.accountName")
    Optional<String> azureAccountName();

    @WithName("azure.containerName")
    Optional<String> azureContainerName();

    @WithName("ftp.host")
    Optional<String> ftpHost();

    @WithName("ftp.user_perustiedot")
    Optional<String> ftpUserPerustiedot();

    @WithName("ftp.password_perustiedot")
    Optional<String> ftpPasswordPerustiedot();

    @WithName("ftp.user_toteumat")
    Optional<String> ftpUserToteumat();

    @WithName("ftp.password_toteumat")
    Optional<String> ftpPasswordToteumat();
}
