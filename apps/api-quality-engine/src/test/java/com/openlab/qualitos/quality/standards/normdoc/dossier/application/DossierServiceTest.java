package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierRepository;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStateException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure.StaticDossierPlanProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DossierServiceTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000099");
    private static final UUID STD = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID AUTHOR = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID FINALIZER = UUID.fromString("dddddddd-0000-0000-0000-000000000004");
    private static final Instant T0 = Instant.parse("2026-06-22T09:00:00Z");

    // ---- In-memory test doubles ----

    static final class InMemoryDossierRepo implements DossierRepository {
        final Map<UUID, DocumentationDossier> store = new HashMap<>();

        @Override public DocumentationDossier save(DocumentationDossier d) {
            if (d.getId() == null) {
                d.assignId(UUID.randomUUID());
            }
            store.put(d.getId(), d);
            return d;
        }
        @Override public Optional<DocumentationDossier> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }
        @Override public List<DocumentationDossier> findByTenant(UUID tenantId) {
            return store.values().stream().filter(d -> d.getTenantId().equals(tenantId)).toList();
        }
    }

    static final class InMemoryNormDocRepo implements NormDocRepository {
        final Map<UUID, NormativeDocument> store = new HashMap<>();

        @Override public NormativeDocument save(NormativeDocument d) {
            if (d.getId() == null) {
                d.assignId(UUID.randomUUID());
            }
            store.put(d.getId(), d);
            return d;
        }
        @Override public Optional<NormativeDocument> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }
        @Override public List<NormativeDocument> findByTenant(UUID t) {
            return store.values().stream().filter(d -> d.getTenantId().equals(t)).toList();
        }
        @Override public List<NormativeDocument> findByTenantAndStatus(UUID t, NormDocStatus s) {
            return store.values().stream()
                    .filter(d -> d.getTenantId().equals(t) && d.getStatus() == s).toList();
        }
        @Override public void delete(UUID id) {
            store.remove(id);
        }
    }

    /** Générateur déterministe ; peut être réglé pour échouer sur certains types. */
    static final class StubGenerator implements NormDocGenerator {
        final List<NormDocKind> failKinds = new ArrayList<>();
        int calls;

        @Override public GeneratedNormDoc generate(NormDocGenerationCommand command) {
            calls++;
            if (failKinds.contains(command.kind())) {
                throw new IllegalStateException("ai-service indisponible");
            }
            List<NormDocSection> sections = command.sections().stream()
                    .map(s -> new NormDocSection(s.key(), s.title(), s.clauses(),
                            "Corps " + s.title()))
                    .toList();
            return new GeneratedNormDoc("Doc " + command.kind() + " — " + command.organizationName(),
                    sections, "ollama");
        }
    }

    static final class StubReuse implements DossierReuseLookup {
        final List<ReusableDoc> result = new ArrayList<>();
        @Override public List<ReusableDoc> findApprovedByKind(NormDocKind kind, UUID exclude) {
            return result.stream().filter(r -> r.kind() == kind).toList();
        }
    }

    static final class StubIntegrity implements DossierIntegrityPort {
        String lastCanonical;
        @Override public Sealed seal(UUID tenantId, String canonicalContent) {
            lastCanonical = canonicalContent;
            return new Sealed("f".repeat(64), "sig-hybride", "tx-anchor-1");
        }
    }

    private InMemoryDossierRepo dossiers;
    private InMemoryNormDocRepo normDocs;
    private StubGenerator generator;
    private StubReuse reuse;
    private StubIntegrity integrity;
    private NormDocStandardLookup standards;
    private NormDocTenantProvider tenant;
    private NormDocActorProvider actor;
    private DossierService service;

    @BeforeEach
    void setup() {
        dossiers = new InMemoryDossierRepo();
        normDocs = new InMemoryNormDocRepo();
        generator = new StubGenerator();
        reuse = new StubReuse();
        integrity = new StubIntegrity();
        standards = id -> Optional.of(
                new NormDocStandardLookup.StandardRef(STD, "iso-9001", "ISO 9001:2015"));
        tenant = () -> TENANT;
        actor = () -> AUTHOR;
        service = newService(actor);
    }

    private DossierService newService(NormDocActorProvider actorProvider) {
        return new DossierService(dossiers, new StaticDossierPlanProvider(), generator, normDocs,
                standards, reuse, integrity, tenant, actorProvider,
                new DossierEventPublisher.NoOp(), Clock.fixed(T0, ZoneOffset.UTC));
    }

    private DossierDto.StartRequest startReq(List<String> keys) {
        return new DossierDto.StartRequest(STD,
                new DossierDto.TenantProfile("ACME", "manufacturing", "PME", "fr",
                        List.of("achats")),
                keys);
    }

    // ---- catalog ----

    @Test
    void catalog_exposesPlannableDocuments() {
        List<DossierDto.DocumentView> cat = service.catalog();
        assertThat(cat).extracting(DossierDto.DocumentView::key)
                .contains("manuel-qualite", "politique-qualite", "proc-audit-interne");
    }

    // ---- start ----

    @Test
    void start_generatesAllDefaultDocuments_asDrafts() {
        DossierDto.View v = service.start(startReq(List.of()));
        assertThat(v.status()).isEqualTo(DossierStatus.GENERE);
        assertThat(v.totalCount()).isEqualTo(6);
        assertThat(v.generatedCount()).isEqualTo(6);
        assertThat(v.progressPercent()).isEqualTo(100);
        assertThat(v.aiProvider()).isEqualTo("ollama");
        // chaque pièce a produit un NormativeDocument en BROUILLON_IA
        assertThat(normDocs.store).hasSize(6);
        assertThat(normDocs.store.values()).allMatch(NormativeDocument::isDraft);
        assertThat(v.documents()).allMatch(d -> d.status() == DossierDocStatus.GENERE);
        assertThat(v.documents()).allMatch(d -> d.normDocId() != null);
    }

    @Test
    void start_subsetSelection_generatesOnlyRequested() {
        DossierDto.View v = service.start(startReq(List.of("manuel-qualite", "politique-qualite")));
        assertThat(v.totalCount()).isEqualTo(2);
        assertThat(v.documents()).extracting(DossierDto.DocumentView::key)
                .containsExactly("manuel-qualite", "politique-qualite");
    }

    @Test
    void start_unknownDocumentKey_400() {
        assertThatThrownBy(() -> service.start(startReq(List.of("inconnu"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown dossier document key");
    }

    @Test
    void start_unknownStandard_throwsNotFound() {
        service = new DossierService(dossiers, new StaticDossierPlanProvider(), generator, normDocs,
                id -> Optional.empty(), reuse, integrity, tenant, actor,
                new DossierEventPublisher.NoOp(), Clock.fixed(T0, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.start(startReq(List.of())))
                .isInstanceOf(StandardNotFoundException.class);
    }

    @Test
    void start_missingStandardId_400() {
        DossierDto.StartRequest req = new DossierDto.StartRequest(null,
                new DossierDto.TenantProfile("ACME", "it", "PME", "fr", List.of()), List.of());
        assertThatThrownBy(() -> service.start(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("standardId");
    }

    @Test
    void start_missingOrganization_400() {
        DossierDto.StartRequest req = new DossierDto.StartRequest(STD,
                new DossierDto.TenantProfile(" ", "it", "PME", "fr", List.of()), List.of());
        assertThatThrownBy(() -> service.start(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organizationName");
    }

    @Test
    void start_aiFailureOnOneDoc_marksFailed_othersSucceed() {
        generator.failKinds.add(NormDocKind.POLICY);
        DossierDto.View v = service.start(startReq(List.of("manuel-qualite", "politique-qualite")));
        assertThat(v.status()).isEqualTo(DossierStatus.GENERE);
        assertThat(v.failedCount()).isEqualTo(1);
        assertThat(v.generatedCount()).isEqualTo(1);
        DossierDto.DocumentView pol = v.documents().stream()
                .filter(d -> d.key().equals("politique-qualite")).findFirst().orElseThrow();
        assertThat(pol.status()).isEqualTo(DossierDocStatus.ECHEC);
        assertThat(pol.failureReason()).contains("indisponible");
    }

    @Test
    void start_suggestsReuse_whenApprovedEquivalentExists() {
        UUID approvedManual = UUID.randomUUID();
        reuse.result.add(new DossierReuseLookup.ReusableDoc(
                approvedManual, UUID.randomUUID(), "iso-27001", "Manuel ISO 27001",
                NormDocKind.MANUAL));
        DossierDto.View v = service.start(startReq(List.of("manuel-qualite")));
        DossierDto.DocumentView manual = v.documents().get(0);
        assertThat(manual.reuseSuggestedNormDocId()).isEqualTo(approvedManual);
    }

    // ---- retry ----

    @Test
    void retry_regeneratesFailedDocuments() {
        generator.failKinds.add(NormDocKind.POLICY);
        DossierDto.View started = service.start(startReq(List.of("politique-qualite")));
        assertThat(started.failedCount()).isEqualTo(1);

        generator.failKinds.clear();
        DossierDto.View retried = service.retryFailed(started.id());
        assertThat(retried.failedCount()).isZero();
        assertThat(retried.generatedCount()).isEqualTo(1);
        assertThat(retried.status()).isEqualTo(DossierStatus.GENERE);
    }

    @Test
    void retry_nothingToRegenerate_409() {
        DossierDto.View started = service.start(startReq(List.of("manuel-qualite")));
        assertThatThrownBy(() -> service.retryFailed(started.id()))
                .isInstanceOf(DossierStateException.class)
                .hasMessageContaining("Aucune pièce");
    }

    @Test
    void retry_finalizedDossier_409() {
        DossierDto.View v = approveAllAndReturn(service.start(startReq(List.of("manuel-qualite"))));
        DossierDto.View finalized = service.finalizeDossier(v.id(),
                new DossierDto.FinalizeRequest("ma-signature", "ok"));
        assertThatThrownBy(() -> service.retryFailed(finalized.id()))
                .isInstanceOf(DossierStateException.class).hasMessageContaining("finalisé");
    }

    // ---- finalize ----

    @Test
    void finalize_allApproved_sealsAnchorsAndPersists() {
        DossierDto.View v = approveAllAndReturn(service.start(startReq(List.of("manuel-qualite"))));
        DossierService finalizer = newService(() -> FINALIZER);
        DossierDto.View out = finalizer.finalizeDossier(v.id(),
                new DossierDto.FinalizeRequest("ma-signature", "RAS"));
        assertThat(out.status()).isEqualTo(DossierStatus.FINALISE);
        assertThat(out.integritySha256()).hasSize(64);
        assertThat(out.integritySignature()).isEqualTo("sig-hybride");
        assertThat(out.anchorTxRef()).isEqualTo("tx-anchor-1");
        assertThat(out.finalizedByUserId()).isEqualTo(FINALIZER);
        // le contenu canonique scellé agrège le Markdown des pièces approuvées
        assertThat(integrity.lastCanonical).contains("Dossier documentaire");
        assertThat(integrity.lastCanonical).contains("# Doc MANUAL");
    }

    @Test
    void finalize_missingSignature_400() {
        DossierDto.View v = approveAllAndReturn(service.start(startReq(List.of("manuel-qualite"))));
        assertThatThrownBy(() -> service.finalizeDossier(v.id(),
                new DossierDto.FinalizeRequest(" ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void finalize_documentNotApproved_409() {
        // pièces générées mais non approuvées → finalisation interdite (§18.2 #5)
        DossierDto.View v = service.start(startReq(List.of("manuel-qualite")));
        assertThatThrownBy(() -> service.finalizeDossier(v.id(),
                new DossierDto.FinalizeRequest("sig", null)))
                .isInstanceOf(DossierStateException.class)
                .hasMessageContaining("approuvées");
    }

    @Test
    void finalize_unknownDossier_404() {
        assertThatThrownBy(() -> service.finalizeDossier(UUID.randomUUID(),
                new DossierDto.FinalizeRequest("sig", null)))
                .isInstanceOf(DossierNotFoundException.class);
    }

    // ---- get / list ----

    @Test
    void get_returnsView() {
        DossierDto.View started = service.start(startReq(List.of("manuel-qualite")));
        assertThat(service.get(started.id()).id()).isEqualTo(started.id());
    }

    @Test
    void get_crossTenant_404() {
        DossierDto.View started = service.start(startReq(List.of("manuel-qualite")));
        DossierService otherTenantSvc = new DossierService(dossiers, new StaticDossierPlanProvider(),
                generator, normDocs, standards, reuse, integrity, () -> OTHER_TENANT, actor,
                new DossierEventPublisher.NoOp(), Clock.fixed(T0, ZoneOffset.UTC));
        assertThatThrownBy(() -> otherTenantSvc.get(started.id()))
                .isInstanceOf(DossierNotFoundException.class);
    }

    @Test
    void list_filtersByTenant() {
        service.start(startReq(List.of("manuel-qualite")));
        assertThat(service.list()).hasSize(1);
    }

    // ---- helpers ----

    /** Approuve toutes les pièces générées d'un dossier (simule la validation humaine). */
    private DossierDto.View approveAllAndReturn(DossierDto.View v) {
        Instant now = T0;
        for (DossierDto.DocumentView d : v.documents()) {
            if (d.normDocId() == null) {
                continue;
            }
            NormativeDocument nd = normDocs.findById(d.normDocId()).orElseThrow();
            nd.submitForReview(AUTHOR, now);
            nd.approve(FINALIZER, "signature-humaine", "ok", now);
            normDocs.save(nd);
        }
        return v;
    }
}
