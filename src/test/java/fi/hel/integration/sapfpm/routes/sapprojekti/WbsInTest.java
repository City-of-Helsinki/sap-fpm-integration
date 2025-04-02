package fi.hel.integration.sapfpm.routes.sapprojekti;


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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class WbsInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:wbs-out")
    MockEndpoint mockFileOut;

    @BeforeEach
    public void beforeAll() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "wbsCsvOut", b ->
                b.weaveByToUri("direct:any-file-out").replace().to(mockFileOut.getEndpointUri()));
    }

    @Test
    void shouldParse_WBS_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String CREDAT = "20250219";

        mockFileOut.whenAnyExchangeReceived(e -> {
            InputStreamCache c = e.getMessage().getBody(InputStreamCache.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            c.writeTo(out);
            out.close();
            String data = out.toString();
            assertEquals(
"PBUKR;POSID;POST1;STUFE;ERDAT;AEDAT;TXT40\r\n" +
"1000;1024200;Post1 text öää; 1;20111231;20250218;ZZZZ ZZZZ\r\n" +
"9000;posid2; Vapaa teksti ; 5;20250218;00000000;VAPA\r\n", data);
        });

        String xmlIn = """
            <ZHKI_PROJEKTIRAKENTEENOSA>
            <IDOC BEGIN="1">
            <EDI_DC40 SEGMENT="1">
            <TABNAM>EDI_DC40</TABNAM>
            <MANDT>300</MANDT>
            <DOCNUM>0000000123456789</DOCNUM>
            <DOCREL>740</DOCREL>
            <STATUS>30</STATUS>
            <DIRECT>1</DIRECT>
            <OUTMOD>2</OUTMOD>
            <IDOCTYP>ZHKI_PROJEKTIRAKENTEENOSA</IDOCTYP>
            <MESTYP>ZHKI_PROJEKTIRAKENTEENOSA</MESTYP>
            <SNDPOR>SAPP10</SNDPOR>
            <SNDPRT>LS</SNDPRT>
            <SNDPRN>P10CLNT300</SNDPRN>
            <RCVPOR>PI_P21</RCVPOR>
            <RCVPRT>LS</RCVPRT>
            <RCVPRN>P20CLNT300</RCVPRN>
            <CREDAT>""" + CREDAT +
"""
</CREDAT>
            <CRETIM>000128</CRETIM>
            <SERIAL>20250219000123</SERIAL>
            </EDI_DC40>
            <ZHKI_PROJEKTIRAKENTEENOSA SEGMENT="1">
            <PBUKR>1000</PBUKR>
            <PSPNR>00004047</PSPNR>
            <POSID>1024200</POSID>
            <POST1>Post1 text öää</POST1>
            <PSPHI>00000380</PSPHI>
            <UP>00000000</UP>
            <STUFE> 1</STUFE>
            <VERNR>00000000</VERNR>
            <ASTNR>00000000</ASTNR>
            <PRCTR>0001000000</PRCTR>
            <PSTRT>20120101</PSTRT>
            <PENDE>20201231</PENDE>
            <ESTRT>00000000</ESTRT>
            <EENDE>00000000</EENDE>
            <ISTRT>00000000</ISTRT>
            <IENDE>00000000</IENDE>
            <PLAKZ>X</PLAKZ>
            <USR04> 0.000</USR04>
            <USR05> 0.000</USR05>
            <USR06> 0.000</USR06>
            <USR07> 0.000</USR07>
            <USR08>20110101</USR08>
            <USR09>20131231</USR09>
            <ERDAT>20111231</ERDAT>
            <AEDAT>20250218</AEDAT>
            <TXT40>ZZZZ ZZZZ</TXT40>
            <BELKZ>X</BELKZ>
            <OBJNR>PR00004047</OBJNR>
            </ZHKI_PROJEKTIRAKENTEENOSA>
            <ZHKI_PROJEKTIRAKENTEENOSA SEGMENT="1">
            <PBUKR>9000</PBUKR>
            <PSPNR>123456</PSPNR>
            <POSID>posid2</POSID>
            <POST1> Vapaa teksti </POST1>
            <PSPHI>00012345</PSPHI>
            <UP>00000123</UP>
            <STUFE> 5</STUFE>
            <PRART>01</PRART>
            <VERNR>01234567</VERNR>
            <VERNA>Name</VERNA>
            <ASTNR>00000000</ASTNR>
            <PRCTR>0001000001</PRCTR>
            <SLWID>HKI_005</SLWID>
            <PLAKZ>X</PLAKZ>
            <USR04> 0.000</USR04>
            <USR05> 0.000</USR05>
            <FAKKZ>X</FAKKZ>
            <USR06> 0.000</USR06>
            <USR07> 0.000</USR07>
            <USR08>00000000</USR08>
            <USR09>00000000</USR09>
            <ERDAT>20250218</ERDAT>
            <AEDAT>00000000</AEDAT>
            <TXT40>VAPA</TXT40>
            <BELKZ>X</BELKZ>
            <OBJNR>PR00000012</OBJNR>
            </ZHKI_PROJEKTIRAKENTEENOSA>
    </IDOC>
    </ZHKI_PROJEKTIRAKENTEENOSA>
    """;
        ex.getMessage().setHeader("CamelFileName", "WBS_OUT_167_SOTE20250219-000123-456.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-wbs", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(2, vals.size());

        producerTemplate.sendBody("direct:wbs-csv-out", vals);

        mockFileOut.expectedMessageCount(1);
        mockFileOut.assertIsSatisfied();
    }

}