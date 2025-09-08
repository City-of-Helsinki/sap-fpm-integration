package fi.hel.integration.sapfpm.routes.sapactual;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class S4TositeInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    S4FITositeInRouteBuilder s4tositeRoute;

    String testTositeFileName = "ID023_FI_TOSITE_OUT_20250809-000123-999.xml";

    @AfterEach
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get("in/s4/" + testTositeFileName));
    }

    @Test
    void shouldParse_Tosite_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = """
                <ROWS>
                <ROW>
                <INTERFACEID>023</INTERFACEID>
                <KOKRS>1000</KOKRS>
                <BUKRS>1400</BUKRS>
                <BELNR>0200000005</BELNR>
                <CO_BELNR>A000000000</CO_BELNR>
                <GJAHR>2025</GJAHR>
                <POPER>007</POPER>
                <BLART>Y4</BLART>
                <BLDAT>20250731</BLDAT>
                <BUDAT>20250731</BUDAT>
                <CPUDT>20250819</CPUDT>
                <TCODE/>
                <XBLNR/>
                <XREF1_HD/>
                <XREF2_HD/>
                <ZZXREF1/>
                <ZZXREF2/>
                <ZZXREF3/>
                <AWREF>0111123456</AWREF>
                <USNAM>AICOSYS</USNAM>
                <AWTYP>BKPFF</AWTYP>
                <VORGN>RFBU</VORGN>
                <KUNNR/>
                <NAME1/>
                <KDAUF/>
                <LIFNR/>
                <LIFNR_NAME1/>
                <IDNUMBER/>
                <EBELN/>
                <Attachment/>
                <DOCLN>000001</DOCLN>
                <BUZEI>001</BUZEI>
                <CO_BUZEI>CO_BUZEI_HERE</CO_BUZEI>
                <VRGNG>COIN</VRGNG>
                <RMVCT/>
                <OBJNR>OR000000000001</OBJNR>
                <ACCASTY>OR</ACCASTY>
                <ACCASTY_TXT>Tilaus</ACCASTY_TXT>
                <ARBID>00000000</ARBID>
                <ARBID_TXT/>
                <RACCT>0000123456</RACCT>
                <RCNTR/>
                <PRCTR>0001400000</PRCTR>
                <RFAREA/>
                <AUFNR>001400000010</AUFNR>
                <PRONR/>
                <PRONR_TXT/>
                <PS_PRJ_PNR>00000000</PS_PRJ_PNR>
                <PS_PSPID/>
                <PS_PSP_PNR>00000000</PS_PSP_PNR>
                <PS_POSID/>
                <NPLNR/>
                <NPLNR_VORGN/>
                <RASSC/>
                <SEGMENT>S12</SEGMENT>
                <LSTAR/>
                <SGTXT> sg txt 1</SGTXT>
                <RHCUR>EUR</RHCUR>
                <DRCRK>H</DRCRK>
                <MWSKZ>4Z</MWSKZ>
                <VAT_PERCENT>0.00%</VAT_PERCENT>
                <HSL>-1234.56</HSL>
                <PBUKRS/>
                <SFAREA/>
                <PPRCTR/>
                <KDPOS>000000</KDPOS>
                <MATNR>000000000000000123</MATNR>
                <VKORG/>
                <EBELP>00000</EBELP>
                <PERNR>00000000</PERNR>
                <WERKS/>
                <RUNIT/>
                <MSL>0.000</MSL>
                <BSCHL>50</BSCHL>
                <KOART>S</KOART>
                <NETDT>00000000</NETDT>
                <PAOBJNR/>
                <ANBWA/>
                <ANLN1/>
                <ANLN2/>
                <ANLKL/>
                <KTOGR/>
                <AUGDT>00000000</AUGDT>
                <AUGBL/>
                <PAUFNR/>
                <PPS_PSP_PNR>00000000</PPS_PSP_PNR>
                <PPS_PSPID/>
                <PNPLNR/>
                <PNPLNR_VORGN/>
                <PKDAUF/>
                <PLSTAR/>
                <ZZVERTN/>
                <LAST_CHANGE_DATETIME>202508190508</LAST_CHANGE_DATETIME>
                <TIMESTAMP>202508190508</TIMESTAMP>
                </ROW>
                <ROW>
                <INTERFACEID>023</INTERFACEID>
                <KOKRS>1000</KOKRS>
                <BUKRS>3900</BUKRS>
                <BELNR>0211123456</BELNR>
                <CO_BELNR/>
                <GJAHR>2025</GJAHR>
                <POPER>007</POPER>
                <BLART>Y4</BLART>
                <BLDAT>20250731</BLDAT>
                <BUDAT>20250731</BUDAT>
                <CPUDT>20250819</CPUDT>
                <TCODE/>
                <XBLNR/>
                <ZZXREF3/>
                <AWREF>0000123456</AWREF>
                <USNAM>AICOSYS</USNAM>
                <AWTYP>BKPFF</AWTYP>
                <VORGN>RFBU</VORGN>
                <KUNNR/>
                <LIFNR/>
                <LIFNR_NAME1/>
                <IDNUMBER/>
                <EBELN/>
                <Attachment/>
                <DOCLN>000002</DOCLN>
                <BUZEI>002</BUZEI>
                <CO_BUZEI>000</CO_BUZEI>
                <ARBID>00000000</ARBID>
                <ARBID_TXT/>
                <RACCT>0000155900</RACCT>
                <RCNTR/>
                <PRCTR/>
                <RFAREA/>
                <AUFNR/>
                <PS_PRJ_PNR>00000000</PS_PRJ_PNR>
                <PS_PSPID/>
                <PS_PSP_PNR>00000000</PS_PSP_PNR>
                <PS_POSID/>
                <NPLNR/>
                <NPLNR_VORGN/>
                <RASSC/>
                <SEGMENT>S12</SEGMENT>
                <LSTAR/>
                <SGTXT>Tekstiä. /123456 lk</SGTXT>
                <RHCUR>EUR</RHCUR>
                <DRCRK>S</DRCRK>
                <MWSKZ/>
                <VAT_PERCENT/>
                <HSL>1234.56</HSL>
                <PBUKRS/>
                <SFAREA/>
                <PPRCTR/>
                <KDPOS>000000</KDPOS>
                <MATNR/>
                <VKORG/>
                <EBELP>00000</EBELP>
                <PERNR>00000000</PERNR>
                <WERKS/>
                <RUNIT/>
                <MSL>0.000</MSL>
                <BSCHL>40</BSCHL>
                <KOART>S</KOART>
                <NETDT>00000000</NETDT>
                <AUGDT>00000000</AUGDT>
                <AUGBL/>
                <PAUFNR/>
                <PPS_PSP_PNR>00000000</PPS_PSP_PNR>
                <PPS_PSPID/>
                <PNPLNR/>
                <PNPLNR_VORGN/>
                <PKDAUF/>
                <PLSTAR/>
                <ZZVERTN/>
                <LAST_CHANGE_DATETIME>202508190508</LAST_CHANGE_DATETIME>
                <TIMESTAMP>202508190508</TIMESTAMP>
                </ROW>
                </ROWS>
                """;
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_20250902-000123-456.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-s4-tosite-file-contents", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(2, vals.size());
        Map<String, Object> secReceipt = vals.get(1);

        assertEquals("002", secReceipt.get("BUZEI"));
        assertEquals("Tekstiä. /123456 lk", secReceipt.get("SGTXT"));
        assertEquals("", secReceipt.get("AUGBL")); // TODO: seems to be empty?
        assertEquals("1234.56", secReceipt.get("HSL"));
        assertEquals("BKPFF", secReceipt.get("AWTYP"));

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-s4-Tosite-palke", ex);
        String resBody = resCsv.getMessage().getBody(String.class);

        String[] splitData = resBody.split("\r\n");
        assertEquals("1400;0200000005;A000000000;2025;007;Y4;20250731;20250731;20250819;;;;" +
                ";;;;" +
                "001;CO_BUZEI_HERE;0000123456;" +
                ";0001400000;;001400000010;" + //AUFNR
                ";;" + // PS_PSPID;RASSC
                "S12;\" sg txt 1\";H;4Z;0.00%;-1234.56;" + // SEGMENT;SGTXT;DRCRK;VAT_PERCENT;
                ";000000000000000123;00000;202508190508;;BKPFF", splitData[0]);
    }

    @Test
    void s4TositeCsvShouldAlwaysContainSameAmountOfCsvFields() {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = "<ROWS><ROW><BUKRS>3900</BUKRS><TEST>1</TEST></ROW></ROWS>";
        ex.getMessage().setBody(xmlIn);
        Exchange res = producerTemplate.send("direct:process-s4-tosite-file-contents", producerTemplate.send("direct:unmarshal-xml", ex));
        ex.getMessage().setBody(res.getMessage().getBody(List.class).getFirst());
        Exchange emptyCsv = producerTemplate.send("direct:marshal-headerless-csv-s4-Tosite-palke", ex);
        assertEquals(s4tositeRoute.createCsvHeader().length, emptyCsv.getMessage().getBody(String.class).split(";").length);
    }

    @Test
    void exceptionThrownWhenReadingFileInTest() {
        Map<String, Object> headers = Map.of(Exchange.FILE_NAME, testTositeFileName);

        producerTemplate.sendBodyAndHeaders("file:in/s4", """
                <ROWS>
                <ROW>
                <BUKRS>3900</BUKRS>
                <BELNR>0111123456</BELNR>
                <GJAHR>2025</GJAHR>
                <BLART>6S</BLART>
                <BLDAT>20221230</BLDAT>
                <BUDAT>20221230</BUDAT>
                <POPER>12</POPER>
                <CPUDT>20221230</CPUDT>
                <XBLNR>3920012345</XBLNR>
                <BUZEI>001</BUZEI>
                <AUGDT>20230120</AUGDT>
                <AUGCP>20230120</AUGCP>
                </ROW>
                </ROWS>
                """, headers);
    }

}