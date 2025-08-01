package fi.hel.integration.sapfpm.routes.sapkumppani;

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
public class PartInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:direct:enrich-and-send-file-to-azure-palke-Part")
    private MockEndpoint mockUploadBlobToAzurePalkeAnyFileOutPart;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "Part-palke-process", b -> {
            b.interceptSendToEndpoint("direct:enrich-and-send-file-to-azure-palke").to(mockUploadBlobToAzurePalkeAnyFileOutPart.getEndpointUri())
                .skipSendToOriginalEndpoint();
        });
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
        Exchange resCsv = producerTemplate.send("direct:marshal-csv-Part-palke", ex);

        assertEquals("RCOMP;NAME1\r\n000001;Valtio ( virastot ja laitokse\r\n" +
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
        Exchange resCsv = producerTemplate.send("direct:marshal-csv-Part-palke", ex);

        assertEquals("RCOMP;NAME1\r\n01;\"\"\"Test \"\" Test\"\r\n02;\"Test ; Test\"\r\n", resCsv.getMessage().getBody(String.class));
    }

    // should only send the latest file (determined by file name) to azure and ignore the file if it's earlier than the one sent last
    @Test
    void shouldOnlySendTheLatestFileToAzure() throws Exception {
        URL firstFileUrl = getClass().getResource("/PART_OUT_test_1.xml");
        URL secFileUrl = getClass().getResource("/PART_OUT_test_2.xml");
        URL thirdFileUrl = getClass().getResource("/PART_OUT_test_3.xml");
        assertNotNull(firstFileUrl);
        assertNotNull(secFileUrl);
        assertNotNull(thirdFileUrl);
        Path firstTestFilePath = Paths.get(firstFileUrl.toURI());
        Path secTestFilePath = Paths.get(secFileUrl.toURI());
        Path thirdTestFilePath = Paths.get(thirdFileUrl.toURI());
        Path inFirstTestFilePath = Paths.get("in/palke/PART_OUT_test_1.xml");
        Path inSecTestFilePath = Paths.get("in/palke/PART_OUT_test_2.xml");
        Path inThirdTestFilePath = Paths.get("in/palke/PART_OUT_test_3.xml");

        mockUploadBlobToAzurePalkeAnyFileOutPart.whenExchangeReceived(1, e -> {
            String fileContent = e.getMessage().getBody(String.class);
            List<String> lines = fileContent.lines().toList();
            Optional<String> foundFirstPart = lines.stream().filter(l -> l.equals("000001;First")).findFirst();
            assertTrue(foundFirstPart.isPresent());
            Optional<String> foundSecPart = lines.stream().filter(l -> l.equals("000002;Sec")).findFirst();
            assertTrue(foundSecPart.isPresent());
            assertEquals(3, lines.size());
        });
        try {
            Files.copy(secTestFilePath, inSecTestFilePath); // second added first
            Files.copy(firstTestFilePath, inFirstTestFilePath); // should be ignored
        } catch (FileAlreadyExistsException existsException) { /* file already copied, ok */ }
        mockUploadBlobToAzurePalkeAnyFileOutPart.expectedMessageCount(1);
        mockUploadBlobToAzurePalkeAnyFileOutPart.assertIsSatisfied();

        mockUploadBlobToAzurePalkeAnyFileOutPart.reset();

        mockUploadBlobToAzurePalkeAnyFileOutPart.whenExchangeReceived(1, e -> {
            String fileContent = e.getMessage().getBody(String.class);
            List<String> lines = fileContent.lines().toList();
            Optional<String> foundFirstPart = lines.stream().filter(l -> l.equals("000001;First")).findFirst();
            assertTrue(foundFirstPart.isPresent());
            Optional<String> foundSecPart = lines.stream().filter(l -> l.equals("000002;Sec")).findFirst();
            assertTrue(foundSecPart.isPresent());
            Optional<String> thirdSecPart = lines.stream().filter(l -> l.equals("000003;Third")).findFirst();
            assertTrue(thirdSecPart.isPresent());
            assertEquals(4, lines.size());
        });
        try {
            Files.copy(thirdTestFilePath, inThirdTestFilePath);
        } catch (FileAlreadyExistsException existsException) { /* file already copied, ok */ }
        mockUploadBlobToAzurePalkeAnyFileOutPart.expectedMessageCount(1);
        mockUploadBlobToAzurePalkeAnyFileOutPart.assertIsSatisfied();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        Files.deleteIfExists(Paths.get("in/palke/PART_OUT_test_1.xml"));
        Files.deleteIfExists(Paths.get("in/palke/PART_OUT_test_2.xml"));
        Files.deleteIfExists(Paths.get("in/palke/PART_OUT_test_3.xml"));
    }
}
