package fi.hel.integration.sapfpm.routes.sapsisainenlaskenta;


import io.quarkus.test.junit.QuarkusTest;
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
public class CoTositeInTest {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:cotosite-out")
    MockEndpoint mockFileOut;

    @BeforeEach
    public void beforeEach() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        AdviceWith.adviceWith(ctx, "coTositeAzureOut", b -> {
            b.weaveByToUri("direct:any-file-out").replace().to(mockFileOut.getEndpointUri());
        });
    }

    @Test
    void shouldParse_CoTosite_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

        String YEAR_MONTH = "202212";

        mockFileOut.whenAnyExchangeReceived(e -> {
                    InputStreamCache c = e.getMessage().getBody(InputStreamCache.class);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    c.writeTo(out);
                    out.close();
                    String data = out.toString();
                    String expCsvHeader = "BELNR;BLDAT;BUDAT;CPUDT;BLART;" +
                            "REFBN;VERSN;AWTYP;AWORG;" +
                            "BUZEI;PERIO;WOGBTR;OBJNR;OBJ_TYPE;TYPE_NR;" +
                            "PRCTR;GJAHR;KSTAR;BEKNZ;BUKRS;SGTXT;FKBER";
                    String[] splitData = data.split("\r\n");
                    assertEquals(expCsvHeader, splitData[0]);
                    assertEquals("0011000001;20250215;20250215;20250216;38;9500381852;000;ZBKPF;5000099237;001;002;2.72-;OR009532100002;Sis. til.;009532100001;0009532100;2025;0000300040;H;9500;953210000220250215;", splitData[1]);
                });

        String xmlIn = """
<Atos_CODCMT xmlns:prx="urn:sap.com:proxy:P10:/1SAI/TASDA8F90C87:740">
   <ZCODCMT>
   <KOKRS>2000</KOKRS>
   <BELNR>0011000001</BELNR>
   <BLDAT>20250215</BLDAT>
   <BUDAT>20250215</BUDAT>
   <CPUDT>20250216</CPUDT>
   <BLART>38</BLART>
   <REFBN>9500381852</REFBN>
   <VERSN>000</VERSN>
   <AWTYP>ZBKPF</AWTYP>
   <AWORG>5000099237</AWORG>
   <BUZEI>
   <BUZEI>001</BUZEI>
   <PERIO>002</PERIO>
   <WOGBTR>2.72-</WOGBTR>
   <OBJNR>OR009532100002</OBJNR>
   <OBJ_TYPE>Sis. til.</OBJ_TYPE>
   <TYPE_NR>009532100001</TYPE_NR>
   <PRCTR>0009532100</PRCTR>
   <GJAHR>2025</GJAHR>
   <KSTAR>0000300040</KSTAR>
   <TWAER>EUR</TWAER>
   <BEKNZ>H</BEKNZ>
   <BUKRS>9500</BUKRS>
   <WTGBTR>2.72-</WTGBTR>
   <MEGBTR>0.000 </MEGBTR>
   <MBGBTR>0.000 </MBGBTR>
   <WRTTP>04</WRTTP>
   <VERSN>000</VERSN>
   <VRGNG>COIN</VRGNG>
   <SGTXT>953210000220250215</SGTXT>
   <REFBZ>001</REFBZ>
   <ZLENR>001</ZLENR>
   <PAOBJNR>0000000000</PAOBJNR>
   <GSBER>9900</GSBER>
   </BUZEI>
   </ZCODCMT>
</Atos_CODCMT>""";
        ex.getMessage().setHeader("CamelFileName", "ID022_FI_TOSITE_OUT_20250219-000123-456.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:unmarshal-and-process-co-tosite", ex);
        List<LinkedHashMap<String, Object>> vals = res.getMessage().getBody(List.class);
        producerTemplate.sendBody("direct:co-tosite-csv-out", vals);

        mockFileOut.expectedMessageCount(1);
        mockFileOut.assertIsSatisfied();
    }

}