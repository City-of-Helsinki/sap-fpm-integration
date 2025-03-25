package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.aggregationstrategy.FileAggregationStrategy;
import fi.hel.integration.sapfpm.routes.LoopingFileReader;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.jboss.logging.Logger;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;

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
    final static String POLL_ENRICH_IN = "file:partIn";

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    @Override
    public void configure() throws Exception {
        // read oldest first in case there are multiple files, so the latest file is created and sent last
        from(POLL_ENRICH_IN + "?sortBy=file:name&charset=ISO-8859-1&include=RAW(" + IN_FILE_PREFIX + ".*.xml)")
            .log("polled file ${headers.CamelFileName}, poll was empty ${exchangeProperty.pollWasEmpty}")
            .to("direct:unmarshal-xml-and-process-part")
            .setHeader("CamelFileName", constant("SAPKUMPPANI.csv"))
            .to("direct:part-csv-out");

        from("direct:unmarshal-xml-and-process-part")
                .unmarshal().jacksonXml().to("direct:process-part");

        from("direct:process-part").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        ).id("ProcessPartOut");

        from("direct:part-csv-out").id("partCsvOut")
                .marshal(partCsvDataFormat)
                .to("direct:part-file-out");

        from("direct:part-file-out").id("PARTFileOut")
            .to("file:partOut?fileExist=Override")
            .log("File ${headers.CamelFileName} written");
    }
}