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

    @WithName("s4_sftp.host")
    Optional<String> s4SftpHost();

    @WithName("s4_sftp.passiveMode")
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

    @WithName("s4_sftp.toteumat.user")
    Optional<String> s4SftpUserToteumat();

    @WithName("s4_sftp.toteumat.password")
    Optional<String> s4SftpPasswordToteumat();

    @WithName("s4_sftp.perustiedot.user")
    Optional<String> s4SftpUserPerustiedot();

    @WithName("s4_sftp.perustiedot.password")
    Optional<String> s4SftpPasswordPerustiedot();
}
