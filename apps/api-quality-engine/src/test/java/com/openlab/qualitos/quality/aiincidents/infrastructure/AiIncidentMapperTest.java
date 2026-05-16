package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiIncidentMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant OCC = Instant.parse("2026-05-16T08:00:00Z");
    static final Instant DET = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant NOW = Instant.parse("2026-05-16T11:00:00Z");

    @Test
    void roundtrip_detected() throws Exception {
        AiIncident i = AiIncident.detect(T, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", "persons", "actions", OCC, DET, U, NOW);
        AiIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getStatus().name()).isEqualTo("DETECTED");
        AiIncident back = invokeToDomain(e);
        assertThat(back.getAiSystemId()).isEqualTo(SYS);
        assertThat(back.getSeverity())
                .isEqualTo(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThat(back.regulatorNotificationDueAt())
                .isEqualTo(DET.plusSeconds(15L * 86400));
    }

    @Test
    void roundtrip_notified_preservesEvidence() throws Exception {
        AiIncident i = AiIncident.detect(T, "REF-2", SYS,
                AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH,
                "d", null, null, OCC, DET, U, NOW);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "root cause", "actions", NOW);
        AiIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getRegulatorReference()).isEqualTo("REG-1");
        AiIncident back = invokeToDomain(e);
        assertThat(back.isNotifiedRegulator()).isTrue();
        assertThat(back.getRootCauseAnalysis()).isEqualTo("root cause");
        assertThat(back.getInvestigationLeadUserId()).isEqualTo(LEAD);
    }

    @Test
    void roundtrip_closed_preservesActions() throws Exception {
        AiIncident i = AiIncident.detect(T, "REF-3", SYS,
                AiIncidentSeverity.SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE,
                "d", null, null, OCC, DET, U, NOW);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", null, NOW);
        i.close("actions taken", NOW);
        AiIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getCorrectiveActions()).isEqualTo("actions taken");
        AiIncident back = invokeToDomain(e);
        assertThat(back.isClosed()).isTrue();
    }

    @Test
    void roundtrip_dismissed_preservesReason() throws Exception {
        AiIncident i = AiIncident.detect(T, "REF-4", SYS,
                AiIncidentSeverity.SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS,
                "d", null, null, OCC, DET, U, NOW);
        i.dismiss("false alarm", NOW);
        AiIncidentJpaEntity e = invokeToEntity(i, null);
        assertThat(e.getDismissalReason()).isEqualTo("false alarm");
        AiIncident back = invokeToDomain(e);
        assertThat(back.isDismissed()).isTrue();
    }

    @Test
    void toEntity_existingTarget_reused() throws Exception {
        AiIncident i = AiIncident.detect(T, "REF-5", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, OCC, DET, U, NOW);
        AiIncidentJpaEntity target = new AiIncidentJpaEntity();
        AiIncidentJpaEntity e = invokeToEntity(i, target);
        assertThat(e).isSameAs(target);
    }

    private static AiIncidentJpaEntity invokeToEntity(
            AiIncident i, AiIncidentJpaEntity t) throws Exception {
        Method m = AiIncidentMapper.class.getDeclaredMethod(
                "toEntity", AiIncident.class, AiIncidentJpaEntity.class);
        m.setAccessible(true);
        return (AiIncidentJpaEntity) m.invoke(null, i, t);
    }

    private static AiIncident invokeToDomain(AiIncidentJpaEntity e) throws Exception {
        Method m = AiIncidentMapper.class.getDeclaredMethod(
                "toDomain", AiIncidentJpaEntity.class);
        m.setAccessible(true);
        return (AiIncident) m.invoke(null, e);
    }
}
