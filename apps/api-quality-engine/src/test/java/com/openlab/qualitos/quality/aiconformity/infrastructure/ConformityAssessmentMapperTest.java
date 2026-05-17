package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConformityAssessmentMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID QMS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Instant VALID = NOW.plusSeconds(365L * 86400);

    @Test
    void roundtrip_planned_internal() throws Exception {
        ConformityAssessment a = ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", U, NOW);
        ConformityAssessmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getStatus().name()).isEqualTo("PLANNED");
        assertThat(e.getProcedure()).isEqualTo(ConformityProcedure.INTERNAL_CONTROL);
        ConformityAssessment back = invokeToDomain(e);
        assertThat(back.getQmsId()).isEqualTo(QMS);
        assertThat(back.getNotifiedBodyId()).isNull();
    }

    @Test
    void roundtrip_certified_preservesEvidence() throws Exception {
        ConformityAssessment a = ConformityAssessment.plan(T, "REF-2", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, "1234", "TUV", "scope", U, NOW);
        a.start(NOW);
        a.certify("CERT-001", "EU-DECL-001", VALID, NOW);
        ConformityAssessmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getCertificateNumber()).isEqualTo("CERT-001");
        assertThat(e.getValidUntil()).isEqualTo(VALID);
        assertThat(e.getNotifiedBodyId()).isEqualTo("1234");
        ConformityAssessment back = invokeToDomain(e);
        assertThat(back.isCertified()).isTrue();
        assertThat(back.getEuDeclarationReference()).isEqualTo("EU-DECL-001");
    }

    @Test
    void roundtrip_revoked_preservesReason() throws Exception {
        ConformityAssessment a = ConformityAssessment.plan(T, "REF-3", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", U, NOW);
        a.revoke("cancelled", NOW);
        ConformityAssessmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getRevocationReason()).isEqualTo("cancelled");
        ConformityAssessment back = invokeToDomain(e);
        assertThat(back.isTerminal()).isTrue();
    }

    @Test
    void roundtrip_failed_preservesReason() throws Exception {
        ConformityAssessment a = ConformityAssessment.plan(T, "REF-4", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, "1234", "TUV", "scope", U, NOW);
        a.start(NOW);
        a.markFailed("non-conformities", NOW);
        ConformityAssessmentJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getFailureReason()).isEqualTo("non-conformities");
        ConformityAssessment back = invokeToDomain(e);
        assertThat(back.isTerminal()).isTrue();
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        ConformityAssessment a = ConformityAssessment.plan(T, "REF-5", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", U, NOW);
        ConformityAssessmentJpaEntity target = new ConformityAssessmentJpaEntity();
        ConformityAssessmentJpaEntity e = invokeToEntity(a, target);
        assertThat(e).isSameAs(target);
    }

    private static ConformityAssessmentJpaEntity invokeToEntity(
            ConformityAssessment a, ConformityAssessmentJpaEntity t) throws Exception {
        Method m = ConformityAssessmentMapper.class.getDeclaredMethod(
                "toEntity", ConformityAssessment.class, ConformityAssessmentJpaEntity.class);
        m.setAccessible(true);
        return (ConformityAssessmentJpaEntity) m.invoke(null, a, t);
    }

    private static ConformityAssessment invokeToDomain(
            ConformityAssessmentJpaEntity e) throws Exception {
        Method m = ConformityAssessmentMapper.class.getDeclaredMethod(
                "toDomain", ConformityAssessmentJpaEntity.class);
        m.setAccessible(true);
        return (ConformityAssessment) m.invoke(null, e);
    }
}
