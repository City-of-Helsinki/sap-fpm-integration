package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.AppConfig;

import fi.hel.integration.sapfpm.model.ID022_FI_TOSITE.E1FIKPF;
import fi.hel.integration.sapfpm.model.ID022_FI_TOSITE.FIDCCP02;
import fi.hel.integration.sapfpm.model.ID022_FI_TOSITE.IDOC;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.component.file.GenericFileOperationFailedException;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/*Sapista FPM:lle:
BKPF, BSEG ja FMGLEXA tulevat jatkossa kaikki yhdessä ja samassa tiedostossa eli tässä uudessa toteutettavassa toteumatiedostossa.

KNA1 = asiakkaat, tälle ei perustietoliittymää eikä tule sotepelle käyttöön   (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
LFA1 = toimittajat, tälle on perustietoliittymä mutta ei tule sopete käyttöön (pieni varaus Kasko ja Palke en ole 100 % varma ovatko käyttäneet)
PRPS = projekti, tälle toimiva perustietoliittymä tulee kaikkiin FPM Cloudeihin*/
@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    @Inject
    AppConfig appConfig;

    @Inject
    Logger log;

    CsvDataFormat csvDataFormat = new CsvDataFormat().setDelimiter(';');


    // TODO: read xml as maps, write out

    @Override
    public void configure() throws Exception {

        log.info("Profile: {{smallrye.config.profile}}");

        //
        //from("file:in?include=RAW(ID022_.*.xml)").to("direct:fpm-files-in");

        // Without POJO / with plain maps, the csv can contain a non-constant amount of
        // headers!
        from("direct:fpm-files-in")
            .log("IN :: ${headers.CamelFileName}")
            .unmarshal().jacksonXml(/*FIDCCP02.class*/)
            .to("direct:process-fpm-without-pojo");

        from("direct:process-fpm").id("ProcessFPM")
            .process(e -> {
                var values = e.getIn().getBody(FIDCCP02.class).IDOC.E1FIKPF;
                // TODO: save into db inside a transaction
                // TODO: e.getIn().getProperty("mapOfAllFiles") and put there
                var mappedByYearAndMonth = values.stream().collect(Collectors.groupingBy(v ->
                    v.GJAHR + " " + v.MONAT
                ));
                e.getIn().setBody(mappedByYearAndMonth);
            })
            .setProperty(Exchange.CHARSET_NAME, constant("ISO-8859-1"))
            .log("processed, writing to Azure")
            .to("direct:azure-out");

        from("direct:process-fpm-without-pojo").id("ProcessPojolessFPM")
                .process(e -> {
                    Map<String, Map<String, List<Map<String, Object>>>> xmlRoot = e.getIn().getBody(Map.class);
                    //
                    Map<String, List<Map<String, Object>>> IDOC = xmlRoot.get("IDOC");
                    List<Map<String, Object>> values = IDOC.get("E1FIKPF");
                    var mappedByYearAndMonth = values.stream().collect(Collectors.groupingBy(v ->
                            v.get("GJAHR") + " " + v.get("MONAT")
                    ));
                    e.getIn().setBody(mappedByYearAndMonth.values());
                })
                .split(body())
                .to("direct:split-E1FIKPF")
                // each E1FIKPF per body, TODO: now split per each map that contains SEGMENT into own file

                .log("processed, writing to Azure")
                .to("direct:azure-out");

        from("direct:split-E1FIKPF")
            .process(e -> {
                Map<String, Object> elem = e.getIn().getBody(Map.class);
                // TODO: move into own method and call recursively?
                Map<Boolean, List<Map.Entry<String, Object>>> elems = elem.entrySet().stream().collect(
                        Collectors.groupingBy(kV -> kV.getValue() instanceof Map));

                Function<Map.Entry<String, Object>, String> keyMapper = Map.Entry::getKey;
                Function<Map.Entry<String, Object>, Object> valueMapper = Map.Entry::getValue;

                // TODO: many of the xml-files actually contain:
                // as first subelement: common values
                // as next subelements: variable values that all should have common values also added to them
                // set
              /*  List<Set<String, Map<String, Object>>> allVals = elems.values().stream().map(entries ->
                    entries.stream().collect(Collectors.toMap(keyMapper, valueMapper))
                ).toList();

                // TODO: send non-maps as is but set file name

                e.getIn().setBody(allVals);*/
            }).split(body()).to("direct:azure-out");


        from("direct:azure-out")
            .setProperty(Exchange.CHARSET_NAME, constant("ISO-8859-1"))
            .marshal(csvDataFormat)
            .to("file:out")
            .log("Azure out done ${headers.CamelFileName}").process(e -> e.getMessage().setHeader("ok", "ok"));
    }
}
