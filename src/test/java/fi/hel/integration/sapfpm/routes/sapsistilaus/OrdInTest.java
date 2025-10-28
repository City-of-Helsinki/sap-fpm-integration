package fi.hel.integration.sapfpm.routes.sapsistilaus;

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
public class OrdInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:direct:enrich-and-send-file-to-azure-kasko")
    private MockEndpoint mockEnrichFileKasko;

    @EndpointInject("mock:direct:any-file-out")
    private MockEndpoint mockUploadBlobToAzureKaskoAnyFileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "append-wip-main-Ord-kasko", b -> {
            b.interceptSendToEndpoint("direct:enrich-and-send-file-to-azure-kasko").to(mockEnrichFileKasko.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "enrichAndSendToAzure-kasko", b -> {
            b.interceptSendToEndpoint("direct:any-file-out").to(mockUploadBlobToAzureKaskoAnyFileOut.getEndpointUri())
                .skipSendToOriginalEndpoint();
        });
    }

    @Test
    void shouldParse_ORD_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        String AUFNR1 = "12345678",
                KTEXT1 = "KTXT KTXT öä",
                AUFNR2 = "2345678",
                KTEXT2 = "Another ktext ",
                expectedKTEXT2 = "\"Another ktext \"";

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
    <AUFNR>""" + AUFNR1 + """
    </AUFNR>
    <KTEXT>""" + KTEXT1 + """
    </KTEXT>
    <STTXT>VAPA</STTXT>
    <AUTYP>01</AUTYP>
    </ZHKI_TARSISTILAUKSET>
    <ZHKI_TARSISTILAUKSET SEGMENT="1">
    <BUKRS>3900</BUKRS>
    <AUART>3901</AUART>
    <AUFNR>""" + AUFNR2 + """
    </AUFNR>
    <KTEXT>""" + KTEXT2 + """
    </KTEXT>
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

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Ord-palke", ex);
        assertEquals(
                "3900;3901;" + AUFNR1 + ";" + KTEXT1 + ";VAPA\r\n" +
                        "3900;3901;" + AUFNR2 + ";" + expectedKTEXT2 + ";VAPA\r\n",
                resCsv.getMessage().getBody(String.class));
    }


    // first a two file batch is sent, exception thrown during sending of the csv, uploaded csv should not have repeated content
    // then a new 1 file batch is sent, csv should have all content and no repetition
    @Test
    void shouldNotAppendTwiceToMainFileIfExceptionThrownDuringUpload() throws Exception {
        URL firstFileUrl = getClass().getResource("/ORD_OUT_once_test_1.xml");
        URL secFileUrl = getClass().getResource("/ORD_OUT_once_test_2.xml");
        URL thirdFileUrl = getClass().getResource("/ORD_OUT_once_test_3.xml");
        assertNotNull(firstFileUrl);
        assertNotNull(secFileUrl);
        assertNotNull(thirdFileUrl);
        Path firstTestFilePath = Paths.get(firstFileUrl.toURI());
        Path secTestFilePath = Paths.get(secFileUrl.toURI());
        Path thirdTestFilePath = Paths.get(thirdFileUrl.toURI());
        Path inFirstTestFilePath = Paths.get("in/kasko/ORD_OUT_once_test_1.xml");
        Path inSecTestFilePath = Paths.get("in/kasko/ORD_OUT_once_test_2.xml");
        Path inThirdTestFilePath = Paths.get("in/kasko/ORD_OUT_once_test_3.xml");

        mockEnrichFileKasko.whenExchangeReceived(1, e -> {
            e.setException(new Exception("Exception during upload!"));
        });
        mockUploadBlobToAzureKaskoAnyFileOut.whenExchangeReceived(1, e -> {
            String fileContent = e.getMessage().getBody(String.class);
            List<String> lines = fileContent.lines().toList();
            assertEquals(3, lines.size());
            // file should be ordered newest first, oldest last
            assertEquals("3900;3901;00000Y;Second;VAPA", lines.get(1));
            assertEquals("3900;3901;00000X;Bla;VAPA", lines.get(2));
        });
        try {
            Files.copy(firstTestFilePath, inFirstTestFilePath);
            Files.copy(secTestFilePath, inSecTestFilePath);
        } catch (FileAlreadyExistsException existsException) { /* file already copied, ok */ }
        mockEnrichFileKasko.expectedMessageCount(2);
        mockUploadBlobToAzureKaskoAnyFileOut.expectedMessageCount(1);
        mockEnrichFileKasko.assertIsSatisfied();
        mockUploadBlobToAzureKaskoAnyFileOut.assertIsSatisfied();

        mockEnrichFileKasko.reset();
        mockUploadBlobToAzureKaskoAnyFileOut.reset();

        mockUploadBlobToAzureKaskoAnyFileOut.whenExchangeReceived(1, e -> {
            String fileContent = e.getMessage().getBody(String.class);
            List<String> lines = fileContent.lines().toList();
            assertEquals(4, lines.size());
            // file should be ordered newest first, oldest last
            assertEquals("3900;3901;00000Z;Third;VAPA", lines.get(1));
            assertEquals("3900;3901;00000Y;Second;VAPA", lines.get(2));
            assertEquals("3900;3901;00000X;Bla;VAPA", lines.get(3));
        });
        try {
            Files.copy(thirdTestFilePath, inThirdTestFilePath);
        } catch (FileAlreadyExistsException existsException) { /* file already copied, ok */ }
        mockUploadBlobToAzureKaskoAnyFileOut.expectedMessageCount(1);
        mockUploadBlobToAzureKaskoAnyFileOut.assertIsSatisfied();
        mockEnrichFileKasko.expectedMessageCount(1);
        mockEnrichFileKasko.assertIsSatisfied();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        Files.deleteIfExists(Paths.get("in/kasko/ORD_OUT_once_test_1.xml"));
        Files.deleteIfExists(Paths.get("in/kasko/ORD_OUT_once_test_2.xml"));
        Files.deleteIfExists(Paths.get("in/kasko/ORD_OUT_once_test_3.xml"));
    }

}
