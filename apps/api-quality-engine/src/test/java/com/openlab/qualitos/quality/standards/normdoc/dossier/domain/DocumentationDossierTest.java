package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentationDossierTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STD = UUID.randomUUID();
    private static final UUID AUTHOR = UUID.randomUUID();
    private static final UUID FINALIZER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-06-22T09:00:00Z");

    private static DossierDocument doc(String key, NormDocKind kind) {
        return DossierDocument.planned(key, kind, key,
                List.of(new SectionPlan("s", "Section", List.of("4.1"), "")));
    }

    private static DocumentationDossier start(List<DossierDocument> docs) {
        return DocumentationDossier.start(TENANT, STD, "iso-9001", "ISO 9001:2015",
                "ACME", "fr", docs, AUTHOR, NOW);
    }

    @Test
    void start_isInGeneration_withZeroProgress() {
        DocumentationDossier d = start(List.of(doc("manuel", NormDocKind.MANUAL),
                doc("pol", NormDocKind.POLICY)));
        assertThat(d.getStatus()).isEqualTo(DossierStatus.GENERATION_EN_COURS);
        assertThat(d.totalCount()).isEqualTo(2);
        assertThat(d.generatedCount()).isZero();
        assertThat(d.progressPercent()).isZero();
        assertThat(d.getTenantId()).isEqualTo(TENANT);
        assertThat(d.getLanguage()).isEqualTo("fr");
    }

    @Test
    void requires_uniqueDocumentKeys() {
        assertThatThrownBy(() -> start(List.of(doc("k", NormDocKind.MANUAL),
                doc("k", NormDocKind.POLICY))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");
    }

    @Test
    void requires_atLeastOneDocument() {
        assertThatThrownBy(() -> start(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshStatus_allGenerated_becomesGenere() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        m.markGenerated(UUID.randomUUID());
        d.refreshGenerationStatus(NOW);
        assertThat(d.getStatus()).isEqualTo(DossierStatus.GENERE);
        assertThat(d.progressPercent()).isEqualTo(100);
    }

    @Test
    void refreshStatus_someFailed_stillGenere_progressCountsNonPending() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DossierDocument p = doc("p", NormDocKind.POLICY);
        DocumentationDossier d = start(List.of(m, p));
        m.markGenerated(UUID.randomUUID());
        p.markFailed("boom");
        d.refreshGenerationStatus(NOW);
        assertThat(d.getStatus()).isEqualTo(DossierStatus.GENERE);
        assertThat(d.failedCount()).isEqualTo(1);
        assertThat(d.generatedCount()).isEqualTo(1);
        assertThat(d.progressPercent()).isEqualTo(100);
    }

    @Test
    void refreshStatus_pending_staysInGeneration() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DossierDocument p = doc("p", NormDocKind.POLICY);
        DocumentationDossier d = start(List.of(m, p));
        m.markGenerated(UUID.randomUUID());
        d.refreshGenerationStatus(NOW);
        assertThat(d.getStatus()).isEqualTo(DossierStatus.GENERATION_EN_COURS);
        assertThat(d.progressPercent()).isEqualTo(50);
    }

    @Test
    void document_lookupByKey() {
        DocumentationDossier d = start(List.of(doc("manuel", NormDocKind.MANUAL)));
        assertThat(d.document("manuel").getKind()).isEqualTo(NormDocKind.MANUAL);
        assertThatThrownBy(() -> d.document("absent"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void finalize_allApproved_sealsAndAnchors() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        m.markGenerated(UUID.randomUUID());
        d.refreshGenerationStatus(NOW);

        d.finalize("a".repeat(64), "sig", "tx-1", FINALIZER, NOW);
        assertThat(d.getStatus()).isEqualTo(DossierStatus.FINALISE);
        assertThat(d.isFinalized()).isTrue();
        assertThat(d.getIntegritySha256()).hasSize(64);
        assertThat(d.getIntegritySignature()).isEqualTo("sig");
        assertThat(d.getAnchorTxRef()).isEqualTo("tx-1");
        assertThat(d.getFinalizedByUserId()).isEqualTo(FINALIZER);
    }

    @Test
    void finalize_pendingDocument_rejected() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        assertThatThrownBy(() -> d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW))
                .isInstanceOf(DossierStateException.class)
                .hasMessageContaining("Génération non terminée");
    }

    @Test
    void finalize_noGeneratedDocument_rejected() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        m.markFailed("boom");
        d.refreshGenerationStatus(NOW);
        assertThatThrownBy(() -> d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW))
                .isInstanceOf(DossierStateException.class)
                .hasMessageContaining("Aucun document généré");
    }

    @Test
    void finalize_twice_rejected() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        m.markGenerated(UUID.randomUUID());
        d.refreshGenerationStatus(NOW);
        d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW);
        assertThatThrownBy(() -> d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW))
                .isInstanceOf(DossierStateException.class)
                .hasMessageContaining("déjà finalisé");
    }

    @Test
    void refreshStatus_doesNotDegradeFinalized() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DocumentationDossier d = start(List.of(m));
        m.markGenerated(UUID.randomUUID());
        d.refreshGenerationStatus(NOW);
        d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW);
        d.refreshGenerationStatus(NOW);
        assertThat(d.getStatus()).isEqualTo(DossierStatus.FINALISE);
    }

    @Test
    void recordProvider_setsProvider() {
        DocumentationDossier d = start(List.of(doc("m", NormDocKind.MANUAL)));
        d.recordProvider("ollama", NOW);
        assertThat(d.getAiProvider()).isEqualTo("ollama");
        d.recordProvider(" ", NOW);
        assertThat(d.getAiProvider()).isEqualTo("ollama");
    }

    @Test
    void generatedDocumentIds_collectsLinks() {
        DossierDocument m = doc("m", NormDocKind.MANUAL);
        DossierDocument p = doc("p", NormDocKind.POLICY);
        DocumentationDossier d = start(List.of(m, p));
        UUID id1 = UUID.randomUUID();
        m.markGenerated(id1);
        assertThat(d.generatedDocumentIds()).containsExactly(id1);
    }

    @Test
    void language_defaultsToFr_whenBlank() {
        DocumentationDossier d = DocumentationDossier.start(TENANT, STD, "iso-9001",
                "ISO 9001", "ACME", " ", List.of(doc("m", NormDocKind.MANUAL)), AUTHOR, NOW);
        assertThat(d.getLanguage()).isEqualTo("fr");
    }
}
