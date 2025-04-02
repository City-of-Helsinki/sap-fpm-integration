package fi.hel.integration.sapfpm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "local-file")
public interface LocalFileConfig {
    @WithName("perustiedot")
    Optional<Boolean> perustiedot();

    @WithName("toteumat")
    Optional<Boolean> toteumat();

    @WithName("co_toteumat")
    Optional<Boolean> coToteumat();
}
