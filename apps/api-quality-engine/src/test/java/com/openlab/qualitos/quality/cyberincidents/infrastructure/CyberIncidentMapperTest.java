package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CyberIncidentMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptySets_csvFieldsNull() throws Exception {
        CyberIncident i = CyberIncident.detect(T, "CYB-1", "t", null, NOW, null,
                CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW,
                0L, Set.of(), Set.of(), null, U);
        CyberIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getAffectedAssetsCsv()).isNull();
        assertThat(e.getAffectedServicesCsv()).isNull();

        CyberIncident back = invokeToDomain(e);
        assertThat(back.getAffectedAssets()).isEmpty();
        assertThat(back.getAffectedServices()).isEmpty();
    }

    @Test
    void roundtrip_filledSets_csvPopulated() throws Exception {
        UUID breach = UUID.randomUUID();
        CyberIncident i = CyberIncident.detect(T, "CYB-2", "t", null, NOW, null,
                CyberIncidentType.RANSOMWARE, CyberIncidentSeverity.HIGH,
                500L, Set.of("file-server", "db-primary"), Set.of("storage"),
                breach, U);
        CyberIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getAffectedAssetsCsv()).contains("file-server").contains("db-primary");
        assertThat(e.getAffectedServicesCsv()).contains("storage");
        assertThat(e.getLinkedBreachId()).isEqualTo(breach);

        CyberIncident back = invokeToDomain(e);
        assertThat(back.getAffectedAssets()).containsExactlyInAnyOrder("file-server", "db-primary");
        assertThat(back.getAffectedServices()).containsExactly("storage");
        assertThat(back.getLinkedBreachId()).isEqualTo(breach);
    }

    @Test
    void roundtrip_closedIncident_preservesClosureMetadata() throws Exception {
        CyberIncident i = CyberIncident.detect(T, "CYB-3", "t", null, NOW, null,
                CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW,
                0L, Set.of(), Set.of(), null, U);
        i.startAssessment(U, NOW);
        i.mitigate("patched", "no impact", U, NOW);
        i.close("done", NOW.plusSeconds(60));
        CyberIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getStatus().name()).isEqualTo("CLOSED");
        assertThat(e.getClosureNotes()).isEqualTo("done");
        CyberIncident back = invokeToDomain(e);
        assertThat(back.isTerminal()).isTrue();
        assertThat(back.getClosedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    private static CyberIncidentJpaEntity invokeToEntity(
            CyberIncident i, CyberIncidentJpaEntity target) throws Exception {
        Method m = CyberIncidentMapper.class.getDeclaredMethod(
                "toEntity", CyberIncident.class, CyberIncidentJpaEntity.class);
        m.setAccessible(true);
        return (CyberIncidentJpaEntity) m.invoke(null, i, target);
    }

    private static CyberIncident invokeToDomain(CyberIncidentJpaEntity e) throws Exception {
        Method m = CyberIncidentMapper.class.getDeclaredMethod(
                "toDomain", CyberIncidentJpaEntity.class);
        m.setAccessible(true);
        return (CyberIncident) m.invoke(null, e);
    }
}
