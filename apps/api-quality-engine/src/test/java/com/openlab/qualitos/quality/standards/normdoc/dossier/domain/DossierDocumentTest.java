package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DossierDocumentTest {

    private static List<SectionPlan> oneSection() {
        return List.of(new SectionPlan("k", "Titre", List.of("4.1"), "consigne"));
    }

    @Test
    void planned_startsPending() {
        DossierDocument d = DossierDocument.planned("manuel", NormDocKind.MANUAL, "Manuel",
                oneSection());
        assertThat(d.getStatus()).isEqualTo(DossierDocStatus.EN_ATTENTE);
        assertThat(d.isPending()).isTrue();
        assertThat(d.isGenerated()).isFalse();
        assertThat(d.getNormDocId()).isNull();
        assertThat(d.getKind()).isEqualTo(NormDocKind.MANUAL);
        assertThat(d.getSections()).hasSize(1);
    }

    @Test
    void markGenerating_thenGenerated_linksNormDoc() {
        DossierDocument d = DossierDocument.planned("p", NormDocKind.POLICY, "Pol", oneSection());
        d.markGenerating();
        assertThat(d.getStatus()).isEqualTo(DossierDocStatus.EN_GENERATION);
        assertThat(d.isPending()).isTrue();

        UUID nd = UUID.randomUUID();
        d.markGenerated(nd);
        assertThat(d.getStatus()).isEqualTo(DossierDocStatus.GENERE);
        assertThat(d.isGenerated()).isTrue();
        assertThat(d.isPending()).isFalse();
        assertThat(d.getNormDocId()).isEqualTo(nd);
    }

    @Test
    void markFailed_recordsReason_clearsLink() {
        DossierDocument d = DossierDocument.planned("p", NormDocKind.PROCEDURE, "Proc", oneSection());
        d.markGenerating();
        d.markFailed("timeout ai-service");
        assertThat(d.isFailed()).isTrue();
        assertThat(d.getFailureReason()).isEqualTo("timeout ai-service");
        assertThat(d.getNormDocId()).isNull();
    }

    @Test
    void markFailed_blankReason_defaults() {
        DossierDocument d = DossierDocument.planned("p", NormDocKind.PROCEDURE, "Proc", oneSection());
        d.markFailed("  ");
        assertThat(d.getFailureReason()).isEqualTo("génération IA indisponible");
    }

    @Test
    void markFailed_longReason_truncated() {
        DossierDocument d = DossierDocument.planned("p", NormDocKind.PROCEDURE, "Proc", oneSection());
        d.markFailed("x".repeat(5000));
        assertThat(d.getFailureReason()).hasSize(2000);
    }

    @Test
    void suggestReuse_recordsCandidate() {
        DossierDocument d = DossierDocument.planned("p", NormDocKind.POLICY, "Pol", oneSection());
        UUID candidate = UUID.randomUUID();
        d.suggestReuse(candidate);
        assertThat(d.getReuseSuggestedNormDocId()).isEqualTo(candidate);
    }

    @Test
    void requires_nonBlankKey() {
        assertThatThrownBy(() -> DossierDocument.planned(" ", NormDocKind.MANUAL, "M", oneSection()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requires_atLeastOneSection() {
        assertThatThrownBy(() -> DossierDocument.planned("k", NormDocKind.MANUAL, "M", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one section");
    }

    @Test
    void requires_nonNullKind() {
        assertThatThrownBy(() -> DossierDocument.planned("k", null, "M", oneSection()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sectionPlan_normalizesNulls() {
        SectionPlan s = new SectionPlan("k", "t", null, null);
        assertThat(s.clauses()).isEmpty();
        assertThat(s.guidance()).isEmpty();
    }

    @Test
    void sectionPlan_requiresTitle() {
        assertThatThrownBy(() -> new SectionPlan("k", " ", List.of(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
