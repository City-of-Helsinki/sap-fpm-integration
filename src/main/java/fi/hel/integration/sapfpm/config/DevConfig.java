package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "dev")
public interface DevConfig {
    @WithName("ftp-upload.enabled")
    Optional<String> ftpUploadEnabled();

}
