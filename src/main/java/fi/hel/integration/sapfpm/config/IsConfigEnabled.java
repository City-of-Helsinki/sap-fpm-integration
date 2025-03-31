package fi.hel.integration.sapfpm.config;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class IsConfigEnabled {
    @Inject
    PalkeConfig palkeConfig;

    @Inject
    KaskoConfig kaskoConfig;

    @Inject
    SotepeConfig sotepeConfig;

    @Inject
    LocalFileConfig localFileConfig;

    public boolean palkeFTPToteumatEnabled() {
        return palkeConfig.ftpPasswordToteumat().isPresent();
    }

    public boolean palkeFTPPerustiedotEnabled() {
        return palkeConfig.ftpPasswordPerustiedot().isPresent();
    }

    public boolean palkeFTPCoToteumatEnabled() {
        return palkeConfig.ftpPasswordCoToteumat().isPresent();
    }

    public boolean sotepeFTPToteumatEnabled() {
        return sotepeConfig.ftpPasswordToteumat().isPresent();
    }

    public boolean sotepeFTPPerustiedotEnabled() {
        return sotepeConfig.ftpPasswordPerustiedot().isPresent();
    }

    public boolean kaskoFTPToteumatEnabled() {
        return kaskoConfig.ftpPasswordToteumat().isPresent();
    }

    public boolean kaskoFTPPerustiedotEnabled() {
        return kaskoConfig.ftpPasswordPerustiedot().isPresent();
    }

    public boolean localPerustiedotEnabled() {
        return localFileConfig.perustiedot().orElse(false);
    }

    public boolean localToteumatEnabled() {
        return localFileConfig.toteumat().orElse(false);
    }

    public boolean localCoToteumatEnabled() {
        return localFileConfig.coToteumat().orElse(false);
    }
}
