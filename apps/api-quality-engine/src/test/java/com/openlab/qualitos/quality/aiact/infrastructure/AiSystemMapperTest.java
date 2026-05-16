package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiSystemMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptyLinkedSets_csvNull() throws Exception {
        AiSystem s = AiSystem.draft(T, "REF-1", "Name", null, null, "purpose",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, null, null,
                null, Set.of(), Set.of(), U, NOW);
        AiSystemJpaEntity e = invokeToEntity(s, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();
        assertThat(e.getLinkedAutomatedDecisionIdsCsv()).isNull();
        AiSystem back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(back.getLinkedAutomatedDecisionIds()).isEmpty();
    }

    @Test
    void roundtrip_filledLinkedSets_csvPopulated() throws Exception {
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        AiSystem s = AiSystem.draft(T, "REF-2", "Name", "desc", "Provider", "purpose",
                AiRiskClassification.HIGH, AiSystemRole.DEPLOYER, true,
                "https://e.example.com/x", "CE-42",
                "oversight", "transparency", "data gov",
                UUID.randomUUID(), Set.of(a1), Set.of(a2), U, NOW);
        AiSystemJpaEntity e = invokeToEntity(s, null);
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(a1.toString());
        assertThat(e.getLinkedAutomatedDecisionIdsCsv()).contains(a2.toString());
        assertThat(e.isGeneralPurpose()).isTrue();
        assertThat(e.getCeMarkingNumber()).isEqualTo("CE-42");

        AiSystem back = invokeToDomain(e);
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(a1);
        assertThat(back.getLinkedAutomatedDecisionIds()).containsExactly(a2);
        assertThat(back.isGeneralPurpose()).isTrue();
        assertThat(back.getDataGovernanceNotes()).isEqualTo("data gov");
    }

    @Test
    void roundtrip_inUseSystem_preservesTimestamps() throws Exception {
        AiSystem s = AiSystem.draft(T, "REF-3", "n", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, U, NOW);
        s.register(NOW);
        s.putInUse(NOW);
        AiSystemJpaEntity e = invokeToEntity(s, null);
        assertThat(e.getEffectiveFrom()).isEqualTo(NOW);
        AiSystem back = invokeToDomain(e);
        assertThat(back.getEffectiveFrom()).isEqualTo(NOW);
        assertThat(back.isInUse()).isTrue();
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        AiSystem s = AiSystem.draft(T, "REF-4", "n", null, null, "p",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, U, NOW);
        AiSystemJpaEntity target = new AiSystemJpaEntity();
        AiSystemJpaEntity e = invokeToEntity(s, target);
        assertThat(e).isSameAs(target);
    }

    private static AiSystemJpaEntity invokeToEntity(
            AiSystem s, AiSystemJpaEntity target) throws Exception {
        Method m = AiSystemMapper.class.getDeclaredMethod(
                "toEntity", AiSystem.class, AiSystemJpaEntity.class);
        m.setAccessible(true);
        return (AiSystemJpaEntity) m.invoke(null, s, target);
    }

    private static AiSystem invokeToDomain(AiSystemJpaEntity e) throws Exception {
        Method m = AiSystemMapper.class.getDeclaredMethod(
                "toDomain", AiSystemJpaEntity.class);
        m.setAccessible(true);
        return (AiSystem) m.invoke(null, e);
    }
}
