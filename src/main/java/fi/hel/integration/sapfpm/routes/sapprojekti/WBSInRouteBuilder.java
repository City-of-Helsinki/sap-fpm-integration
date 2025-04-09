package fi.hel.integration.sapfpm.routes.sapprojekti;

import fi.hel.integration.sapfpm.aggregationstrategy.AggregateLinesWithoutStacking;
import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
// WBS_OUT:n käsittelee Projektit ja projektin rakenneosat  (SAPPROJEKTI_PRPS, WBS_OUT.xml)
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
// PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin
@ApplicationScoped
public class WBSInRouteBuilder extends PerustiedotRouteBuilder {
    CsvDataFormat wbsCsvDataFormat = new CsvDataFormat().setDelimiter(';').setQuoteDisabled(true).setHeader(new String[] {
        "PBUKR", "POSID", "POST1", "STUFE", "ERDAT", "AEDAT", "TXT40"
    });

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> project = new LinkedHashMap<>();
        project.put("PBUKR", valuesLine.get("PBUKR"));
        project.put("POSID", valuesLine.get("POSID"));
        project.put("POST1", valuesLine.get("POST1"));
        project.put("STUFE", valuesLine.get("STUFE"));
        project.put("ERDAT", valuesLine.get("ERDAT"));
        project.put("AEDAT", valuesLine.get("AEDAT"));
        project.put("TXT40", valuesLine.get("TXT40"));
        return project;
    }

    @Override
    public String getFilePrefix() {
        return "(WBS_OUT_|PROJECT)";
        //  ".*FI_TOSITE_"
    }

    @Override
    public String getFtpDir() {
        return "204";
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        log.info("Starting wbs/project " + toimiala);
        from(fileOrFtpIn).id((toimiala == null ? "" : toimiala) + "wbsIn")
                // errorHandler(noErrorHandler())
            .log("%s read ${headers.CamelFileName}".formatted(toimiala))
            .to("direct:unmarshal-xml")
            .to("direct:process-wbs")
            .aggregate(new AggregateLinesWithoutStacking()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPPROJEKTI.csv"))
            .setProperty("outDir", constant(toimiala))
            .setProperty("fileUploadDir", constant("SAP/TEST"))
            .to("direct:wbs-csv-out");
        //direct:upload-blob-to-azur
    }

    @Override
    public void buildSupportingRoutes() {
        from("direct:process-wbs").id("ProcessWBS")
            .setBody(e -> extractValuesFromIDOC(e,  "ZHKI_PROJEKTIRAKENTEENOSA", this::extractValues));

        from("direct:wbs-csv-out").routeId("wbsCsvOut")
            .marshal(wbsCsvDataFormat)
            .to("direct:any-file-out");
    }
}

