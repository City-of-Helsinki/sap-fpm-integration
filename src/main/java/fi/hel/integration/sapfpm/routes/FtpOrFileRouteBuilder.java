package fi.hel.integration.sapfpm.routes;


public interface FtpOrFileRouteBuilder {
    String getFilePrefix();
    String getFtpDir(String toimiala);
    void buildMainRoute(String fileOrFtpIn, String toimiala);
    void buildSupportingRoutes();
}

