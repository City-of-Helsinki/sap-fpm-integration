package fi.hel.integration.sapfpm.routes.sapkumppani;

import fi.hel.integration.sapfpm.aggregationstrategy.AggregateLinesWithoutStacking;
import fi.hel.integration.sapfpm.config.PalkeConfig;
import fi.hel.integration.sapfpm.routes.PerustiedotRouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import java.util.*;

import static fi.hel.integration.sapfpm.IDOCParser.*;
import static fi.hel.integration.sapfpm.routes.InRouteBuilder.buildInParams;

// PART_OUT_ _> SAPKUMPPANI
// kaikki kumppanit, yksi tiedosto per päivä
// eli tässä ei tarvita kaikkien lukemista, viimeisin riittää
@ApplicationScoped
public class PartInRouteBuilder extends PerustiedotRouteBuilder {
    @Inject
    PalkeConfig palkeConfig;

    CsvDataFormat partCsvDataFormat = new CsvDataFormat().setQuoteDisabled(true).setDelimiter(';').setHeader(new String[] {
        "RCOMP", "NAME1"
    });

    public LinkedHashMap<String, Object> extractValues(Map<String, Object> valuesLine) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>(); // order matters
        part.put("RCOMP", valuesLine.get("RCOMP")); // kumppanikoodi
        part.put("NAME1", valuesLine.get("NAME1")); // lyhyt nimi
        return part;
    }

    // PART_OUT_167_SOTE
    // PALKE: PART_OUT_138_9500
    @Override
    public String getFilePrefix() {
        return "PART_OUT_";
    }

    @Override
    public String getFtpDir() {
        return "210";
    }

    @Override
    public void buildMainRoute(String fileOrFtpIn, String toimiala) {
        from(fileOrFtpIn).id("PartIn" + toimiala)
            .to("direct:unmarshal-xml")
            .to("direct:process-part")
            .aggregate(new AggregateLinesWithoutStacking()).constant(true).completionFromBatchConsumer()
            .setHeader("CamelFileName", constant("SAPKUMPPANI.csv"))
            .setProperty("outDir", constant(toimiala))
            .to("direct:part-csv-out")
            .choice()
                .when(simple("${exchangeProperty.outDir} == 'palke'"))
                    .setProperty("uploadFileDir", constant("SAS/TEST"))
                    .to("direct:upload-blob-to-azure-" + toimiala)
            .end();
    }

    @Override
    public void buildSupportingRoutes() {
        from("direct:process-part").id("ProcessPartOut").setBody(e ->
            extractValuesDirectlyFromXML(e, "Kumppaniyhtiö", this::extractValues)
        );

        from("direct:part-csv-out").routeId("partCsvOut")
            .marshal(partCsvDataFormat)
            .to("direct:any-file-out");

    }
}