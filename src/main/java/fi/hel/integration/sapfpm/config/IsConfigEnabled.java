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
    public boolean palkeS4SFTPToteumatEnabled() { return palkeConfig.s4SftpPasswordToteumat().isPresent(); }

    public boolean palkeFTPPerustiedotEnabled() {
        return palkeConfig.ftpPasswordPerustiedot().isPresent();
    }
    public boolean palkeS4SFTPPerustiedotEnabled() { return palkeConfig.s4SftpPasswordPerustiedot().isPresent(); }

    public boolean palkeFTPCoToteumatEnabled() {
        return palkeConfig.ftpPasswordCoToteumat().isPresent();
    }

    public boolean sotepeFTPToteumatEnabled() { return sotepeConfig.ftpPasswordToteumat().isPresent(); }
    public boolean sotepeS4SFTPToteumatEnabled() { return sotepeConfig.s4SftpPasswordToteumat().isPresent(); }

    public boolean sotepeFTPPerustiedotEnabled() { return sotepeConfig.ftpPasswordPerustiedot().isPresent(); }
    public boolean sotepeS4SFTPPerustiedotEnabled() { return sotepeConfig.s4SftpPasswordPerustiedot().isPresent(); }

    public boolean kaskoFTPToteumatEnabled() {
        return kaskoConfig.ftpPasswordToteumat().isPresent();
    }
    public boolean kaskoS4SFTPToteumatEnabled() { return kaskoConfig.s4SftpPasswordToteumat().isPresent(); }

    public boolean kaskoFTPPerustiedotEnabled() {
        return kaskoConfig.ftpPasswordPerustiedot().isPresent();
    }
    public boolean kaskoS4SFTPPerustiedotEnabled() { return kaskoConfig.s4SftpPasswordPerustiedot().isPresent(); }

    public boolean localPerustiedotEnabled() {
        return localFileConfig.perustiedot().orElse(false);
    }

    public boolean localToteumatEnabled() {
        return localFileConfig.toteumat().orElse(false);
    }

    public boolean localCoToteumatEnabled() {
        return localFileConfig.coToteumat().orElse(false);
    }


    public boolean localOrFTPPerustiedotEnabled() {
        return localPerustiedotEnabled() ||
                sotepeFTPPerustiedotEnabled() || palkeFTPPerustiedotEnabled() || kaskoFTPPerustiedotEnabled() ||
                sotepeS4SFTPPerustiedotEnabled() || palkeS4SFTPPerustiedotEnabled() || kaskoS4SFTPPerustiedotEnabled();
    }

    public boolean localOrFTPToteumatEnabled() {
        return localToteumatEnabled() ||
                sotepeFTPToteumatEnabled() || palkeFTPToteumatEnabled() || kaskoFTPToteumatEnabled();
    }

    public boolean localOrFTPCoToteumatEnabled() {
        return localCoToteumatEnabled() || palkeFTPCoToteumatEnabled();
    }

    public boolean localS4ToteumatEnabled() {
        return localFileConfig.s4Toteumat().orElse(false);
    }

    public boolean localOrSFTPS4ToteumatEnabled() {
        return localS4ToteumatEnabled() ||
                sotepeS4SFTPToteumatEnabled() || palkeS4SFTPToteumatEnabled() || kaskoS4SFTPToteumatEnabled();
    }
}
