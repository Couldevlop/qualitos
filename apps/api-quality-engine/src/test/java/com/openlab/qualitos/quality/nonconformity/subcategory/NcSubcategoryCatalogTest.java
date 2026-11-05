package com.openlab.qualitos.quality.nonconformity.subcategory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le catalogue de sous-catégories : ce qu'un client lit sans rien régler, ce
 * qu'il obtient quand il règle, et ce que le domaine refuse.
 */
class NcSubcategoryCatalogTest {

    private static final UUID TENANT = UUID.randomUUID();

    private FakeRepository repository;
    private NcSubcategoryCatalog catalog;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        catalog = new NcSubcategoryCatalog(repository, new ObjectMapper());
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sansReglage_leCatalogueDeLaPlateformeSApplique() {
        NcSubcategoryCatalog.View vue = catalog.view();

        assertThat(vue.defaults()).isTrue();
        assertThat(vue.entries()).isEqualTo(NcSubcategoryDefaults.CATALOGUE);
    }

    @Test
    void leCatalogueDeLaPlateformeCouvreLesSeptCategories() {
        // Une catégorie sans aucune nature serait un cul-de-sac à la saisie.
        assertThat(NcSubcategoryDefaults.CATALOGUE).extracting(NcSubcategory::category)
                .containsAll(List.of(NcCategory.values()));
    }

    @Test
    void lesCodesDuCatalogueDeLaPlateformeSontUniques() {
        assertThat(NcSubcategoryDefaults.CATALOGUE).extracting(NcSubcategory::code)
                .doesNotHaveDuplicates();
    }

    @Test
    void leClientRemplaceLeCatalogueParLeSien() {
        NcSubcategoryCatalog.View vue = catalog.replace(List.of(
                new NcSubcategory("MIEN-1", NcCategory.PROCESS, "Ma nature à moi")));

        assertThat(vue.defaults()).isFalse();
        assertThat(vue.entries()).singleElement().extracting(NcSubcategory::code).isEqualTo("MIEN-1");
        assertThat(catalog.view().entries()).singleElement()
                .extracting(NcSubcategory::label).isEqualTo("Ma nature à moi");
    }

    @Test
    void leRetourAuCatalogueDeLaPlateformeEffaceLeReglage() {
        catalog.replace(List.of(new NcSubcategory("MIEN-1", NcCategory.PROCESS, "Ma nature")));

        NcSubcategoryCatalog.View vue = catalog.reset();

        assertThat(vue.defaults()).isTrue();
        assertThat(catalog.view().defaults()).isTrue();
    }

