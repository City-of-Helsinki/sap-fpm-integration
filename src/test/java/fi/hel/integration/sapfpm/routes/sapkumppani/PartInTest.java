package fi.hel.integration.sapfpm.routes.sapkumppani;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.apache.camel.builder.Builder.exchangeProperty;
import static org.apache.camel.builder.Builder.header;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PartInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:enrich-and-send-file-to-azure-kasko")
    private MockEndpoint mockEnrichFileKasko;

    @EndpointInject("mock:direct:any-file-out")
    private MockEndpoint mockUploadBlobToAzureKaskoAnyFileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "append-wip-main-Part-kasko", b -> {
            b.interceptSendToEndpoint("direct:enrich-and-send-file-to-azure-kasko").to(mockEnrichFileKasko.getEndpointUri());
        });
        AdviceWith.adviceWith(ctx, "enrichAndSendToAzure-kasko", b -> {
            b.interceptSendToEndpoint("direct:any-file-out").to(mockUploadBlobToAzureKaskoAnyFileOut.getEndpointUri())
                .skipSendToOriginalEndpoint();
        });
    }

    @AfterAll
    public static void afterAll() throws Exception {
        Files.deleteIfExists(Paths.get("in/kasko/PART_OUT_once_test_1.xml"));
        Files.deleteIfExists(Paths.get("in/kasko/PART_OUT_once_test_2.xml"));
        Files.deleteIfExists(Paths.get("in/kasko/PART_OUT_once_test_3.xml"));
    }

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

    @Test
    void csvFormatShouldHandleQuotes() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String xmlIn = """
<Kumppanit><Kumppaniyhtiö><RCOMP>01</RCOMP>
   <NAME1>"Test " Test</NAME1>
</Kumppaniyhtiö>
<Kumppaniyhtiö><RCOMP>02</RCOMP>
   <NAME1>Test ; Test</NAME1>
</Kumppaniyhtiö></Kumppanit>
    """;
        ex.getMessage().setHeader("CamelFileName", "PART_OUT_167_SOTE20241023-190022.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-part", producerTemplate.send("direct:unmarshal-xml", ex));

        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        assertEquals(2, vals.size());

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Part-palke", ex);

        assertEquals("01;\"\"\"Test \"\" Test\"\r\n02;\"Test ; Test\"\r\n", resCsv.getMessage().getBody(String.class));
    }


    // first a two file batch is sent, exception thrown during sending of the csv, uploaded csv should not have repeated content
    // then a new 1 file batch is sent, csv should have all content and no repetition
    @Test
    void shouldNotAppendTwiceToMainFileIfExceptionThrownDuringUpload() throws Exception {
        URL firstFileUrl = getClass().getResource("/PART_OUT_once_test_1.xml");
        URL secFileUrl = getClass().getResource("/PART_OUT_once_test_2.xml");
        URL thirdFileUrl = getClass().getResource("/PART_OUT_once_test_3.xml");
        assertNotNull(firstFileUrl);
        assertNotNull(secFileUrl);
        assertNotNull(thirdFileUrl);
        Path firstTestFilePath = Paths.get(firstFileUrl.toURI());
        Path secTestFilePath = Paths.get(secFileUrl.toURI());
        Path thirdTestFilePath = Paths.get(thirdFileUrl.toURI());
        Path inFirstTestFilePath = Paths.get("in/kasko/PART_OUT_once_test_1.xml");
        Path inSecTestFilePath = Paths.get("in/kasko/PART_OUT_once_test_2.xml");
        Path inThirdTestFilePath = Paths.get("in/kasko/PART_OUT_once_test_3.xml");

        mockEnrichFileKasko.whenExchangeReceived(1, e -> {
            e.setException(new Exception("Exception during upload!"));
        });
        mockUploadBlobToAzureKaskoAnyFileOut.whenExchangeReceived(1, e -> {
            String fileContent = e.getMessage().getBody(String.class);
            System.out.println("SMERP: " + fileContent);
            List<String> lines = fileContent.lines().toList();
            Optional<String> foundFirstPart = lines.stream().filter(l -> l.equals("00000X;Bla")).findFirst();
            assertTrue(foundFirstPart.isPresent());
            Optional<String> foundSecPart = lines.stream().filter(l -> l.equals("00000Y;Second")).findFirst();
            assertTrue(foundSecPart.isPresent());
            assertEquals(3, lines.size());
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
            System.out.println(fileContent);
            Optional<String> foundFirstPart = lines.stream().filter(l -> l.equals("00000X;Bla")).findFirst();
            assertTrue(foundFirstPart.isPresent());
            Optional<String> foundSecPart = lines.stream().filter(l -> l.equals("00000Y;Second")).findFirst();
            assertTrue(foundSecPart.isPresent());
            Optional<String> thirdSecPart = lines.stream().filter(l -> l.equals("00000Z;Third")).findFirst();
            assertTrue(thirdSecPart.isPresent());
            assertEquals(4, lines.size());
        });
        try {
            Files.copy(thirdTestFilePath, inThirdTestFilePath);
        } catch (FileAlreadyExistsException existsException) { /* file already copied, ok */ }
        mockUploadBlobToAzureKaskoAnyFileOut.expectedMessageCount(1);
        mockUploadBlobToAzureKaskoAnyFileOut.assertIsSatisfied();
        mockEnrichFileKasko.expectedMessageCount(1);
        mockEnrichFileKasko.assertIsSatisfied();
    }

}
