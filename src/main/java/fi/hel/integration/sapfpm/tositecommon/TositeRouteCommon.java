package fi.hel.integration.sapfpm.tositecommon;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TositeRouteCommon extends RouteBuilder {

    @ConfigProperty(name = "default-route-redelivery-delay", defaultValue = "10000")
    public int DEFAULT_REDELIVERY_DELAY;

    @ConfigProperty(name = "default-route-max-redeliveries", defaultValue = "120")
    public int DEFAULT_MAX_REDELIVERIES;

    @ConfigProperty(name = "fetch-gjahr-poper-if-changed-after-last-change-days", defaultValue = "90")
    public int FETCH_GJAHR_POPER_IF_CHANGED_DAYS_AFTER_LATEST_TIMESTAMP;

    public void buildFtpBatchingRoute(String fromUri, String routeId, String toimiala, String initDbUri,
         String processFileUri, String onBatchCompletionUri) {

        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        AtomicInteger processedFileAmount = new AtomicInteger(0);

        from(fromUri).routeId(routeId)
            .setHeader("toimiala", constant(toimiala))
            .choice()
                .when(variable("route:dbInited").isNotEqualTo(Boolean.TRUE))
                .log("initing db")
                .to(initDbUri)
                .setVariable("route:dbInited", constant(true))
            .end()
            .onException(Exception.class)
                .maximumRedeliveries(DEFAULT_MAX_REDELIVERIES).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .choice()
                .when(simple("${exchangeProperty.CamelBatchIndex} == 0"))
                .process(e -> {
                    if (initialBatchSize.get() == -1) {
                        initialBatchSize.set(e.getProperty("CamelBatchSize", Integer.class));
                        log.info("set initial batch size to " + initialBatchSize.get());
                    }
                })
            .end()
            .log("read ${headers.CamelFileName}")
            .to(processFileUri)
            .removeProperty("originalBody")
            .removeProperty("sqlNamedParams")
            .removeProperty("sqlValNames")
            .setBody(constant(""))
            .process(e -> {
                int newTotal = processedFileAmount.addAndGet(1);
                e.setProperty("processedFileAmount", newTotal);
                int initial = initialBatchSize.get();
                log.info("initial: " + initial + ", total: " + newTotal);
                if (newTotal >= initial) {
                    e.setProperty("writeOut", true);
                }
                e.setProperty("initialBatchSize", initial);
            })
            .choice().when(simple("${exchangeProperty.writeOut} == true"))
                .log("Done! Group and write unique csvs from db")
                .to(onBatchCompletionUri)
                .process(e -> {
                    e.setProperty("writeOut", false);
                    processedFileAmount.set(0);
                    initialBatchSize.set(-1);
                })
            .end();
    }

    public void buildAppendDbToExistingFileRoute(String fromUri, String routeId, int dbPageLimit, String fetchByYearAndMonthFromDbUri,
             String marshalHeaderlessCsvURI) {
        // outDir has to be set
        from(fromUri).routeId(routeId)
            .errorHandler(noErrorHandler()) // upper route handles errors to avoid duplicate lines in the file
            .setProperty("fileExist", constant("Append"))
            .setHeader("pageLimit", constant(dbPageLimit))
            .setProperty("dbHasMoreResults", constant(true))
            .setHeader("pageLimit", constant(dbPageLimit))
            .setProperty("dbHasMoreResults", constant(true))
            .loopDoWhile(exchangeProperty("dbHasMoreResults").isEqualTo(true))
                .to(fetchByYearAndMonthFromDbUri)
                .removeProperty("originalBody")
                .removeProperty("sqlNamedParams")
                .removeProperty("sqlValNames")
                .process(e -> {
                    List<LinkedHashMap<String, Object>> res = e.getMessage().getBody(List.class);
                    if (res == null || res.isEmpty() || res.size() < dbPageLimit) {
                        e.removeProperty("dbHasMoreResults");
                        e.getMessage().removeHeader("lastId");
                    } else {
                        e.getMessage().setHeader("lastId", res.getLast().get("id"));
                    }
                })
                .choice()
                    .when(simple("${body} != null && ${body.size()} > 0"))
                        .to(marshalHeaderlessCsvURI)
                        .to("direct:any-file-out")
                .end()
                .setBody(constant(""))
            .end();
    }

    public void buildFileAppendingFromDbPageRoute(String fromUri, String routeId, String toimiala,
              String fetchYearsAndMonthsFromDbUri, String initDbFetchParamsAndFileNameUri,
                  String marshalWithHeaderCsvURI, String fetchCountUri, String appendFromDbToFileUri, String sendFileToAzureUri) {

        from(fromUri)
            .routeId(routeId)
            .onException(Exception.class)
                .maximumRedeliveries(DEFAULT_MAX_REDELIVERIES).redeliveryDelay(DEFAULT_REDELIVERY_DELAY)
            .end()
            .setHeader("toimiala", constant(toimiala))
            .setHeader("daysToSubtract", constant(FETCH_GJAHR_POPER_IF_CHANGED_DAYS_AFTER_LATEST_TIMESTAMP))
            .log("Writing db out to azure for ${headers.toimiala}")
            .setProperty("outDir", constant(toimiala))
            .to(fetchYearsAndMonthsFromDbUri)
                .split(body())
                    .to(initDbFetchParamsAndFileNameUri)
                    .setProperty("fileExist", constant("Override"))
                    .setBody(constant(""))
                    .to(marshalWithHeaderCsvURI)
                    .to("direct:any-file-out")
                    .to(fetchCountUri)
                    .to(appendFromDbToFileUri)
                    .log("appending done, enriching and sending to azure")
                    .to(sendFileToAzureUri)
                    .setBody(constant(""))
                .end()
            .log("${headers.toimiala} all months and years sent to Azure from db");
    }

    public void buildDbFetchAndFileNameInitializer(String fromUri, String routeId, String monthValName, String fileNamePrefix) {
        from(fromUri).routeId(routeId)
            .process(e -> {
                LinkedHashMap<String, Object> row = e.getMessage().getBody(LinkedHashMap.class);
                String year = (String)row.get("GJAHR");
                String month = (String)row.get(monthValName);
                e.getMessage().setHeader("GJAHR", year);
                e.getMessage().setHeader(monthValName, month);
                String simpleMonth = month.replaceFirst("^0+", "");
                if (simpleMonth.isEmpty()) {
                    simpleMonth = "0";
                }
                e.getMessage().setHeader(FileConstants.FILE_NAME, fileNamePrefix + "_" + year + "_" + simpleMonth + ".csv");
            });
    }
}
