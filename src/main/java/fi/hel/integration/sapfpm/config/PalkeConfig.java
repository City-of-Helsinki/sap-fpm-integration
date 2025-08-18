package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "palke")
public interface PalkeConfig {
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

    @WithName("sftp.host")
    Optional<String> s4SftpHost();

    @WithName("sftp.passiveMode")
    Optional<String> s4SftpPassiveMode();

    @WithName("ftp.perustiedot.user")
    Optional<String> ftpUserPerustiedot();

    @WithName("ftp.perustiedot.password")
    Optional<String> ftpPasswordPerustiedot();

    @WithName("ftp.toteumat.user")
    Optional<String> ftpUserToteumat();

    @WithName("ftp.toteumat.password")
    Optional<String> ftpPasswordToteumat();

    @WithName("ftp.co_toteumat.user")
    Optional<String> ftpUserCoToteumat();

    @WithName("ftp.co_toteumat.password")
    Optional<String> ftpPasswordCoToteumat();

    @WithName("sftp.toteumat.user")
    Optional<String> s4SftpUserToteumat();

    @WithName("sftp.toteumat.password")
    Optional<String> s4SftpPasswordToteumat();
}
