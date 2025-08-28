package fi.hel.integration.sapfpm;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.jboss.logging.Logger;

import java.sql.*;
import java.util.Map;

@ApplicationScoped
public class IdempotentRepositoryProvider {

    @DataSource("sapactual")
    AgroalDataSource ds;

    @Inject
    Logger log;

    @Named("perustiedot_kasko_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository perustiedotKaskoIdempotentRepository() {
        return createIdempotentRepository();
    }

    @Named("perustiedot_palke_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository perustiedotPalkeIdempotentRepository() {
        return createIdempotentRepository();
    }

    @Named("perustiedot_sotepe_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository perustiedotSotepeIdempotentRepository() {
        return createIdempotentRepository();
    }

    @Named("co_toteumat_palke_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository coToteumatPalkeIdempotentRepository() {
        return queryFileNamesFromDbAndAddToIdempotentRepository("palke", "COTOSITESAPFILE");
    }

    @Named("toteumat_kasko_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatKaskoIdempotentRepository() {
        return queryFileNamesFromDbAndAddToIdempotentRepository("kasko", "TOSITESAPFILE");
    }

    @Named("toteumat_palke_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatPalkeIdempotentRepository() {
        return queryFileNamesFromDbAndAddToIdempotentRepository("palke", "TOSITESAPFILE");
    }

    @Named("toteumat_sotepe_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatSotepeIdempotentRepository() {
        return queryFileNamesFromDbAndAddToIdempotentRepository("sotepe", "TOSITESAPFILE");
    }

    /*@Named("s4_toteumat_sotepe_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository s4ToteumatSotepeIdempotentRepository() {
        return queryFileNamesFromDbAndAddToIdempotentRepository("sotepe", "S4TOSITESAPFILE");
    }*/

    public IdempotentRepository createIdempotentRepository() {
        MemoryIdempotentRepository memoryIdempotentRepo = new MemoryIdempotentRepository();
        memoryIdempotentRepo.setCacheSize(50000);
        return memoryIdempotentRepo;
    }

    public IdempotentRepository queryFileNamesFromDbAndAddToIdempotentRepository(String toimiala, String tableName) {
        Map<String, Object> repoCache = LRUCacheFactory.newLRUCache(50000);
        try {
            log.info("ds login timeout: " + ds.getLoginTimeout());
            log.info("ds is healthy: " + ds.isHealthy(true));
        } catch (SQLException sqlE) {
            log.error("sqlExc while getting login timeout: ");
            log.error(sqlE);
        }
        try(Connection c = ds.getConnection();
            PreparedStatement stmt = c.prepareStatement("SELECT fileName FROM %s WHERE toimiala = ?".formatted(tableName))
        ) {
            stmt.setString(1, toimiala);
            try(ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    repoCache.put(rs.getString(1), "");
                }
            }
        } catch (SQLException e) {
            log.error(e);
        }
        log.info("%s files already processed: %s".formatted(toimiala, repoCache.size()));
        return new MemoryIdempotentRepository(repoCache);
    }

    public static String idempotentRepositoryParam(String perustiedotOrToteumat, String toimiala) {
        return "&idempotentRepository=#%s_%s_idempotentRepository".formatted(perustiedotOrToteumat, toimiala) +
                "&idempotentKey=${file:name}";
    }
}
