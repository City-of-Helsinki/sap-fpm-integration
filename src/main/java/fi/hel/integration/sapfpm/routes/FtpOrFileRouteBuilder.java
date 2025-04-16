package fi.hel.integration.sapfpm.routes;


public interface FtpOrFileRouteBuilder {
    String getFilePrefix();
    String getFtpDir();
    void buildMainRoute(boolean isLocal, String fileOrFtpIn, String toimiala);
    void buildSupportingRoutes();
}

