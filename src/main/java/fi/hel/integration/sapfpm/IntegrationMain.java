package fi.hel.integration.sapfpm;

import org.apache.camel.main.Main;

public final class IntegrationMain {

    private IntegrationMain() {}

	public static void main(String[] args) throws Exception {
        Main main = new Main();
        // keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }

}
