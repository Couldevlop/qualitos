package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.domain.Art22LawfulBasis;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutomatedDecisionMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptySets_csvFieldsNull() throws Exception {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-1",
                "N", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, U, NOW);
        AutomatedDecisionJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getInputDataCategoriesCsv()).isNull();
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();

        AutomatedDecisionRecord back = invokeToDomain(e);
        assertThat(back.getInputDataCategories()).isEmpty();
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void roundtrip_filledSets_csvPopulated() throws Exception {
        UUID activity = UUID.randomUUID();
        UUID dpia = UUID.randomUUID();
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-2",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                Art22LawfulBasis.EXPLICIT_CONSENT, null,
                Set.of("payment-history", "credit-score"), Set.of(activity), dpia,
                "algo", "high", "human review", "object", U, NOW);
        AutomatedDecisionJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getInputDataCategoriesCsv()).contains("payment-history");
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(activity.toString());
        assertThat(e.getLinkedDpiaId()).isEqualTo(dpia);

        AutomatedDecisionRecord back = invokeToDomain(e);
        assertThat(back.getInputDataCategories())
                .containsExactlyInAnyOrder("payment-history", "credit-score");
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(activity);
        assertThat(back.getLinkedDpiaId()).isEqualTo(dpia);
        assertThat(back.getArt22LawfulBasis()).isEqualTo(Art22LawfulBasis.EXPLICIT_CONSENT);
    }

    @Test
    void roundtrip_updatesExistingEntity_inPlace() throws Exception {
        AutomatedDecisionJpaEntity existing = new AutomatedDecisionJpaEntity();
        UUID eid = UUID.randomUUID();
        existing.setId(eid);
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-3",
                "N", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, U, NOW);
        r.assignId(eid);
        AutomatedDecisionJpaEntity out = invokeToEntity(r, existing);
        assertThat(out).isSameAs(existing);
        assertThat(out.getId()).isEqualTo(eid);
    }

    private static AutomatedDecisionJpaEntity invokeToEntity(
            AutomatedDecisionRecord r, AutomatedDecisionJpaEntity target) throws Exception {
        Method m = AutomatedDecisionMapper.class.getDeclaredMethod(
                "toEntity", AutomatedDecisionRecord.class, AutomatedDecisionJpaEntity.class);
        m.setAccessible(true);
        return (AutomatedDecisionJpaEntity) m.invoke(null, r, target);
    }

    private static AutomatedDecisionRecord invokeToDomain(AutomatedDecisionJpaEntity e) throws Exception {
        Method m = AutomatedDecisionMapper.class.getDeclaredMethod(
                "toDomain", AutomatedDecisionJpaEntity.class);
        m.setAccessible(true);
        return (AutomatedDecisionRecord) m.invoke(null, e);
    }
}
