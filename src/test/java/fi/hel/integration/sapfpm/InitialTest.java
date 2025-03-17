package fi.hel.integration.sapfpm;

import fi.hel.integration.sapfpm.routes.InRouteBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@ApplicationScoped
class InitialTest {
    @Inject
    InRouteBuilder in;

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void fpmShouldReadAndParse_ID022_XMLFiles() throws InterruptedException, Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        ex.getIn().setHeader("CamelFileName", "ID022_FI_TOSITE_20240829-113059-336.xml");
        ex.getIn().setBody("<FIDCCP02 xmlns:prx=\"urn:sap.com:proxy:P10:/1SAI/TAS1453E8F459338B01509F:740\">\n" +
            "<IDOC>\n" +
                "<E1FIKPF>\n" +
                    "<BUKRS>3900</BUKRS>\n" +
                    "<BELNR>0000000001</BELNR>\n" +
                    "<GJAHR>2023</GJAHR>\n" +
                    "<BLART>6S</BLART>\n" +
                    "<BLDAT>20230227</BLDAT>\n" +
                    "<BUDAT>20230227</BUDAT>\n" +
                    "<MONAT>02</MONAT>\n" +
                "</E1FIKPF>\n" +
                "<E1FIKPF>\n" +
                    "<BUKRS>3900</BUKRS>\n" +
                    "<BELNR>0000000002</BELNR>\n" +
                    "<GJAHR>2023</GJAHR>\n" +
                    "<BLART>6S</BLART>\n" +
                    "<BLDAT>20230227</BLDAT>\n" +
                    "<BUDAT>20230227</BUDAT>\n" +
                    "<MONAT>02</MONAT>\n" +
                "</E1FIKPF>\n" +
            "</IDOC>\n" +
        "</FIDCCP02>");



        AdviceWith.adviceWith(ctx, "ProcessFPM", a -> {
            // a.mockEndpointsAndSkip("direct:azure-out");
        });

        Exchange res = producerTemplate.send("direct:fpm-files-in", ex);
        System.out.println("DERP: " + res.getMessage().getBody().getClass());

        //assertEquals("ok", res.getMessage().getBody(List.class));
    }

    @EndpointInject("mock:out")
    private MockEndpoint mockFileOut;

    @Test
    void fpmShouldParse_ORD_OUT() throws Exception {
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

        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        ex.getMessage().setHeader("CamelFileName", "ORD_OUT_167_SOTE20241023-190022.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:any-sap-file-in", ex);

        AdviceWith.adviceWith(ctx, "GenericFileOut", builder -> {
            builder.interceptSendToEndpoint("file:out")
                .skipSendToOriginalEndpoint()
                .to(mockFileOut.getEndpointUri());
        });

        mockFileOut.whenAnyExchangeReceived(e -> {
            InputStreamCache c = e.getMessage().getBody(InputStreamCache.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            c.writeTo(out);
            String data = out.toString();
            assertEquals("""
BUKRS;AUART;AUFNR;KTEXT;STTXT
3900;3901;3963110753;Asumisen tuki/0753;VAPA
3900;3901;3974190310;LAKOSO Etelä-Itä kotipalvelu/0310;VAPA""", data);
        });

        Map<String, List<LinkedHashMap<String, Object>>> entry = res.getMessage().getBody(Map.class);
        assertTrue(entry.containsKey("202410"));
        List<LinkedHashMap<String, Object>> vals = entry.get("202410");
        assertEquals(2, vals.size());

        producerTemplate.sendBody("direct:ord-azure-out", entry.get("202410"));

        mockFileOut.expectedMessageCount(1);
    }

}
