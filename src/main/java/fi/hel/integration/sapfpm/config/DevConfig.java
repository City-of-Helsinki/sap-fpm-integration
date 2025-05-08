package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "dev")
public interface DevConfig {
    @WithName("ftp-upload.host")
    Optional<String> ftpUploadHost();

    @WithName("ftp-upload.user")
    Optional<String> ftpUploadUser();

    @WithName("ftp-upload.password")
    Optional<String> ftpUploadPassword();

}
