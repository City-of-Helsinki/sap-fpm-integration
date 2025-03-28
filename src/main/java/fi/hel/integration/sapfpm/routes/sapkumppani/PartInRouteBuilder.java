package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.aggregationstrategy.AggregateLinesWithoutStacking;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;

// PART_OUT_ _> SAPKUMPPANI
// kaikki kumppanit, yksi tiedosto per päivä
// eli tässä ei tarvita kaikkien lukemista, viimeisin riittää
@ApplicationScoped
public class PartInRouteBuilder extends RouteBuilder {
    CsvDataFormat partCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
        "RCOMP", "NAME1"
    });

    // PART_OUT_167_SOTE
    // PALKE: PART_OUT_138_9500
    final static String IN_FILE_PREFIX = "PART_OUT_";

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    @Override
    public void configure() throws Exception {
        // include or antInclude only works with 1 file at a time
        from("file:in?" + buildInParams(IN_FILE_PREFIX)).id("partIn")
            .to("direct:unmarshal-and-process-part")
            .aggregate(new AggregateLinesWithoutStacking()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPKUMPPANI.csv"))
            .to("direct:part-csv-out");

        from("direct:unmarshal-and-process-part")
            .unmarshal().jacksonXml().to("direct:process-part");

        from("direct:process-part").id("ProcessPartOut").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        );

        from("direct:part-csv-out").routeId("partCsvOut")
                .marshal(partCsvDataFormat)
                .to("direct:any-file-out");
    }
}