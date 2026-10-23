package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrFixtures;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** La trace de chaque écriture R&amp;R : l'action, la ressource, et aucune donnée personnelle. */
class AuditLogGageRrPublisherTest {

    final AuditEventService events = mock(AuditEventService.class);
    final AuditLogGageRrPublisher publisher = new AuditLogGageRrPublisher(events);
    final UUID tenant = UUID.randomUUID();
    final UUID acteur = UUID.randomUUID();

    GageRrStudy etude() {
        GageRrStudy e = GageRrStudy.record(tenant, GageRrFixtures.workbookDetails(), acteur, Instant.EPOCH);
        return new GageRrStudy(UUID.randomUUID(), tenant, e.getPartReference(), e.getCharacteristic(),
                e.getInstrument(), e.getLsl(), e.getUsl(), e.getOperatorCount(), e.getPartCount(),
                e.getTrialCount(), e.getSigmaMultiplier(), e.getStudyDate(), e.getResponsibleUserId(),
                e.getResponsibleName(), e.getOperators(), e.getMeasurements(), e.getGrrPercent(),
                e.getGrrTolerancePercent(), e.getNdc(), e.getDecision(), e.getAnovaGrrPercent(), e.getAnovaNdc(),
                acteur, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void chaqueEcritureLaisseSaTraceSansDonneePersonnelle() {
        GageRrStudy e = etude();
        publisher.recorded(e, acteur);
        publisher.revised(e, acteur);
        publisher.deleted(e, acteur);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> traces =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(3)).recordForTenant(eq(tenant), traces.capture());
        List<AuditEventDto.RecordEventRequest> t = traces.getAllValues();
        assertThat(t).extracting(AuditEventDto.RecordEventRequest::action).containsExactly(
                "spc.gage-rr.recorded", "spc.gage-rr.revised", "spc.gage-rr.deleted");
        assertThat(t).allSatisfy(r -> {
            assertThat(r.actorType()).isEqualTo(ActorType.USER);
            assertThat(r.actorUserId()).isEqualTo(acteur);
            assertThat(r.resourceType()).isEqualTo("spc-gage-rr");
            assertThat(r.resourceId()).isEqualTo(e.getId());
            assertThat(r.payloadJson()).isEqualTo("{\"operators\":3,\"parts\":10,\"trials\":3,\"sigmaMultiplier\":5.15,"
                    + "\"grrPercent\":21.0,\"anovaGrrPercent\":21.16,\"ndc\":6,\"decision\":\"CONDITIONAL\"}");
            assertThat(r.payloadJson()).doesNotContain("Alice", "Leroy", "9.955");
        });
    }
}
