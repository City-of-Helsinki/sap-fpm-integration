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

    @Test
    void shouldParse_CoTosite_OUT() throws Exception {
        CamelContext ctx = producerTemplate.getCamelContext();
        Exchange ex = new DefaultExchange(ctx);

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
        ex.getMessage().setHeader("CamelFileName", "ID016_CO_TOSITE_OUT_20250219-000123-456.xml");
        ex.getMessage().setBody(xmlIn);

        Exchange res = producerTemplate.send("direct:process-cotosite-file-contents", producerTemplate.send("direct:unmarshal-xml", ex));

        List<List<LinkedHashMap<String, Object>>> resReceipts = res.getMessage().getBody(List.class);
        assertEquals(1, resReceipts.size());
        List<LinkedHashMap<String, Object>> vals = resReceipts.getFirst();
        assertEquals(1, vals.size());
        Map<String, Object> firstReceipt = vals.getFirst();

        assertEquals("001", firstReceipt.get("BUZEI"));
        assertEquals("953210000220250215", firstReceipt.get("SGTXT"));

        ex.getMessage().setBody(vals);
        Exchange resCsv = producerTemplate.send("direct:marshal-headerless-csv-Cotosite-palke", ex);
        String resBody = resCsv.getMessage().getBody(String.class);

        String[] splitData = resBody.split("\r\n");
        assertEquals("0011000001;20250215;20250215;20250216;38;9500381852;000;ZBKPF;5000099237;001;002;2.72-;OR009532100002;Sis. til.;009532100001;0009532100;2025;0000300040;H;9500;953210000220250215;", splitData[0]);

    }

}