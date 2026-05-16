package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PmmPlanMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID REV = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_draft() throws Exception {
        PmmPlan p = PmmPlan.draft(T, "REF-1", SYS, "n", "d",
                "metrics", "method", PmmReviewFrequency.MONTHLY,
                "resp", "trigger", "qms-1", U, NOW);
        PmmPlanJpaEntity e = invokeToEntity(p, null);
        assertThat(e.getStatus().name()).isEqualTo("DRAFT");
        PmmPlan back = invokeToDomain(e);
        assertThat(back.getAiSystemId()).isEqualTo(SYS);
        assertThat(back.getReviewFrequency()).isEqualTo(PmmReviewFrequency.MONTHLY);
        assertThat(back.getQmsLinkReference()).isEqualTo("qms-1");
    }

    @Test
    void roundtrip_active_preservesActivation() throws Exception {
        PmmPlan p = PmmPlan.draft(T, "REF-2", SYS, "n", null,
                "m", "c", PmmReviewFrequency.WEEKLY, null, null, null, U, NOW);
        p.activate(NOW);
        p.recordReview(REV, NOW);
        PmmPlanJpaEntity e = invokeToEntity(p, null);
        assertThat(e.getActivatedAt()).isEqualTo(NOW);
        assertThat(e.getLastReviewedByUserId()).isEqualTo(REV);
        PmmPlan back = invokeToDomain(e);
        assertThat(back.isActive()).isTrue();
        assertThat(back.getLastReviewedByUserId()).isEqualTo(REV);
    }

    @Test
    void roundtrip_closed_preservesEffectiveTo() throws Exception {
        PmmPlan p = PmmPlan.draft(T, "REF-3", SYS, "n", null,
                "m", "c", PmmReviewFrequency.ANNUAL, null, null, null, U, NOW);
        p.activate(NOW);
        Instant end = NOW.plusSeconds(3600);
        p.close("EoL", end);
        PmmPlanJpaEntity e = invokeToEntity(p, null);
        assertThat(e.getEffectiveTo()).isEqualTo(end);
        PmmPlan back = invokeToDomain(e);
        assertThat(back.isClosed()).isTrue();
        assertThat(back.getClosureReason()).isEqualTo("EoL");
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        PmmPlan p = PmmPlan.draft(T, "REF-4", SYS, "n", null,
                null, null, null, null, null, null, U, NOW);
        PmmPlanJpaEntity target = new PmmPlanJpaEntity();
        PmmPlanJpaEntity e = invokeToEntity(p, target);
        assertThat(e).isSameAs(target);
    }

    private static PmmPlanJpaEntity invokeToEntity(PmmPlan p, PmmPlanJpaEntity t) throws Exception {
        Method m = PmmPlanMapper.class.getDeclaredMethod(
                "toEntity", PmmPlan.class, PmmPlanJpaEntity.class);
        m.setAccessible(true);
        return (PmmPlanJpaEntity) m.invoke(null, p, t);
    }

    private static PmmPlan invokeToDomain(PmmPlanJpaEntity e) throws Exception {
        Method m = PmmPlanMapper.class.getDeclaredMethod("toDomain", PmmPlanJpaEntity.class);
        m.setAccessible(true);
        return (PmmPlan) m.invoke(null, e);
    }
}
