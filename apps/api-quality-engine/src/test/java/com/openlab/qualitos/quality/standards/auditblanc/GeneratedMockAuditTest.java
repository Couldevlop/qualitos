package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.auditblanc.domain.GeneratedMockAudit;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Résultat brut de la génération IA (§8.4 onglet 7). */
class GeneratedMockAuditTest {

    @Test
    void nullArguments_defaultToEmpty() {
        GeneratedMockAudit g = new GeneratedMockAudit(null, null, 0d, null);
        assertThat(g.questions()).isEmpty();
        assertThat(g.aiFindings()).isEmpty();
        assertThat(g.provider()).isEmpty();
        assertThat(g.readiness()).isZero();
    }

    @Test
    void carriesValues() {
        GeneratedMockAudit g = new GeneratedMockAudit(
                List.of(new MockAuditQuestion("8.1", "q", null)),
                Map.of("8.1", "constat"), 75d, "ollama");
        assertThat(g.questions()).hasSize(1);
        assertThat(g.aiFindings()).containsEntry("8.1", "constat");
        assertThat(g.readiness()).isEqualTo(75d);
        assertThat(g.provider()).isEqualTo("ollama");
    }
}
