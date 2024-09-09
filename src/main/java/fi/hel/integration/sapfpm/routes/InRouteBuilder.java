package fi.hel.integration.sapfpm.routes;

import fi.hel.integration.sapfpm.config.AppConfig;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.component.file.GenericFileOperationFailedException;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@ApplicationScoped
public class InRouteBuilder extends RouteBuilder {

    @Inject
    AppConfig appConfig;


    @Override
    public void configure() throws Exception {
       
        from("direct:fpm-files-in").to("direct:process-fpm");
           
        from("direct:process-fpm")
             .id("ProcessFPM")
            .log("FPM IN :: ${headers.CamelFileName} :: profile {{smallrye.config.profile}}")
            .setProperty(Exchange.CHARSET_NAME, constant("ISO-8859-1"))
            .log("processed, writing to Azure")
           // .split(body().tokenize(LINE_END, 1, true), new PersonWithBenefitSumsAggregator() {}).process(e -> {}).end().log("all processed")
            .to("direct:azure-out");

        from("direct:azure-out")
            .setProperty(Exchange.CHARSET_NAME, constant("ISO-8859-1"))
            .log("Azure out done").process(e -> e.getMessage().setHeader("ok", "ok"));
    }
}
