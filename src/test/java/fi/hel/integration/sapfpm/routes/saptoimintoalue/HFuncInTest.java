package fi.hel.integration.sapfpm.routes.saptoimintoalue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HFuncInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:direct:enrich-and-send-file-to-azure-palke-HFunc")
    private MockEndpoint mockUploadBlobToAzurePalkeAnyFileOutPart;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "HFunc-palke-process", b -> {
            b.interceptSendToEndpoint("direct:enrich-and-send-file-to-azure-palke").to(mockUploadBlobToAzurePalkeAnyFileOutPart.getEndpointUri())
                .skipSendToOriginalEndpoint();
        });
    }

    @Test
    void shouldParse_H_FUNC_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = """
<SI_FunctionalArea_OUT><Functional_area>
<Line>
<Nimi>HKI</Nimi><Hierarkiataso>0</Hierarkiataso><Joukko>X</Joukko><Ylahierarkia/><Toiminto-alue/><Kuvaus>ylätaso.</Kuvaus><alku>00000000</alku><Loppu>00000000</Loppu>
</Line>
<Line>
<Nimi/><Hierarkiataso>0</Hierarkiataso><Joukko/><Ylahierarkia>ZZZ</Ylahierarkia><Toiminto-alue>Z002</Toiminto-alue><Kuvaus>Siirto</Kuvaus><alku>20110101</alku><Loppu>99991231</Loppu>
</Line>
<Line>
<Nimi/><Hierarkiataso>1</Hierarkiataso><Joukko/><Ylahierarkia>95</Ylahierarkia><Toiminto-alue>958001</Toiminto-alue><Kuvaus>Kuvaus</Kuvaus><alku>20100101</alku><Loppu>99991231</Loppu>
</Line>
</Functional_area></SI_FunctionalArea_OUT>
    """;
        ex.getMessage().setHeader("CamelFileName", "H_FUNC_OUT_167_PALKE_20241023-190022.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-hfunc-palke", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(1, vals.size());

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-csv-HFunc-palke", ex);

        assertEquals("Nimi;Hierarkiataso;Joukko;Ylahierarkia;Toiminto-alue;Kuvaus;alku;Loppu\r\n" +
                "\"\";1;;95;958001;Kuvaus;20100101;99991231\r\n", resCsv.getMessage().getBody(String.class));
    }

    @Test
    void includeToimintoalueTest() {
        HFuncInRouteBuilder h = new HFuncInRouteBuilder();
        String toimiala = "palke";
        assertFalse(h.includeToimintoalue(toimiala, null));
        assertFalse(h.includeToimintoalue(toimiala, ""));
        assertFalse(h.includeToimintoalue(toimiala, " "));
        assertTrue(h.includeToimintoalue(toimiala, "95100224"));
        assertTrue(h.includeToimintoalue(toimiala, "10400"));
        toimiala = "kasko";
        assertTrue(h.includeToimintoalue(toimiala, "140230"));
        assertTrue(h.includeToimintoalue(toimiala, "10999"));
        toimiala = "sotepe";
        assertTrue(h.includeToimintoalue(toimiala, "393920"));
        assertTrue(h.includeToimintoalue(toimiala, "70352352"));
        assertTrue(h.includeToimintoalue(toimiala, "101"));
    }
}
