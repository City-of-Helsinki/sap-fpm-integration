package fi.hel.integration.sapfpm;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TestDbHelper {
    @Inject
    ProducerTemplate producerTemplate;

    public void dropTables() {
        String dropTables = Stream.of("TOSITE", "TOSITERIVI", "S4TOSITERIVI", "S4TOSITESAPFILE", "TOSITESAPFILE").map("DROP TABLE IF EXISTS %s"::formatted).collect(Collectors.joining(";"));
        producerTemplate.sendBody("jdbc:sapactual", dropTables);
    }
}
