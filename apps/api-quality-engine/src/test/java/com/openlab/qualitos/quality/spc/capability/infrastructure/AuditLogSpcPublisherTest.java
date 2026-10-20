package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;
import com.openlab.qualitos.quality.spc.capability.domain.SpcFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** La trace de chaque écriture SPC : l'action, la ressource, et aucune donnée personnelle. */
class AuditLogSpcPublisherTest {

    final AuditEventService events = mock(AuditEventService.class);
    final AuditLogSpcPublisher publisher = new AuditLogSpcPublisher(events);
    final UUID tenant = UUID.randomUUID();
    final UUID acteur = UUID.randomUUID();

    CapabilityStudy etude() {
        CapabilityStudy e = CapabilityStudy.record(tenant, SpcFixtures.goldenDetails(),
                CapabilityThresholds.DEFAULT, acteur, Instant.EPOCH);
        return new CapabilityStudy(UUID.randomUUID(), tenant, e.getPartReference(), e.getCharacteristic(),
                e.getSpecification(), e.getMachine(), e.getLeaderUserId(), e.getLeaderName(), e.getStudyDate(),
                e.isActionPlan(), e.getLsl(), e.getUsl(), e.getSubgroupSize(), e.getMeasurements(), e.getCp(),
                e.getCpk(), e.getThresholds(), acteur, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void chaqueEcritureDEtudeLaisseSaTrace() {
        CapabilityStudy e = etude();
        publisher.recorded(e, acteur);
        publisher.revised(e, acteur);
        publisher.actionPlanChanged(e, acteur);
        publisher.deleted(e, acteur);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> traces =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(4)).recordForTenant(eq(tenant), traces.capture());
        List<AuditEventDto.RecordEventRequest> t = traces.getAllValues();
        assertThat(t).extracting(AuditEventDto.RecordEventRequest::action).containsExactly(
                "spc.study.recorded", "spc.study.revised", "spc.study.action-plan", "spc.study.deleted");
        assertThat(t).allSatisfy(r -> {
            assertThat(r.resourceType()).isEqualTo("spc-study");
            assertThat(r.resourceId()).isEqualTo(e.getId());
            assertThat(r.actorType()).isEqualTo(ActorType.USER);
            assertThat(r.actorUserId()).isEqualTo(acteur);
            assertThat(r.payloadJson()).contains("\"cpk\":1.769").contains("\"status\":\"CAPABLE\"")
                    .doesNotContain("J. Martin").doesNotContain("504");
        });
    }

    @Test
    void lesSeuilsModifiesLaissentLeurTrace() {
        publisher.thresholdsChanged(tenant, new CapabilityThresholds(new BigDecimal("1.50"), new BigDecimal("1.00")),
                acteur);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> trace =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events).recordForTenant(eq(tenant), trace.capture());
        assertThat(trace.getValue().action()).isEqualTo("spc.settings.updated");
        assertThat(trace.getValue().resourceType()).isEqualTo("spc-settings");
        assertThat(trace.getValue().payloadJson()).isEqualTo("{\"capableThreshold\":1.50,\"watchThreshold\":1.00}");
    }

    @Test
    void uneGrilleQuiNeSeSerialisePasEstUneErreurDeStockage() {
        ObjectMapper casse = new ObjectMapper().registerModule(new SimpleModule().addSerializer(
                new StdSerializer<Double>(Double.class) {
                    @Override
                    public void serialize(Double v, JsonGenerator g, SerializerProvider p) throws IOException {
                        throw new com.fasterxml.jackson.core.JsonGenerationException("casse", g);
                    }
                }));
        SpcRepositoryAdapters.Studies studies = new SpcRepositoryAdapters.Studies(
                mock(SpcStudyJpaRepository.class), casse);
        CapabilityStudy neuve = CapabilityStudy.record(tenant, SpcFixtures.goldenDetails(),
                CapabilityThresholds.DEFAULT, acteur, Instant.EPOCH);

        assertThatThrownBy(() -> studies.save(neuve)).isInstanceOf(IllegalStateException.class);
    }
}
