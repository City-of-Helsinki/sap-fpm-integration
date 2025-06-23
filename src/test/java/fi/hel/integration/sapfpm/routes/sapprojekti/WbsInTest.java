package fi.hel.integration.sapfpm.routes.sapprojekti;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class WbsInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void shouldParse_WBS_OUT_palke() throws Exception {
        Exchange ex = createTestExchange(List.of("9500"));
        Exchange res = producerTemplate.send("direct:process-wbs-palke", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(1, vals.size());

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Wbs-palke", ex);
        assertEquals("9500;00004047;posid;Post1 text öää;\" 1\";20111231;20250218;VAPA\r\n", resCsv.getMessage().getBody(String.class));
    }

    @Test
    void shouldParse_WBS_OUT_with_project_filtering() throws Exception {
        String toimiala = "kasko";
        String PBUKR = "1400";
        Exchange res = producerTemplate.send("direct:process-wbs-" + toimiala, producerTemplate.send("direct:unmarshal-xml", createTestExchange(List.of(PBUKR))));
        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(1, vals.size());
        assertEquals(PBUKR, vals.getFirst().get("PBUKR"));

        toimiala = "palke";
        PBUKR = "9500";
        res = producerTemplate.send("direct:process-wbs-" + toimiala, producerTemplate.send("direct:unmarshal-xml", createTestExchange(List.of(PBUKR))));
        vals = res.getMessage().getBody(List.class);
        assertEquals(1, vals.size());
        assertEquals(PBUKR, vals.getFirst().get("PBUKR"));

        toimiala = "sotepe";
        PBUKR = "3900";
        res = producerTemplate.send("direct:process-wbs-" + toimiala, producerTemplate.send("direct:unmarshal-xml", createTestExchange(List.of(PBUKR))));
        vals = res.getMessage().getBody(List.class);
        assertEquals(1, vals.size());
        assertEquals(PBUKR, vals.getFirst().get("PBUKR"));
        assertEquals("00004047", vals.getFirst().get("PSPNR"));
    }

    Exchange createTestExchange(List<String> PBUKRs) {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

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
            <CREDAT>20250219</CREDAT>
            <CRETIM>000128</CRETIM>
            <SERIAL>20250219000123</SERIAL>
            </EDI_DC40>""" +
                PBUKRs.stream().map(this::createProjektiRakenteenosaSegment).collect(Collectors.joining("")) +
            """
            </IDOC>
            </ZHKI_PROJEKTIRAKENTEENOSA>
            """;
        ex.getMessage().setHeader("CamelFileName", "WBS_OUT_167_SOTE20250219-000123-456.xml");
        ex.getMessage().setBody(xmlIn);
        return ex;
    }

    String createProjektiRakenteenosaSegment(String PBUKR) {
        return """
    <ZHKI_PROJEKTIRAKENTEENOSA SEGMENT="1">
                            <PBUKR>""" + PBUKR + """
                            </PBUKR>
                            <PSPNR>00004047</PSPNR>
                            <POSID>posid</POSID>
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
                            <TXT40>VAPA</TXT40>
                            <BELKZ>X</BELKZ>
                            <OBJNR>PR00004047</OBJNR>
                            </ZHKI_PROJEKTIRAKENTEENOSA>
                            """;
    }

}