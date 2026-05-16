package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Nis2RiskMeasureMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptySets_csvNull() throws Exception {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW);
        Nis2RiskMeasureJpaEntity e = invokeToEntity(m, null);
        assertThat(e.getEvidenceUrlsCsv()).isNull();
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();

        Nis2RiskMeasure back = invokeToDomain(e);
        assertThat(back.getEvidenceUrls()).isEmpty();
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void roundtrip_filledSets_csvPopulated() throws Exception {
        UUID act = UUID.randomUUID();
        UUID dpa = UUID.randomUUID();
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-2",
                Nis2MeasureCategory.MFA_AND_COMMUNICATIONS, "t", null,
                U, 4, ResidualRiskRating.MEDIUM, null, 180,
                Set.of("https://x.com/e.pdf", "https://y.com/policy.html"),
                Set.of(act), Set.of(dpa), null, U, NOW);
        Nis2RiskMeasureJpaEntity e = invokeToEntity(m, null);
        assertThat(e.getEvidenceUrlsCsv()).contains("https://x.com/e.pdf");
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(act.toString());
        assertThat(e.getLinkedProcessorAgreementIdsCsv()).contains(dpa.toString());

        Nis2RiskMeasure back = invokeToDomain(e);
        assertThat(back.getEvidenceUrls()).hasSize(2);
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(act);
        assertThat(back.getLinkedProcessorAgreementIds()).containsExactly(dpa);
    }

    @Test
    void roundtrip_verifiedMeasure_metadataPreserved() throws Exception {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-3",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 4, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW);
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(U, NOW.plusSeconds(60), NOW.plusSeconds(60));
        Nis2RiskMeasureJpaEntity e = invokeToEntity(m, null);
        assertThat(e.getStatus().name()).isEqualTo("VERIFIED");
        assertThat(e.getLastReviewedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(e.getNextReviewDueAt()).isNotNull();
        Nis2RiskMeasure back = invokeToDomain(e);
        assertThat(back.isVerified()).isTrue();
    }

    private static Nis2RiskMeasureJpaEntity invokeToEntity(
            Nis2RiskMeasure m, Nis2RiskMeasureJpaEntity target) throws Exception {
        Method method = Nis2RiskMeasureMapper.class.getDeclaredMethod(
                "toEntity", Nis2RiskMeasure.class, Nis2RiskMeasureJpaEntity.class);
        method.setAccessible(true);
        return (Nis2RiskMeasureJpaEntity) method.invoke(null, m, target);
    }

    private static Nis2RiskMeasure invokeToDomain(Nis2RiskMeasureJpaEntity e) throws Exception {
        Method method = Nis2RiskMeasureMapper.class.getDeclaredMethod(
                "toDomain", Nis2RiskMeasureJpaEntity.class);
        method.setAccessible(true);
        return (Nis2RiskMeasure) method.invoke(null, e);
    }
}
