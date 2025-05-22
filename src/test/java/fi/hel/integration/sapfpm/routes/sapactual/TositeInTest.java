package fi.hel.integration.sapfpm.routes.sapactual;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TositeInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    FITositeInRouteBuilder tositeRoute;

    @EndpointInject("mock:file:out_something")
    private MockEndpoint mockFileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "AnyFileOut", b -> {
            //b.interceptSendToEndpoint("file:*").onWhen(header(FileConstants.FILE_NAME).contains("FI_TOSITE")).to(mockJdbcSapActual.getEndpointUri());
        });
    }

    @Test
    void shouldParse_Tosite_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = """
                <FIDCCP02 xmlns:prx="urn:sap.com:proxy:P10:/1SAI/TAS1234E5F678901A01209F:740">
                <IDOC>
                <E1FIKPF>
                <BUKRS>3900</BUKRS>
                <BELNR>0111123456</BELNR>
                <GJAHR>2022</GJAHR>
                <BLART>6S</BLART>
                <BLDAT>20221230</BLDAT>
                <BUDAT>20221230</BUDAT>
                <MONAT>12</MONAT>
                <CPUDT>20221230</CPUDT>
                <WWERT>20221230</WWERT>
                <USNAM>GORJALJ</USNAM>
                <XBLNR>3920012345</XBLNR>
                <WAERS>EUR</WAERS>
                <KURSF> 1.00000</KURSF>
                <GLVOR>SD00</GLVOR>
                <AWTYP>VBRK</AWTYP>
                <FIKRS>HKI</FIKRS>
                <HWAER>EUR</HWAER>
                <KURS2> 0.00000</KURS2>
                <KURS3> 0.00000</KURS3>
                <NUMPG>000</NUMPG>
                <STJAH>0000</STJAH>
                <Z_VKBUR>3941</Z_VKBUR>
                <E1FISEG SEGMENT="">
                <BUZEI>002</BUZEI>
                <AUGDT>00000000</AUGDT>
                <AUGCP>00000000</AUGCP>
                <BSCHL>50</BSCHL>
                <KOART>S</KOART>
                <SHKZG>H</SHKZG>
                <GSBER>5000</GSBER>
                <MWSKZ>4Z</MWSKZ>
                <DMBTR> 50.80</DMBTR>
                <DMBE2> 0.00</DMBE2>
                <DMBE3> 0.00</DMBE3>
                <WRBTR> 50.80</WRBTR>
                <KZBTR> 0.00</KZBTR>
                <PSWBT> 50.80</PSWBT>
                <PSWSL>EUR</PSWSL>
                <HWBAS> 0.00</HWBAS>
                <FWBAS> 0.00</FWBAS>
                <VALUT>20221230</VALUT>
                <ZUONR>20221230</ZUONR>
                <VORGN>SD00</VORGN>
                <FDTAG>00000000</FDTAG>
                <KOKRS>1000</KOKRS>
                <TXGRP>001</TXGRP>
                <AUFNR>003926212345</AUFNR>
                <POSN2>000000</POSN2>
                <BZDAT>00000000</BZDAT>
                <PERNR>00000000</PERNR>
                <HKONT>0000123456</HKONT>
                <ABPER>000000</ABPER>
                <MATNR>000000000000000123</MATNR>
                <WERKS>3900</WERKS>
                <MENGE> 1.000</MENGE>
                <MEINS>ST</MEINS>
                <ERFMG> 0.000</ERFMG>
                <BPMNG> 0.000</BPMNG>
                <EBELP>00000</EBELP>
                <ZEKKN>00</ZEKKN>
                <BUALT> 0.00</BUALT>
                <PRCTR>0003902345</PRCTR>
                <DABRZ>00000000</DABRZ>
                <FIPOS>3200</FIPOS>
                <AUFPL>0000000000</AUFPL>
                <APLZL>00000000</APLZL>
                <PROJK>00000000</PROJK>
                <PAOBJNR>0000000000</PAOBJNR>
                <FISTL>3900</FISTL>
                <GEBER>31001</GEBER>
                <ALTKT>0000003234</ALTKT>
                <KBLPOS>000</KBLPOS>
                <OBZEI>000</OBZEI>
                <TXDAT>00000000</TXDAT>
                <KURSR> 0.00000</KURSR>
                <GBETR> 0.00</GBETR>
                <E1FISE2 SEGMENT="">
                <SCTAX> 0.00</SCTAX>
                <STTAX> 0.00</STTAX>
                <KSTAR>0000323456</KSTAR>
                </E1FISE2>
                <E1FINBU SEGMENT="">
                <MWSTS> 0.00</MWSTS>
                <WMWST> 0.00</WMWST>
                <ZFBDT>00000000</ZFBDT>
                <ZBD1T> 0</ZBD1T>
                <ZBD2T> 0</ZBD2T>
                <ZBD3T> 0</ZBD3T>
                <ZBD1P> 0.000</ZBD1P>
                <ZBD2P> 0.000</ZBD2P>
                <SKFBT> 0.00</SKFBT>
                <SKNTO> 0.00</SKNTO>
                <WSKTO> 0.00</WSKTO>
                <REBZJ>0000</REBZJ>
                <REBZZ>000</REBZZ>
                <VRSDT>00000000</VRSDT>
                <MADAT>00000000</MADAT>
                <MANST>0</MANST>
                <QSSHB> 0.00</QSSHB>
                <QSFBT> 0.00</QSFBT>
                <KIDNO>39200123456</KIDNO>
                <SKNT2> 0.00</SKNT2>
                <SKNT3> 0.00</SKNT3>
                <PYAMT> 0.00</PYAMT>
                <ABSBT> 0.00</ABSBT>
                <DTWS1>00</DTWS1>
                <DTWS2>00</DTWS2>
                <DTWS3>00</DTWS3>
                <DTWS4>00</DTWS4>
                </E1FINBU>
                </E1FISEG>
                <E1FISEG SEGMENT="">
                <BUZEI>001</BUZEI>
                <AUGDT>20230120</AUGDT>
                <AUGCP>20230120</AUGCP>
                <AUGBL>0001001550</AUGBL>
                <BSCHL>01</BSCHL>
                <KOART>D</KOART>
                <SHKZG>S</SHKZG>
                <MWSKZ>4Z</MWSKZ>
                <DMBTR> 50.80</DMBTR>
                <DMBE2> 0.00</DMBE2>
                <DMBE3> 0.00</DMBE3>
                <WRBTR> 50.80</WRBTR>
                <KZBTR> 0.00</KZBTR>
                <PSWBT> 50.80</PSWBT>
                <PSWSL>EUR</PSWSL>
                <HWBAS> 0.00</HWBAS>
                <FWBAS> 0.00</FWBAS>
                <VALUT>20221230</VALUT>
                <SGTXT>Tekstiä. /123456 lk</SGTXT>
                <VORGN>SD00</VORGN>
                <FDLEV>F1</FDLEV>
                <FDGRP>A1</FDGRP>
                <FDTAG>20230117</FDTAG>
                <KOKRS>1000</KOKRS>
                <TXGRP>000</TXGRP>
                <VBELN>3920012345</VBELN>
                <POSN2>000000</POSN2>
                <BZDAT>00000000</BZDAT>
                <PERNR>00000000</PERNR>
                <XUMSW>X</XUMSW>
                <SAKNR>0000111111</SAKNR>
                <HKONT>0000111111</HKONT>
                <ABPER>000000</ABPER>
                <MENGE> 0.000</MENGE>
                <ERFMG> 0.000</ERFMG>
                <BPMNG> 0.000</BPMNG>
                <EBELP>00000</EBELP>
                <ZEKKN>00</ZEKKN>
                <BUALT> 0.00</BUALT>
                <DABRZ>00000000</DABRZ>
                <FIPOS>2999</FIPOS>
                <AUFPL>0000000000</AUFPL>
                <APLZL>00000000</APLZL>
                <PROJK>00000000</PROJK>
                <PAOBJNR>0000000000</PAOBJNR>
                <FISTL>3900</FISTL>
                <ALTKT>0000001550</ALTKT>
                <XREF2>5001234567</XREF2>
                <KBLPOS>000</KBLPOS>
                <OBZEI>000</OBZEI>
                <XREF3>12345678</XREF3>
                <TXDAT>00000000</TXDAT>
                <KURSR> 0.00000</KURSR>
                <GBETR> 0.00</GBETR>
                <E1FISE2 SEGMENT="">
                <SCTAX> 0.00</SCTAX>
                <STTAX> 0.00</STTAX>
                </E1FISE2>
                <E1FINBU SEGMENT="">
                <MWSTS> 0.00</MWSTS>
                <WMWST> 0.00</WMWST>
                <KUNNR>0012345678</KUNNR>
                <ZFBDT>20221230</ZFBDT>
                <ZTERM>N143</ZTERM>
                <ZBD1T> 17</ZBD1T>
                <ZBD2T> 0</ZBD2T>
                <ZBD3T> 0</ZBD3T>
                <ZBD1P> 0.000</ZBD1P>
                <ZBD2P> 0.000</ZBD2P>
                <SKFBT> 50.80</SKFBT>
                <SKNTO> 0.00</SKNTO>
                <WSKTO> 0.00</WSKTO>
                <REBZJ>0000</REBZJ>
                <REBZZ>000</REBZZ>
                <VRSDT>00000000</VRSDT>
                <MANSP>A</MANSP>
                <MABER>Z1</MABER>
                <MADAT>00000000</MADAT>
                <MANST>0</MANST>
                <QSSHB> 0.00</QSSHB>
                <QSFBT> 0.00</QSFBT>
                <KIDNO>39200149790</KIDNO>
                <SKNT2> 0.00</SKNT2>
                <SKNT3> 0.00</SKNT3>
                <PYAMT> 0.00</PYAMT>
                <ABSBT> 0.00</ABSBT>
                <DTWS1>00</DTWS1>
                <DTWS2>00</DTWS2>
                <DTWS3>00</DTWS3>
                <DTWS4>00</DTWS4>
                </E1FINBU>
                </E1FISEG>
                <E1FISET SEGMENT="">
                <BUZEI>001</BUZEI>
                <MWSKZ>4Z</MWSKZ>
                <HKONT>0000123456</HKONT>
                <TXGRP>001</TXGRP>
                <SHKZG>H</SHKZG>
                <HWBAS> 50.80</HWBAS>
                <FWBAS> 50.80</FWBAS>
                <HWSTE> 0.00</HWSTE>
                <FWSTE> 0.00</FWSTE>
                <KTOSL>MWS</KTOSL>
                <KNUMH>0000001234</KNUMH>
                <H2STE> 0.00</H2STE>
                <H3STE> 0.00</H3STE>
                <H2BAS> 0.00</H2BAS>
                <H3BAS> 0.00</H3BAS>
                <KSCHL>MWST</KSCHL>
                <STMDT>20230201</STMDT>
                <STMTI>094357</STMTI>
                <MLDDT>00000000</MLDDT>
                <KBETR> 0.00</KBETR>
                <STBKZ>2</STBKZ>
                <LWSTE> 0.00</LWSTE>
                <LWBAS> 0.00</LWBAS>
                <TXDAT>00000000</TXDAT>
                <TAXPS>000000</TAXPS>
                <TXMOD> 0</TXMOD>
                </E1FISET>
                </E1FIKPF>
                </IDOC>
                </FIDCCP02>
                """;
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_20250219-000123-456.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-tosite-file-contents", producerTemplate.send("direct:unmarshal-xml", ex));

        List<List<LinkedHashMap<String, Object>>> resReceipts = res.getMessage().getBody(List.class);
        assertEquals(1, resReceipts.size());
        List<LinkedHashMap<String, Object>> vals = resReceipts.get(0);
        assertEquals(2, vals.size());
        Map<String, Object> secReceipt = vals.get(1);

        assertEquals("001", secReceipt.get("BUZEI"));
        assertEquals("Tekstiä. /123456 lk", secReceipt.get("SGTXT"));
        assertEquals("0001001550", secReceipt.get("AUGBL"));
        assertEquals(" 50.80", secReceipt.get("HSL"));

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Tosite-palke", ex);
        String resBody = resCsv.getMessage().getBody(String.class);

        String[] splitData = resBody.split("\r\n");
        assertEquals("3900;0111123456;;2022;12;6S;20221230;20221230;20221230;;3920012345;;" +
                ";;;;" +
                "002;;0000123456;" +
                ";0003902345;;003926212345;" + //AUFNR
                "00000000;;;;" +
                "H;4Z;; 50.80;" +
                ";000000000000000123;00000;;", splitData[0]);
    }

    @Test
    void tositeCsvShouldAlwaysContainSameAmountOfCsvFields() {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = "<FIDCCP02><IDOC><E1FIKPF><BUKRS>3900</BUKRS><E1FISEG><TEST>1</TEST></E1FISEG></E1FIKPF></IDOC></FIDCCP02>";
        ex.getMessage().setBody(xmlIn);
        Exchange res = producerTemplate.send("direct:process-tosite-file-contents", producerTemplate.send("direct:unmarshal-xml", ex));
        ex.getMessage().setBody(res.getMessage().getBody(List.class).getFirst());
        Exchange emptyCsv = producerTemplate.send("direct:marshal-headerless-csv-Tosite-palke", ex);
        assertEquals(tositeRoute.createCsvHeader().length, emptyCsv.getMessage().getBody(String.class).split(";").length);
    }

    public Map<String, Object> createReceipt(String fileName, String gjahr, String poper, String bukrs, String belnr) {
        return Map.of("fileName", fileName,"GJAHR", gjahr, "POPER", poper, "BUKRS", bukrs, "BELNR", belnr);
    }

    @Test
    void exceptionThrownWhenReadingFileInTest() {
        Map<String, Object> headers = Map.of(Exchange.FILE_NAME, "ID022_FI_TOSITE_OUT_20250219-000123-456.xml");

        producerTemplate.sendBodyAndHeaders("file:in", """
                <FIDCCP02>
                <IDOC>
                <E1FIKPF>
                <BUKRS>3900</BUKRS>
                <BELNR>0111123456</BELNR>
                <GJAHR>2025</GJAHR>
                <BLART>6S</BLART>
                <BLDAT>20221230</BLDAT>
                <BUDAT>20221230</BUDAT>
                <MONAT>12</MONAT>
                <CPUDT>20221230</CPUDT>
                <XBLNR>3920012345</XBLNR>
                <E1FISEG SEGMENT="">
                <BUZEI>001</BUZEI>
                <AUGDT>20230120</AUGDT>
                <AUGCP>20230120</AUGCP>
                </E1FISEG>
                </E1FIKPF>
                </IDOC>
                </FIDCCP02>
                """, headers);

        // mock file out

    }


}