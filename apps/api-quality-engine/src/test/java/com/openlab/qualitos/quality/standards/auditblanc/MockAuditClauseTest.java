package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Value object — clause à risque + état de preuve (§8.4 onglet 7). */
class MockAuditClauseTest {

    private static MockAuditClause clause(ObligationLevel obligation, RiskLevel risk,
                                          int total, int covered) {
        return new MockAuditClause("8.1", "Maîtrise opérationnelle",
                obligation, risk, total, covered, null);
    }

    @Test
    void coverageRatio_andFullyCovered() {
        assertThat(clause(ObligationLevel.MUST, RiskLevel.HIGH, 4, 1).coverageRatio())
                .isEqualTo(0.25);
        assertThat(clause(ObligationLevel.MUST, RiskLevel.HIGH, 4, 1).fullyCovered()).isFalse();
        assertThat(clause(ObligationLevel.MUST, RiskLevel.HIGH, 4, 4).fullyCovered()).isTrue();
    }

    @Test
    void coverageRatio_zeroTotal_isZero_andNotFullyCovered() {
        MockAuditClause c = clause(ObligationLevel.MUST, RiskLevel.HIGH, 0, 0);
        assertThat(c.coverageRatio()).isZero();
        assertThat(c.fullyCovered()).isFalse();
    }

    @Test
    void nullRisk_defaultsToMedium() {
        MockAuditClause c = new MockAuditClause("4.1", "Contexte",
                ObligationLevel.MUST, null, 2, 0, List.of("DOCUMENT"));
        assertThat(c.risk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(c.evidenceTypes()).containsExactly("DOCUMENT");
    }

    @Test
    void blankOrNullCode_orTitle_rejected() {
        assertThatThrownBy(() -> new MockAuditClause(" ", "t",
                ObligationLevel.MUST, RiskLevel.LOW, 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new MockAuditClause(null, "t",
                ObligationLevel.MUST, RiskLevel.LOW, 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new MockAuditClause("4.1", " ",
                ObligationLevel.MUST, RiskLevel.LOW, 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> new MockAuditClause("4.1", null,
                ObligationLevel.MUST, RiskLevel.LOW, 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void negativeTotal_rejected() {
        assertThatThrownBy(() -> clause(ObligationLevel.MUST, RiskLevel.LOW, -1, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("totalRequirements");
    }

    @Test
    void coveredExceedsTotal_rejected() {
        assertThatThrownBy(() -> clause(ObligationLevel.MUST, RiskLevel.LOW, 2, 3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("coveredRequirements");
    }

    @Test
    void negativeCovered_rejected() {
        assertThatThrownBy(() -> clause(ObligationLevel.MUST, RiskLevel.LOW, 2, -1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("coveredRequirements");
    }

    @Test
    void nonBlankRisk_andEvidenceTypes_preserved() {
        MockAuditClause c = clause(ObligationLevel.MUST, RiskLevel.HIGH, 2, 1);
        assertThat(c.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(c.evidenceTypes()).isEmpty();
    }

    @Test
    void riskScore_mustCriticalUncovered_dominates() {
        double high = clause(ObligationLevel.MUST, RiskLevel.CRITICAL, 4, 0).riskScore();
        double low = clause(ObligationLevel.MAY, RiskLevel.LOW, 4, 4).riskScore();
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void riskScore_penalisesUncovered() {
        assertThat(clause(ObligationLevel.MUST, RiskLevel.HIGH, 4, 0).riskScore())
                .isGreaterThan(clause(ObligationLevel.MUST, RiskLevel.HIGH, 4, 4).riskScore());
    }

    @Test
    void criticality_grid() {
        assertThat(clause(ObligationLevel.MUST, RiskLevel.HIGH, 2, 0).criticality())
                .isEqualTo(MockAuditCriticality.MAJOR);
        assertThat(clause(ObligationLevel.MUST, RiskLevel.CRITICAL, 2, 1).criticality())
                .isEqualTo(MockAuditCriticality.MAJOR);
        assertThat(clause(ObligationLevel.MUST, RiskLevel.MEDIUM, 2, 0).criticality())
                .isEqualTo(MockAuditCriticality.MINOR);
        assertThat(clause(ObligationLevel.MUST, RiskLevel.LOW, 2, 0).criticality())
                .isEqualTo(MockAuditCriticality.MINOR);
        assertThat(clause(ObligationLevel.SHOULD, RiskLevel.CRITICAL, 2, 0).criticality())
                .isEqualTo(MockAuditCriticality.OBSERVATION);
        assertThat(clause(ObligationLevel.MUST, RiskLevel.CRITICAL, 2, 2).criticality())
                .isEqualTo(MockAuditCriticality.OBSERVATION);
    }

    @Test
    void riskScore_coversAllWeights() {
        // Couvre tous les bras des switch obligation/risk.
        for (ObligationLevel o : ObligationLevel.values()) {
            for (RiskLevel r : RiskLevel.values()) {
                assertThat(clause(o, r, 2, 1).riskScore()).isPositive();
            }
        }
    }
}
