package com.openlab.qualitos.quality.aiact.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSystemTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(3600);

    @Test
    void draft_initialStateIsDraft() {
        AiSystem s = draft(AiRiskClassification.LIMITED);
        assertThat(s.isDraft()).isTrue();
        assertThat(s.getStatus()).isEqualTo(AiSystemStatus.DRAFT);
        assertThat(s.getEffectiveFrom()).isNull();
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> AiSystem.draft(T, "lowercase", "Name", null, null,
                "purpose", AiRiskClassification.MINIMAL_OR_NO,
                AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankName_throws() {
        assertThatThrownBy(() -> AiSystem.draft(T, "REF-1", " ", null, null,
                "purpose", AiRiskClassification.MINIMAL_OR_NO,
                AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidUrl_throws() {
        assertThatThrownBy(() -> AiSystem.draft(T, "REF-1", "Name", null, null,
                "purpose", AiRiskClassification.HIGH, AiSystemRole.PROVIDER, false,
                "not-a-url", null, "oversight", "transparency", null,
                null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editDraft_changesFields() {
        AiSystem s = draft(AiRiskClassification.LIMITED);
        s.editDraft("New name", "desc", "Provider", "new purpose",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.DEPLOYER, true,
                null, "CE-1", null, null, "data gov",
                UUID.randomUUID(), Set.of(UUID.randomUUID()), Set.of(), LATER);
        assertThat(s.getName()).isEqualTo("New name");
        assertThat(s.getRole()).isEqualTo(AiSystemRole.DEPLOYER);
        assertThat(s.isGeneralPurpose()).isTrue();
        assertThat(s.getCeMarkingNumber()).isEqualTo("CE-1");
        assertThat(s.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void editDraft_notDraft_throws() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        assertThatThrownBy(() -> s.editDraft("X", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void register_minimalRisk_ok() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        assertThat(s.getStatus()).isEqualTo(AiSystemStatus.REGISTERED);
    }

    @Test
    void register_unacceptable_throws() {
        AiSystem s = draft(AiRiskClassification.UNACCEPTABLE);
        assertThatThrownBy(() -> s.register(LATER))
                .isInstanceOf(AiSystemStateException.class)
                .hasMessageContaining("UNACCEPTABLE");
    }

    @Test
    void register_alreadyRegistered_throws() {
        AiSystem s = draft(AiRiskClassification.LIMITED);
        s.register(LATER);
        assertThatThrownBy(() -> s.register(LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void putInUse_high_requiresConformityAndOversight() {
        AiSystem s = draft(AiRiskClassification.HIGH);
        s.register(LATER);
        assertThatThrownBy(() -> s.putInUse(LATER))
                .isInstanceOf(AiSystemStateException.class)
                .hasMessageContaining("HIGH");
    }

    @Test
    void putInUse_high_withAllEvidence_ok() {
        AiSystem s = AiSystem.draft(T, "REF-HIGH", "High system", null, null,
                "purpose", AiRiskClassification.HIGH, AiSystemRole.PROVIDER, false,
                "https://evidence.example.com/file.pdf", "CE-1",
                "human review every output", "transparency notice",
                null, null, null, null, U, NOW);
        s.register(LATER);
        s.putInUse(LATER);
        assertThat(s.isInUse()).isTrue();
        assertThat(s.getEffectiveFrom()).isEqualTo(LATER);
    }

    @Test
    void putInUse_high_missingOversight_throws() {
        AiSystem s = AiSystem.draft(T, "REF-H2", "H2", null, null, "p",
                AiRiskClassification.HIGH, AiSystemRole.PROVIDER, false,
                "https://e.example.com/x", null, null, "transparency", null,
                null, null, null, U, NOW);
        s.register(LATER);
        assertThatThrownBy(() -> s.putInUse(LATER))
                .isInstanceOf(AiSystemStateException.class)
                .hasMessageContaining("humanOversightDescription");
    }

    @Test
    void putInUse_limited_requiresTransparency() {
        AiSystem s = draft(AiRiskClassification.LIMITED);
        s.register(LATER);
        assertThatThrownBy(() -> s.putInUse(LATER))
                .isInstanceOf(AiSystemStateException.class)
                .hasMessageContaining("transparencyMeasures");
    }

    @Test
    void putInUse_limited_withTransparency_ok() {
        AiSystem s = AiSystem.draft(T, "REF-L", "L", null, null, "p",
                AiRiskClassification.LIMITED, AiSystemRole.DEPLOYER, false,
                null, null, null, "Chatbot notice", null,
                null, null, null, U, NOW);
        s.register(LATER);
        s.putInUse(LATER);
        assertThat(s.isInUse()).isTrue();
    }

    @Test
    void putInUse_minimal_ok() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        s.putInUse(LATER);
        assertThat(s.isInUse()).isTrue();
    }

    @Test
    void putInUse_unacceptableImpossible_blockedAtRegister() {
        AiSystem s = draft(AiRiskClassification.UNACCEPTABLE);
        assertThatThrownBy(() -> s.register(LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void decommission_fromInUse_ok() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        s.putInUse(LATER);
        Instant end = LATER.plusSeconds(3600);
        s.decommission(end);
        assertThat(s.isTerminal()).isTrue();
        assertThat(s.getStatus()).isEqualTo(AiSystemStatus.DECOMMISSIONED);
        assertThat(s.getEffectiveTo()).isEqualTo(end);
    }

    @Test
    void decommission_fromDraft_throws() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        assertThatThrownBy(() -> s.decommission(LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void withdraw_fromDraft_ok() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.withdraw("changed plans", LATER);
        assertThat(s.getStatus()).isEqualTo(AiSystemStatus.WITHDRAWN);
        assertThat(s.getWithdrawalReason()).isEqualTo("changed plans");
        assertThat(s.getEffectiveTo()).isEqualTo(LATER);
    }

    @Test
    void withdraw_fromRegistered_ok() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        s.withdraw("reason", LATER);
        assertThat(s.isTerminal()).isTrue();
    }

    @Test
    void withdraw_fromInUse_notAllowed() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(LATER);
        s.putInUse(LATER);
        assertThatThrownBy(() -> s.withdraw("r", LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void withdraw_blankReason_throws() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        assertThatThrownBy(() -> s.withdraw(" ", LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void terminalStates_cannotTransition() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        s.withdraw("r", LATER);
        assertThatThrownBy(() -> s.register(LATER))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void assignId_setsIdentifier() {
        AiSystem s = draft(AiRiskClassification.LIMITED);
        UUID id = UUID.randomUUID();
        s.assignId(id);
        assertThat(s.getId()).isEqualTo(id);
    }

    @Test
    void sanitizeUrl_blankBecomesNull() {
        AiSystem s = AiSystem.draft(T, "REF-1", "n", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.PROVIDER, false,
                "  ", null, null, null, null,
                null, null, null, U, NOW);
        assertThat(s.getConformityAssessmentEvidenceUrl()).isNull();
    }

    @Test
    void sanitizeIds_nullBecomesEmptySet() {
        AiSystem s = draft(AiRiskClassification.MINIMAL_OR_NO);
        assertThat(s.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(s.getLinkedAutomatedDecisionIds()).isEmpty();
    }

    @Test
    void riskClassification_helpers() {
        assertThat(AiRiskClassification.UNACCEPTABLE.isProhibited()).isTrue();
        assertThat(AiRiskClassification.HIGH.requiresConformityAssessment()).isTrue();
        assertThat(AiRiskClassification.LIMITED.requiresTransparency()).isTrue();
        assertThat(AiRiskClassification.HIGH.requiresTransparency()).isTrue();
        assertThat(AiRiskClassification.MINIMAL_OR_NO.isProhibited()).isFalse();
        assertThat(AiRiskClassification.MINIMAL_OR_NO.requiresConformityAssessment()).isFalse();
        assertThat(AiRiskClassification.MINIMAL_OR_NO.requiresTransparency()).isFalse();
    }

    private static AiSystem draft(AiRiskClassification risk) {
        return AiSystem.draft(T, "REF-1", "Name", "desc",
                "Provider Co", "purpose",
                risk, AiSystemRole.PROVIDER, false,
                null, null, null, null, null,
                null, null, null, U, NOW);
    }
}
