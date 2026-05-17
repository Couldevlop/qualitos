package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiQmsMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID V = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_draft_emptyCovered_csvNull() throws Exception {
        AiQms q = AiQms.draft(T, "REF-1", "1.0", "n", "d",
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW);
        AiQmsJpaEntity e = invokeToEntity(q, null);
        assertThat(e.getCoveredAiSystemIdsCsv()).isNull();
        AiQms back = invokeToDomain(e);
        assertThat(back.getCoveredAiSystemIds()).isEmpty();
        assertThat(back.getVersion()).isEqualTo("1.0");
    }

    @Test
    void roundtrip_filledCovered_csvPopulated() throws Exception {
        UUID s2 = UUID.randomUUID();
        AiQms q = AiQms.draft(T, "REF-2", "2.1", "n", null,
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(SYS, s2), U, NOW);
        AiQmsJpaEntity e = invokeToEntity(q, null);
        assertThat(e.getCoveredAiSystemIdsCsv()).contains(SYS.toString());
        AiQms back = invokeToDomain(e);
        assertThat(back.getCoveredAiSystemIds()).containsExactlyInAnyOrder(SYS, s2);
    }

    @Test
    void roundtrip_inForce_preservesApproval() throws Exception {
        AiQms q = AiQms.draft(T, "REF-3", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW);
        q.approve(U, V, "ok", NOW);
        q.putInForce(NOW);
        AiQmsJpaEntity e = invokeToEntity(q, null);
        assertThat(e.getStatus().name()).isEqualTo("IN_FORCE");
        assertThat(e.getApprovedByUserId()).isEqualTo(V);
        AiQms back = invokeToDomain(e);
        assertThat(back.isInForce()).isTrue();
        assertThat(back.getEffectiveFrom()).isEqualTo(NOW);
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        AiQms q = AiQms.draft(T, "REF-4", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW);
        AiQmsJpaEntity target = new AiQmsJpaEntity();
        AiQmsJpaEntity e = invokeToEntity(q, target);
        assertThat(e).isSameAs(target);
    }

    private static AiQmsJpaEntity invokeToEntity(AiQms q, AiQmsJpaEntity t) throws Exception {
        Method m = AiQmsMapper.class.getDeclaredMethod(
                "toEntity", AiQms.class, AiQmsJpaEntity.class);
        m.setAccessible(true);
        return (AiQmsJpaEntity) m.invoke(null, q, t);
    }

    private static AiQms invokeToDomain(AiQmsJpaEntity e) throws Exception {
        Method m = AiQmsMapper.class.getDeclaredMethod("toDomain", AiQmsJpaEntity.class);
        m.setAccessible(true);
        return (AiQms) m.invoke(null, e);
    }
}
