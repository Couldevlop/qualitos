package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FriaMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID V = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_draft_preservesFields() throws Exception {
        Fria f = Fria.draft(T, "REF-1", SYS, "process", "1 year",
                "categories", "risks", null, null, null, U, NOW);
        FriaJpaEntity e = invokeToEntity(f, null);
        assertThat(e.getReference()).isEqualTo("REF-1");
        assertThat(e.getStatus().name()).isEqualTo("DRAFT");
        Fria back = invokeToDomain(e);
        assertThat(back.getAiSystemId()).isEqualTo(SYS);
        assertThat(back.getDeploymentDurationDescription()).isEqualTo("1 year");
    }

    @Test
    void roundtrip_approved_preservesActors() throws Exception {
        Fria f = Fria.draft(T, "REF-2", SYS, "process", null, "cat", "risks",
                "mitigation", "oversight", "complaint", U, NOW);
        f.submit(U, NOW);
        f.approve(V, "notes", NOW);
        FriaJpaEntity e = invokeToEntity(f, null);
        assertThat(e.getStatus().name()).isEqualTo("APPROVED");
        assertThat(e.getApprovedByUserId()).isEqualTo(V);
        assertThat(e.getSubmittedByUserId()).isEqualTo(U);
        Fria back = invokeToDomain(e);
        assertThat(back.isApproved()).isTrue();
        assertThat(back.getApprovalNotes()).isEqualTo("notes");
    }

    @Test
    void roundtrip_archived_preservesEffectiveTo() throws Exception {
        Fria f = Fria.draft(T, "REF-3", SYS, "p", null, "c", "r",
                "m", "o", "complaint", U, NOW);
        f.submit(U, NOW);
        f.approve(V, null, NOW);
        Instant end = NOW.plusSeconds(86400);
        f.archive("decommissioned", end);
        FriaJpaEntity e = invokeToEntity(f, null);
        assertThat(e.getEffectiveTo()).isEqualTo(end);
        Fria back = invokeToDomain(e);
        assertThat(back.isArchived()).isTrue();
        assertThat(back.getArchivedReason()).isEqualTo("decommissioned");
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        Fria f = Fria.draft(T, "REF-4", SYS, "p", null, "c", "r",
                null, null, null, U, NOW);
        FriaJpaEntity target = new FriaJpaEntity();
        FriaJpaEntity e = invokeToEntity(f, target);
        assertThat(e).isSameAs(target);
    }

    private static FriaJpaEntity invokeToEntity(Fria f, FriaJpaEntity target) throws Exception {
        Method m = FriaMapper.class.getDeclaredMethod(
                "toEntity", Fria.class, FriaJpaEntity.class);
        m.setAccessible(true);
        return (FriaJpaEntity) m.invoke(null, f, target);
    }

    private static Fria invokeToDomain(FriaJpaEntity e) throws Exception {
        Method m = FriaMapper.class.getDeclaredMethod("toDomain", FriaJpaEntity.class);
        m.setAccessible(true);
        return (Fria) m.invoke(null, e);
    }
}
