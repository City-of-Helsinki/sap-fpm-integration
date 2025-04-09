package fi.hel.integration.sapfpm.routes.sapactual;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.component.jdbc.JdbcConstants;

import java.util.Map;

public class FITositeDB extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        /*from("direct:write-tosite-to-db")
            .process(e -> {
                //String fileName = e.getMessage().getHeader(FileConstants.FILE_NAME, String.class);
             //   Map<String, String> vals = e.getMessage().getBody(Map.class);
            })
            .setHeader(JdbcConstants.JDBC_PARAMETERS, body())
            .setBody(constant(
                    "INSERT INTO TOSITE (fileName, BUKRS, BELNR, GJAHR, MONAT) VALUES " +
                          "(:?fileName, :?BUKRS, :?BELNR, :?GJAHR, :?MONAT)"))
            .to("jdbc:sapactual?useHeadersAsParameters=true");*/
    }
}
