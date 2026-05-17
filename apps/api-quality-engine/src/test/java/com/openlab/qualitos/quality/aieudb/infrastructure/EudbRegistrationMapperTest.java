package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EudbRegistrationMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");

    @Test
    void roundtrip_draft() throws Exception {
        EudbRegistration r = EudbRegistration.draft(T, "REF-1", SYS,
                "Acme", "EU Rep", "FR", "purpose", "doc", U, NOW);
        EudbRegistrationJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getStatus().name()).isEqualTo("DRAFT");
        assertThat(e.getMemberStateOfReference()).isEqualTo("FR");
        EudbRegistration back = invokeToDomain(e);
        assertThat(back.getAiSystemId()).isEqualTo(SYS);
        assertThat(back.getProviderEuRepresentative()).isEqualTo("EU Rep");
    }

    @Test
    void roundtrip_registered_preservesEudbId() throws Exception {
        EudbRegistration r = EudbRegistration.draft(T, "REF-2", SYS,
                "Acme", null, "DE", "purpose", null, U, NOW);
        r.submit(U, NOW);
        r.markRegistered("EUDB-AI-ZYX987", NOW, NOW);
        EudbRegistrationJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getEudbId()).isEqualTo("EUDB-AI-ZYX987");
        assertThat(e.getRegistrationDate()).isEqualTo(NOW);
        EudbRegistration back = invokeToDomain(e);
        assertThat(back.isRegistered()).isTrue();
        assertThat(back.getSubmittedByUserId()).isEqualTo(U);
    }

    @Test
    void roundtrip_updated_preservesUpdateMetadata() throws Exception {
        EudbRegistration r = EudbRegistration.draft(T, "REF-3", SYS,
                "Acme", null, "ES", "purpose", null, U, NOW);
        r.submit(U, NOW);
        r.markRegistered("EUDB-AI-AAA111", NOW, NOW);
        Instant upd = NOW.plusSeconds(86400);
        r.declareUpdate("model retrained", upd, upd);
        EudbRegistrationJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getLastUpdateSummary()).isEqualTo("model retrained");
        EudbRegistration back = invokeToDomain(e);
        assertThat(back.isUpdated()).isTrue();
        assertThat(back.getLastUpdateDate()).isEqualTo(upd);
    }

    @Test
    void roundtrip_rejected_preservesReason() throws Exception {
        EudbRegistration r = EudbRegistration.draft(T, "REF-4", SYS,
                "Acme", null, "IT", "purpose", null, U, NOW);
        r.reject("incomplete", NOW);
        EudbRegistrationJpaEntity e = invokeToEntity(r, null);
        assertThat(e.getRejectionReason()).isEqualTo("incomplete");
        EudbRegistration back = invokeToDomain(e);
        assertThat(back.getStatus().name()).isEqualTo("REJECTED");
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        EudbRegistration r = EudbRegistration.draft(T, "REF-5", SYS,
                "Acme", null, "FR", "purpose", null, U, NOW);
        EudbRegistrationJpaEntity target = new EudbRegistrationJpaEntity();
        EudbRegistrationJpaEntity e = invokeToEntity(r, target);
        assertThat(e).isSameAs(target);
    }

    private static EudbRegistrationJpaEntity invokeToEntity(
            EudbRegistration r, EudbRegistrationJpaEntity t) throws Exception {
        Method m = EudbRegistrationMapper.class.getDeclaredMethod(
                "toEntity", EudbRegistration.class, EudbRegistrationJpaEntity.class);
        m.setAccessible(true);
        return (EudbRegistrationJpaEntity) m.invoke(null, r, t);
    }

    private static EudbRegistration invokeToDomain(EudbRegistrationJpaEntity e) throws Exception {
        Method m = EudbRegistrationMapper.class.getDeclaredMethod(
                "toDomain", EudbRegistrationJpaEntity.class);
        m.setAccessible(true);
        return (EudbRegistration) m.invoke(null, e);
    }
}
