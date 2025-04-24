package fi.hel.integration.sapfpm.routes;


public interface FtpOrFileRouteBuilder {
    String getFilePrefix();
    String getFtpDir();
    void buildMainRoute(String fileOrFtpIn, String toimiala);
    void buildSupportingRoutes();
}

