package com.openlab.qualitos.quality.settings.catalog;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les listes déroulantes du client : ce qu'il lit sans rien régler, ce qu'il
 * obtient quand il règle, et ce que le domaine refuse de laisser passer.
 */
class TenantCatalogServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTRE = UUID.randomUUID();

    private FakeRepository repository;
    private TenantCatalogService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        service = new TenantCatalogService(repository);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ===== sans réglage : rien ne change pour le client =====

    @Test
    void sansReglage_lesValeursDeLaPlateformeSAppliquent() {
        TenantCatalogService.View vue = service.view(CatalogId.AUDIT_TYPE);

        assertThat(vue.defaults()).isTrue();
        assertThat(vue.entries()).isEqualTo(CatalogId.AUDIT_TYPE.defaults());
        assertThat(vue.entries()).extracting(CatalogEntry::code).contains("INTERNAL", "LPA");
    }

    @Test
    void leResumeListeLesDouzeListesEtDitLesquellesSontReglees() {
        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("LIGNE-1", "Ligne 1")));

        List<TenantCatalogService.Summary> resume = service.summaries();

        assertThat(resume).hasSize(CatalogId.values().length);
        assertThat(resume).anySatisfy(s -> {
            assertThat(s.catalogue()).isEqualTo("fives-zone");
            assertThat(s.defaults()).isFalse();
            assertThat(s.mode()).isEqualTo(CatalogMode.OPEN);
        });
        assertThat(resume).anySatisfy(s -> {
            assertThat(s.catalogue()).isEqualTo("audit-type");
            assertThat(s.defaults()).isTrue();
            assertThat(s.entryCount()).isEqualTo(CatalogId.AUDIT_TYPE.defaults().size());
        });
    }

    /**
     * Ce que l'application charge au démarrage : les listes réglées, et elles
     * seules. Douze appels pour apprendre qu'il n'y a rien à savoir seraient
     * douze appels de trop.
     */
    @Test
    void lesListesRegleesSeChargentEnUnSeulAppel() {
        assertThat(service.customised()).isEmpty();

        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("LIGNE-1", "Ligne 1")));
        service.replace(CatalogId.AUDIT_TYPE, CatalogId.AUDIT_TYPE.defaults());

        assertThat(service.customised()).containsOnlyKeys("fives-zone", "audit-type");
        assertThat(service.customised().get("fives-zone"))
                .containsExactly(CatalogEntry.of("LIGNE-1", "Ligne 1"));
    }

    @Test
    void lesListesRegleesDUnClientNeFuientPasChezUnAutre() {
        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("MIEN", "Ma zone")));

        TenantContext.setTenantId(AUTRE.toString());

        assertThat(service.customised()).isEmpty();
    }

    // ===== régler =====

    @Test
    void uneListeLibreSeRemplaceEntierement_etLOrdreRecuEstLOrdreDAffichage() {
        TenantCatalogService.View vue = service.replace(CatalogId.FIVES_ZONE, List.of(
                CatalogEntry.of("BLOC-B", "Bloc opératoire B"),
                CatalogEntry.of("BLOC-A", "Bloc opératoire A"),
                new CatalogEntry("ANCIEN", "Ancienne aile", false)));

        assertThat(vue.defaults()).isFalse();
        assertThat(vue.entries()).extracting(CatalogEntry::code)
                .containsExactly("BLOC-B", "BLOC-A", "ANCIEN");
        assertThat(repository.rows).hasSize(3);
        assertThat(repository.rows.get(0).getPosition()).isZero();
        assertThat(repository.rows.get(1).getPosition()).isEqualTo(1);
        // Relu, l'ordre tient : c'est lui que la liste déroulante suivra.
        assertThat(service.view(CatalogId.FIVES_ZONE).entries())
                .extracting(CatalogEntry::code).containsExactly("BLOC-B", "BLOC-A", "ANCIEN");
    }

    @Test
    void unRemplacementEffacePuisReecrit_sansLaisserDeTrouDOrdre() {
        service.replace(CatalogId.FIVES_ZONE, List.of(
                CatalogEntry.of("A", "A"), CatalogEntry.of("B", "B"), CatalogEntry.of("C", "C")));

        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("C", "C renommé")));

        assertThat(repository.rows).hasSize(1);
        assertThat(repository.rows.get(0).getCode()).isEqualTo("C");
        assertThat(repository.rows.get(0).getLabel()).isEqualTo("C renommé");
        assertThat(repository.rows.get(0).getPosition()).isZero();
    }

    @Test
    void uneListeAdosseeAuCodeSeRenommeEtSeReordonne() {
        List<CatalogEntry> renommee = new ArrayList<>();
        for (CatalogEntry e : CatalogId.EHS_INCIDENT_TYPE.defaults()) {
            renommee.add(CatalogEntry.of(e.code(),
                    e.code().equals("NEAR_MISS") ? "Évènement sans dommage" : e.label()));
        }
        java.util.Collections.reverse(renommee);

        TenantCatalogService.View vue = service.replace(CatalogId.EHS_INCIDENT_TYPE, renommee);

        assertThat(vue.entries().get(0).code()).isEqualTo("OTHER");
        assertThat(vue.entries()).anySatisfy(e -> {
            assertThat(e.code()).isEqualTo("NEAR_MISS");
            assertThat(e.label()).isEqualTo("Évènement sans dommage");
        });
    }

    @Test
    void retablir_revientAuxValeursDeLaPlateformeEtEffaceLesLignes() {
        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("LIGNE-1", "Ligne 1")));

        TenantCatalogService.View vue = service.reset(CatalogId.FIVES_ZONE);

        assertThat(vue.defaults()).isTrue();
        assertThat(vue.entries()).isEqualTo(CatalogId.FIVES_ZONE.defaults());
        assertThat(repository.rows).isEmpty();
    }

    // ===== ce que le code ne laisse pas faire =====

    /**
     * Ajouter une valeur à une liste adossée à une énumération du code reviendrait
     * à accepter une donnée qu'aucun chemin de code ne sait traiter.
     */
    @Test
    void surUneListeAdosseeAuCode_ajouterUneValeurEstRefuse() {
        List<CatalogEntry> avecDeTrop = new ArrayList<>(CatalogId.AUDIT_TYPE.defaults());
        avecDeTrop.add(CatalogEntry.of("CROISE", "Audit croisé"));

        assertThatThrownBy(() -> service.replace(CatalogId.AUDIT_TYPE, avecDeTrop))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("CROISE");
        assertThat(repository.rows).isEmpty();
    }

    @Test
    void surUneListeAdosseeAuCode_retirerUneValeurEstRefuse() {
        List<CatalogEntry> amputee = new ArrayList<>(CatalogId.CAPA_NATURE.defaults());
        amputee.removeIf(e -> e.code().equals("CONTAINMENT"));

        assertThatThrownBy(() -> service.replace(CatalogId.CAPA_NATURE, amputee))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("CONTAINMENT");
    }

    @Test
    void surUneListeAdosseeAuCode_desactiverUneValeurEstRefuse() {
        List<CatalogEntry> desactivee = CatalogId.MOC_CHANGE_TYPE.defaults().stream()
                .map(e -> new CatalogEntry(e.code(), e.label(), !e.code().equals("TEMPORARY")))
                .toList();

        assertThatThrownBy(() -> service.replace(CatalogId.MOC_CHANGE_TYPE, desactivee))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("TEMPORARY");
    }

    @Test
    void uneListeVideEstRefusee_leRetablissementEstLeCheminPrevu() {
        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE, List.of()))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("reset");
        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE, null))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void unCodeEnDoubleEstRefuse_memeEnCasseDifferente() {
        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE, List.of(
                CatalogEntry.of("ZONE-A", "Zone A"), CatalogEntry.of("zone-a", "Zone A bis"))))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void uneListeEntierementDesactiveeEstRefusee() {
        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE, List.of(
                new CatalogEntry("ZONE-A", "Zone A", false))))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining("active");
    }

    @Test
    void auDelaDuPlafond_laListeNEstPlusUneListe() {
        List<CatalogEntry> trop = new ArrayList<>();
        for (int i = 0; i <= CatalogId.MAX_ENTRIES; i++) {
            trop.add(CatalogEntry.of("Z" + i, "Zone " + i));
        }

        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE, trop))
                .isInstanceOf(CatalogValidationException.class)
                .hasMessageContaining(String.valueOf(CatalogId.MAX_ENTRIES));
    }

    @Test
    void unCodeHorsFormatEstRefuseDesLeDomaine() {
        assertThatThrownBy(() -> CatalogEntry.of("zone a", "Zone A"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> CatalogEntry.of("ZONE É", "Zone E"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> CatalogEntry.of("", "Zone"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> CatalogEntry.of("ZONE", "  "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> CatalogEntry.of("ZONE", "x".repeat(CatalogEntry.LABEL_MAX + 1)))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> CatalogEntry.of("Z".repeat(CatalogEntry.CODE_MAX + 1), "Zone"))
                .isInstanceOf(CatalogValidationException.class);
    }

    /** Un code saisi en minuscules désigne la même chose : il remonte en majuscules. */
    @Test
    void unCodeEstNormaliseEnMajuscules() {
        assertThat(CatalogEntry.of(" zone-a ", "  Zone A  "))
                .isEqualTo(new CatalogEntry("ZONE-A", "Zone A", true));
    }

    // ===== cloisonnement et lectures de confort =====

    @Test
    void sansClient_rienNEstLuNiEcrit() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.view(CatalogId.FIVES_ZONE))
                .isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(() -> service.summaries())
                .isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(() -> service.replace(CatalogId.FIVES_ZONE,
                List.of(CatalogEntry.of("A", "A")))).isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(() -> service.reset(CatalogId.FIVES_ZONE))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void laListeDUnClientNEstPasCelleDUnAutre() {
        service.replace(CatalogId.FIVES_ZONE, List.of(CatalogEntry.of("MIEN", "Ma zone")));

        TenantContext.setTenantId(AUTRE.toString());

        assertThat(service.view(CatalogId.FIVES_ZONE).defaults()).isTrue();
        assertThat(service.view(CatalogId.FIVES_ZONE).entries())
                .isEqualTo(CatalogId.FIVES_ZONE.defaults());
    }

    @Test
    void lesEntreesActivesSontCeQuUnFormulaireProposent() {
        service.replace(CatalogId.FIVES_ZONE, List.of(
                CatalogEntry.of("VIVANTE", "Zone en service"),
                new CatalogEntry("FERMEE", "Zone fermée", false)));

        assertThat(service.active(CatalogId.FIVES_ZONE))
                .extracting(CatalogEntry::code).containsExactly("VIVANTE");
    }

    /**
     * Un code retiré du catalogue reste lisible : l'historique garde son code, et
     * un libellé vide laisserait croire que la donnée manque.
     */
    @Test
    void leLibelleDUnCodeInconnuEstLeCodeLuiMeme() {
        assertThat(service.label(CatalogId.AUDIT_TYPE, "INTERNAL")).isEqualTo("Interne");
        assertThat(service.label(CatalogId.AUDIT_TYPE, "internal")).isEqualTo("Interne");
        assertThat(service.label(CatalogId.AUDIT_TYPE, "DISPARU")).isEqualTo("DISPARU");
        assertThat(service.label(CatalogId.AUDIT_TYPE, null)).isEmpty();
        assertThat(service.label(CatalogId.AUDIT_TYPE, "  ")).isEmpty();
    }

    @Test
    void uneListeSeTrouveParSonNomDUrl_etRienDAutre() {
        assertThat(CatalogId.bySlug("audit-type")).contains(CatalogId.AUDIT_TYPE);
        assertThat(CatalogId.bySlug(" AUDIT-TYPE ")).contains(CatalogId.AUDIT_TYPE);
        assertThat(CatalogId.bySlug("inconnue")).isEmpty();
        assertThat(CatalogId.bySlug(null)).isEmpty();
    }

    /** Les noms d'URL sont uniques, et les valeurs par défaut tiennent toutes. */
    @Test
    void chaqueListeADesDefautsValidesEtUnNomUnique() {
        assertThat(CatalogId.all()).extracting(CatalogId::slug).doesNotHaveDuplicates();
        for (CatalogId id : CatalogId.values()) {
            assertThat(id.defaults()).as(id.slug()).isNotEmpty();
            assertThat(id.defaults()).extracting(CatalogEntry::code).doesNotHaveDuplicates();
            assertThat(id.defaults()).allSatisfy(e -> assertThat(e.active()).isTrue());
            // Les défauts doivent pouvoir être réenregistrés tels quels, sinon
            // « rétablir puis enregistrer » échouerait sur la liste de départ.
            assertThat(service.replace(id, id.defaults()).entries()).isEqualTo(id.defaults());
        }
    }

    /** Un dépôt en mémoire : l'ordre d'insertion est celui de la base, par position. */
    private static final class FakeRepository implements TenantCatalogEntryRepository {

        private final List<TenantCatalogEntryRow> rows = new ArrayList<>();

        @Override
        public List<TenantCatalogEntryRow> findByTenantIdAndCatalogueOrderByPositionAsc(
                UUID tenantId, String catalogue) {
            return rows.stream()
                    .filter(r -> r.getTenantId().equals(tenantId) && r.getCatalogue().equals(catalogue))
                    .sorted(java.util.Comparator.comparingInt(TenantCatalogEntryRow::getPosition))
                    .toList();
        }

        @Override
        public void deleteByTenantIdAndCatalogue(UUID tenantId, String catalogue) {
            rows.removeIf(r -> r.getTenantId().equals(tenantId) && r.getCatalogue().equals(catalogue));
        }

        @Override
        public List<String> findCustomisedCatalogues(UUID tenantId) {
            return rows.stream()
                    .filter(r -> r.getTenantId().equals(tenantId))
                    .map(TenantCatalogEntryRow::getCatalogue)
                    .distinct()
                    .toList();
        }

        @Override
        public <S extends TenantCatalogEntryRow> List<S> saveAll(Iterable<S> entities) {
            List<S> saved = new ArrayList<>();
            entities.forEach(e -> {
                if (e.getId() == null) {
                    e.setId(UUID.randomUUID());
                }
                rows.add(e);
                saved.add(e);
            });
            return saved;
        }

        @Override
        public void flush() {
            // rien à vider : tout est déjà en mémoire
        }

        // ---- le reste de JpaRepository n'est pas employé par le service ----

        @Override public <S extends TenantCatalogEntryRow> S save(S entity) { throw unsupported(); }
        @Override public java.util.Optional<TenantCatalogEntryRow> findById(UUID uuid) { throw unsupported(); }
        @Override public boolean existsById(UUID uuid) { throw unsupported(); }
        @Override public List<TenantCatalogEntryRow> findAll() { return List.copyOf(rows); }
        @Override public List<TenantCatalogEntryRow> findAllById(Iterable<UUID> uuids) { throw unsupported(); }
        @Override public long count() { return rows.size(); }
        @Override public void deleteById(UUID uuid) { throw unsupported(); }
        @Override public void delete(TenantCatalogEntryRow entity) { throw unsupported(); }
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) { throw unsupported(); }
        @Override public void deleteAll(Iterable<? extends TenantCatalogEntryRow> entities) { throw unsupported(); }
        @Override public void deleteAll() { rows.clear(); }
        @Override public <S extends TenantCatalogEntryRow> S saveAndFlush(S entity) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> List<S> saveAllAndFlush(Iterable<S> entities) { throw unsupported(); }
        @Override public void deleteAllInBatch(Iterable<TenantCatalogEntryRow> entities) { throw unsupported(); }
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) { throw unsupported(); }
        @Override public void deleteAllInBatch() { throw unsupported(); }
        @Override public TenantCatalogEntryRow getOne(UUID uuid) { throw unsupported(); }
        @Override public TenantCatalogEntryRow getById(UUID uuid) { throw unsupported(); }
        @Override public TenantCatalogEntryRow getReferenceById(UUID uuid) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> java.util.Optional<S> findOne(
                org.springframework.data.domain.Example<S> example) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> List<S> findAll(
                org.springframework.data.domain.Example<S> example) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> List<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Sort sort) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> org.springframework.data.domain.Page<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Pageable pageable) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> long count(
                org.springframework.data.domain.Example<S> example) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow> boolean exists(
                org.springframework.data.domain.Example<S> example) { throw unsupported(); }
        @Override public <S extends TenantCatalogEntryRow, R> R findBy(
                org.springframework.data.domain.Example<S> example,
                java.util.function.Function<org.springframework.data.repository.query.FluentQuery
                        .FetchableFluentQuery<S>, R> queryFunction) { throw unsupported(); }
        @Override public List<TenantCatalogEntryRow> findAll(
                org.springframework.data.domain.Sort sort) { throw unsupported(); }
        @Override public org.springframework.data.domain.Page<TenantCatalogEntryRow> findAll(
                org.springframework.data.domain.Pageable pageable) { throw unsupported(); }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used by TenantCatalogService");
        }
    }
}
