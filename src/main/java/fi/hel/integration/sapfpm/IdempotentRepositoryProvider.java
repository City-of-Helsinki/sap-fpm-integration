package fi.hel.integration.sapfpm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;

@ApplicationScoped
public class IdempotentRepositoryProvider {
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
        return createIdempotentRepository();
    }

    @Named("toteumat_kasko_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatKaskoIdempotentRepository() {
        return createIdempotentRepository();
    }

    @Named("toteumat_palke_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatPalkeIdempotentRepository() {
        return createIdempotentRepository();
    }

    @Named("toteumat_sotepe_idempotentRepository")
    @ApplicationScoped
    public IdempotentRepository toteumatSotepeIdempotentRepository() {
        return createIdempotentRepository();
    }

    public IdempotentRepository createIdempotentRepository() {
        MemoryIdempotentRepository memoryIdempotentRepo = new MemoryIdempotentRepository();
        memoryIdempotentRepo.setCacheSize(5000);
        return memoryIdempotentRepo;
    }

    public static String idempotentRepositoryParam(String perustiedotOrToteumat, String toimiala) {
        return "&idempotentRepository=#%s_%s_idempotentRepository".formatted(perustiedotOrToteumat, toimiala);
    }
}