    @Test
    void unCatalogueVideEstRefuse() {
        assertThatThrownBy(() -> catalog.replace(List.of()))
                .isInstanceOf(NcValidationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void unCodeEnDoubleEstRefuse() {
        // La casse ne crée pas deux natures : « PRD-DIM » et « prd-dim » sont la même.
        assertThatThrownBy(() -> catalog.replace(List.of(
                new NcSubcategory("PRD-DIM", NcCategory.PRODUCT, "Cote"),
                new NcSubcategory("prd-dim", NcCategory.PRODUCT, "Cote encore"))))
                .isInstanceOf(NcValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void unCatalogueTropLongEstRefuse() {
        List<NcSubcategory> trop = new ArrayList<>();
        for (int i = 0; i <= NcSubcategoryCatalog.MAX_ENTRIES; i++) {
            trop.add(new NcSubcategory("C" + i, NcCategory.OTHER, "Nature " + i));
        }

        assertThatThrownBy(() -> catalog.replace(trop))
                .isInstanceOf(NcValidationException.class)
                .hasMessageContaining(String.valueOf(NcSubcategoryCatalog.MAX_ENTRIES));
    }

    @Test
    void aucuneNatureChoisie_resteNul() {
        assertThat(catalog.resolve(null, NcCategory.PRODUCT)).isNull();
        assertThat(catalog.resolve("   ", NcCategory.PRODUCT)).isNull();
    }

    @Test
    void uneNatureConnueEstAcceptee_etRenduDansSaCasse() {
        assertThat(catalog.resolve("prd-dim", NcCategory.PRODUCT)).isEqualTo("PRD-DIM");
    }

    @Test
    void uneNatureInconnueEstRefusee() {
        assertThatThrownBy(() -> catalog.resolve("PAS-UN-CODE", NcCategory.PRODUCT))
                .isInstanceOf(NcValidationException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void uneNatureDUneAutreCategorieEstRefusee() {
        // Classer un écart produit sous une nature d'environnement rendrait tout
        // croisement catégorie × nature illisible.
        assertThatThrownBy(() -> catalog.resolve("ENV-DEC", NcCategory.PRODUCT))
                .isInstanceOf(NcValidationException.class)
                .hasMessageContaining("belongs to category");
    }

    @Test
    void sansCategorieImposee_laNatureEstSimplementVerifiee() {
        assertThat(catalog.resolve("ENV-DEC", null)).isEqualTo("ENV-DEC");
    }

    @Test
    void uneEntreeSansCodeOuSansLibelleEstRefusee() {
        assertThatThrownBy(() -> new NcSubcategory("  ", NcCategory.OTHER, "Libellé"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NcSubcategory("CODE", NcCategory.OTHER, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NcSubcategory("CODE", null, "Libellé"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unCodeOuUnLibelleTropLongEstRefuse() {
        assertThatThrownBy(() -> new NcSubcategory("C".repeat(NcSubcategory.CODE_MAX + 1),
                NcCategory.OTHER, "Libellé")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NcSubcategory("CODE", NcCategory.OTHER,
                "L".repeat(NcSubcategory.LABEL_MAX + 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void horsContexteClient_rienNEstRendu() {
        TenantContext.clear();

        assertThatThrownBy(() -> catalog.view()).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void unReglageIllisibleRemonteEnAnomalie() {
        // Mieux vaut une erreur franche qu'un classement sous une nomenclature
        // qui n'est pas celle du client.
        NcSubcategorySettingsJpaEntity casse = new NcSubcategorySettingsJpaEntity();
        casse.setTenantId(TENANT);
        casse.setCatalogue("{ pas du json");
        repository.save(casse);

        assertThatThrownBy(() -> catalog.view()).isInstanceOf(IllegalStateException.class);
    }

    /** Un dépôt en mémoire : la règle testée est celle du catalogue, pas celle de JPA. */
    private static final class FakeRepository implements NcSubcategorySettingsJpaRepository {

        private final Map<UUID, NcSubcategorySettingsJpaEntity> parTenant = new HashMap<>();

        @Override
        public Optional<NcSubcategorySettingsJpaEntity> findByTenantId(UUID tenantId) {
            return Optional.ofNullable(parTenant.get(tenantId));
        }

        @Override
        public <S extends NcSubcategorySettingsJpaEntity> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            parTenant.put(entity.getTenantId(), entity);
            return entity;
        }

        @Override
        public void delete(NcSubcategorySettingsJpaEntity entity) {
            parTenant.remove(entity.getTenantId());
        }

        // --- le reste du contrat JpaRepository, inutile ici ---
        @Override public List<NcSubcategorySettingsJpaEntity> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<NcSubcategorySettingsJpaEntity> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<NcSubcategorySettingsJpaEntity> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public List<NcSubcategorySettingsJpaEntity> findAllById(Iterable<UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return parTenant.size(); }
        @Override public void deleteById(UUID id) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(Iterable<? extends UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends NcSubcategorySettingsJpaEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { parTenant.clear(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<NcSubcategorySettingsJpaEntity> findById(UUID id) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(UUID id) { throw new UnsupportedOperationException(); }
        @Override public void flush() { /* rien à vider */ }
        @Override public <S extends NcSubcategorySettingsJpaEntity> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<NcSubcategorySettingsJpaEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { parTenant.clear(); }
        @Override public NcSubcategorySettingsJpaEntity getOne(UUID id) { throw new UnsupportedOperationException(); }
        @Override public NcSubcategorySettingsJpaEntity getById(UUID id) { throw new UnsupportedOperationException(); }
        @Override public NcSubcategorySettingsJpaEntity getReferenceById(UUID id) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends NcSubcategorySettingsJpaEntity, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
