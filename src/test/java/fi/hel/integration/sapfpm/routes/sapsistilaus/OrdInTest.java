package fi.hel.integration.sapfpm.routes.sapsistilaus;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OrdInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:ord-out")
    MockEndpoint mockFileOut;

    @BeforeEach
    public void beforeAll() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "ordCsvOut", b ->
            b.weaveByToUri("direct:any-file-out").replace().to(mockFileOut.getEndpointUri()));
    }

    @Test
    void shouldParse_ORD_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        mockFileOut.whenAnyExchangeReceived(e -> {
            InputStreamCache c = e.getMessage().getBody(InputStreamCache.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            c.writeTo(out);
            String data = out.toString();
            out.close();
            assertEquals("BUKRS;AUART;AUFNR;KTEXT;STTXT\r\n" +
                   "3900;3901;3963110753;Asumisen tuki/0753;VAPA\r\n" +
                   "3900;3901;3974190310;LAKOSO Etelä-Itä kotipalvelu/0310;VAPA\r\n", data);
        });

        String xmlIn = """
    <ZHKI_TARSISTILAUKSET>
    <IDOC BEGIN="1">
    <EDI_DC40 SEGMENT="1">
    <TABNAM>EDI_DC40</TABNAM>
    <MANDT>300</MANDT>
    <DOCNUM>0000000000352688</DOCNUM>
    <DOCREL>758</DOCREL>
    <STATUS>30</STATUS>
    <DIRECT>1</DIRECT>
    <OUTMOD>2</OUTMOD>
    <IDOCTYP>ZHKI_TARSISTILAUKSET</IDOCTYP>
    <MESTYP>ZHKI_TARSISTILAUKSET</MESTYP>
    <SNDPOR>SAPQ50</SNDPOR>
    <SNDPRT>LS</SNDPRT>
    <SNDPRN>Q50CLNT300</SNDPRN>
    <RCVPOR>PO_Q21</RCVPOR>
    <RCVPRT>LS</RCVPRT>
    <RCVPRN>PO_GEN</RCVPRN>
    <CREDAT>20241023</CREDAT>
    <CRETIM>190021</CRETIM>
    <SERIAL>20241023190021</SERIAL>
    </EDI_DC40>
    <ZHKI_TARSISTILAUKSET SEGMENT="1">
    <BUKRS>3900</BUKRS>
    <AUART>3901</AUART>
    <AUFNR>3963110753</AUFNR>
    <KTEXT>Asumisen tuki/0753</KTEXT>
    <STTXT>VAPA</STTXT>
    <AUTYP>01</AUTYP>
    </ZHKI_TARSISTILAUKSET>
    <ZHKI_TARSISTILAUKSET SEGMENT="1">
    <BUKRS>3900</BUKRS>
    <AUART>3901</AUART>
    <AUFNR>3974190310</AUFNR>
    <KTEXT>LAKOSO Etelä-Itä kotipalvelu/0310</KTEXT>
    <STTXT>VAPA</STTXT>
    <AUTYP>01</AUTYP>
    </ZHKI_TARSISTILAUKSET>
    </IDOC>
    </ZHKI_TARSISTILAUKSET>
    """;
        ex.getMessage().setHeader("CamelFileName", "ORD_OUT_167_SOTE20241023-190022.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-ord", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals =  res.getMessage().getBody(List.class);
        assertEquals(2, vals.size());

        producerTemplate.sendBody("direct:ord-csv-out", vals);

        mockFileOut.expectedMessageCount(1);
        mockFileOut.assertIsSatisfied();
    }

}
