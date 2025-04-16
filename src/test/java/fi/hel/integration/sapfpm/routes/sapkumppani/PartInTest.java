package fi.hel.integration.sapfpm.routes.sapkumppani;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class PartInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void shouldParse_PART_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = """
<Kumppanit xmlns:prx="urn:sap.com:proxy:P10:/1SAI/TAS1C232228B2827D2C086F:740">
   <Kumppaniyhtiö>
   <RCOMP>000001</RCOMP>
   <NAME1>Valtio ( virastot ja laitokse</NAME1>
   </Kumppaniyhtiö>
   <Kumppaniyhtiö>
   <RCOMP>000002</RCOMP>
   <NAME1>Kunnat</NAME1>
   </Kumppaniyhtiö>
</Kumppanit>
    """;
        ex.getMessage().setHeader("CamelFileName", "PART_OUT_167_SOTE20241023-190022.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-part", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(2, vals.size());

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Part-palke", ex);

        assertEquals("000001;Valtio ( virastot ja laitokse\r\n" +
                "000002;Kunnat\r\n", resCsv.getMessage().getBody(String.class));
    }

}
