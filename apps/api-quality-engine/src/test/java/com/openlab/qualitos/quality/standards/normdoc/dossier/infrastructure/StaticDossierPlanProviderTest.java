package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticDossierPlanProviderTest {

    private final StaticDossierPlanProvider provider = new StaticDossierPlanProvider();

    @Test
    void defaultPlan_containsManualPolicyAndProcedures() {
        DossierPlan plan = provider.planFor(List.of());
        assertThat(plan.documents()).hasSize(6);
        assertThat(plan.documents()).extracting(DossierDocument::getKind)
                .contains(NormDocKind.MANUAL, NormDocKind.POLICY, NormDocKind.PROCEDURE);
        // Manuel multi-sections (HLS §4 à §10)
        DossierDocument manual = plan.documents().stream()
                .filter(d -> d.getKind() == NormDocKind.MANUAL).findFirst().orElseThrow();
        assertThat(manual.getSections()).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void selection_returnsRequestedOnly_inOrder() {
        DossierPlan plan = provider.planFor(List.of("proc-audit-interne", "manuel-qualite"));
        assertThat(plan.documents()).extracting(DossierDocument::getKey)
                .containsExactly("proc-audit-interne", "manuel-qualite");
    }

    @Test
    void selection_unknownKey_throws() {
        assertThatThrownBy(() -> provider.planFor(List.of("nope")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void catalog_listsAllPlannableDocuments() {
        assertThat(provider.catalog()).hasSize(6);
        assertThat(provider.catalog()).extracting(DossierDocument::getKey)
                .contains("manuel-qualite", "politique-qualite",
                        "proc-maitrise-documents", "proc-audit-interne",
                        "proc-actions-correctives", "proc-revue-direction");
    }
}
