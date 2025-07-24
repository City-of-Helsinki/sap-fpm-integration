package fi.hel.integration.sapfpm.tositecommon;

import org.apache.camel.builder.RouteBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TositeRouteCommon extends RouteBuilder {

    public void buildFtpBatchingRoute(String fromUri, String routeId, String initRouteUri,
         String processFileUri, String onBatchCompletionUri) {

        AtomicInteger initialBatchSize = new AtomicInteger(-1);
        AtomicInteger processedFileAmount = new AtomicInteger(0);

        from(fromUri).routeId(routeId).to(initRouteUri)
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

    public void buildFileAppendingFromDbPageRoute(String fromUri, String toimiala,
              String fetchYearsAndMonthsFromDbUri, String initDbFetchParamsAndFileNameUri,
                  String marshalWithHeaderCsvURI, String fetchCountUri, int dbPageLimit, String fetchByYearAndMonthFromDbUri, String marshalHeaderlessCsvURI, String sendFileToAzureUri) {
        from(fromUri)
            .setHeader("toimiala", constant(toimiala))
            .log("Writing db out to azure for ${headers.toimiala}")
            .setProperty("outDir", constant(toimiala))
            .to(fetchYearsAndMonthsFromDbUri)
                .split(body())
                    .to(initDbFetchParamsAndFileNameUri)
                    .setProperty("fileExist", constant("Override"))
                    .setBody(constant(""))
                    .to(marshalWithHeaderCsvURI)
                    .to("direct:any-file-out")
                    .setProperty("fileExist", constant("Append"))
                    .to(fetchCountUri)
                    .setHeader("pageLimit", constant(dbPageLimit))
                    .setProperty("dbHasMoreResults", constant(true))
                    .loopDoWhile(exchangeProperty("dbHasMoreResults").isEqualTo(true))
                        .to(fetchByYearAndMonthFromDbUri)
                        .process(e -> {
                            List<LinkedHashMap<String, Object>> res = e.getMessage().getBody(List.class);
                            if (res == null || res.isEmpty() || res.size() < dbPageLimit) {
                                e.removeProperty("dbHasMoreResults");
                                e.getMessage().removeHeader("lastId");
                            } else {
                                e.getMessage().setHeader("lastId", res.getLast().get("id"));
                            }
                        })
                        .to(marshalHeaderlessCsvURI)
                        .to("direct:any-file-out")
                        .removeProperty("originalBody")
                        .removeProperty("sqlNamedParams")
                        .removeProperty("sqlValNames")
                        .setBody(constant(""))
                    .end()
                    .log("appending done, enriching and sending to azure")
                    .to(sendFileToAzureUri)
                    .setBody(constant(""))
                .end()
            .log("${headers.toimiala} all months and years sent to Azure from db");

    }
}
