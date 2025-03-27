package fi.hel.integration.sapfpm.routes.sapprojekti;

import fi.hel.integration.sapfpm.aggregationstrategy.ProcessedLinesAggregationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;
// TODO: saattaa olla PROJECT nimellä testissä!!!

// WBS_OUT:n käsittelee Projektit ja projektin rakenneosat  (SAPPROJEKTI_PRPS, WBS_OUT.xml)
// tuplat: xml ehkä järjestyksessä, eli jos saman filun sisällä tulee useampi, valitse jälkimmäinen?
// PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin
@ApplicationScoped
public class WBSInRouteBuilder extends RouteBuilder {
    CsvDataFormat wbsCsvDataFormat = new CsvDataFormat().setDelimiter(';').setQuoteDisabled(true).setHeader(new String[] {
        "PBUKR", "POSID", "POST1", "STUFE", "ERDAT", "AEDAT", "TXT40"
    });

    final static String IN_FILE_PREFIX = "WBS_OUT_";
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
    public void configure() throws Exception {

        // include or antInclude only works with 1 file at a time
        from("file:in?" + buildInParams(IN_FILE_PREFIX)).id("wbsIn")
            .to("direct:unmarshal-and-process-wbs")
            .aggregate(new ProcessedLinesAggregationStrategy()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPPROJEKTI_PRPS.csv"))
            .to("direct:wbs-csv-out");

        from("direct:unmarshal-and-process-wbs")
            .unmarshal().jacksonXml().to("direct:process-wbs");

        from("direct:process-wbs").id("ProcessWBS")
            .setBody(e -> extractValuesFromIDOC(e,  "ZHKI_PROJEKTIRAKENTEENOSA", this::extractValues));

        from("direct:wbs-csv-out").routeId("wbsCsvOut")
            .marshal(wbsCsvDataFormat)
            .to("direct:any-file-out");
    }
}

