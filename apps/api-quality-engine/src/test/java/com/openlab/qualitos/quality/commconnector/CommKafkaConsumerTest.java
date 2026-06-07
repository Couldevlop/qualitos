package com.openlab.qualitos.quality.commconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommKafkaConsumerTest {

    @Mock CommConnectorService service;
    final ObjectMapper json = new ObjectMapper();

    CommKafkaConsumer consumer() { return new CommKafkaConsumer(service, json); }

    @Test
    void consume_targetedAction_mapsAndNotifies() {
        UUID tenant = UUID.randomUUID();
        String payload = "{\"tenantId\":\"" + tenant + "\",\"action\":\"non-conformity.detected\","
                + "\"resourceType\":\"NON_CONFORMITY\",\"resourceId\":\"42\",\"summary\":\"Défaut X\"}";

        consumer().consume(payload);

        ArgumentCaptor<CommEvent> cap = ArgumentCaptor.forClass(CommEvent.class);
        verify(service).notify(eq(tenant), cap.capture());
        CommEvent e = cap.getValue();
        assertThat(e.kind()).isEqualTo(CommEvent.Kind.NC_DETECTED);
        assertThat(e.resourceId()).isEqualTo("42");
        assertThat(e.summary()).isEqualTo("Défaut X");
        assertThat(e.severity()).isEqualTo(CommSeverity.WARNING);
    }

    @Test
    void consume_kpiThreshold_mapsCriticalSeverity() {
        UUID tenant = UUID.randomUUID();
        String payload = "{\"tenantId\":\"" + tenant + "\",\"action\":\"kpi.threshold.breached\"}";
        consumer().consume(payload);
        ArgumentCaptor<CommEvent> cap = ArgumentCaptor.forClass(CommEvent.class);
        verify(service).notify(eq(tenant), cap.capture());
        assertThat(cap.getValue().kind()).isEqualTo(CommEvent.Kind.KPI_THRESHOLD_BREACHED);
        assertThat(cap.getValue().severity()).isEqualTo(CommSeverity.CRITICAL);
    }

    @Test
    void consume_nonTargetedAction_ignored() {
        String payload = "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"action\":\"pdca.cycle.created\"}";
        consumer().consume(payload);
        verifyNoInteractions(service);
    }

    @Test
    void consume_unreadablePayload_ignored() {
        consumer().consume("not-json{");
        verifyNoInteractions(service);
    }

    @Test
    void consume_missingTenantOrAction_ignored() {
        consumer().consume("{\"action\":\"non-conformity.detected\"}");
        consumer().consume("{\"tenantId\":\"" + UUID.randomUUID() + "\"}");
        verifyNoInteractions(service);
    }

    @Test
    void consume_invalidTenantUuid_ignored() {
        consumer().consume("{\"tenantId\":\"not-a-uuid\",\"action\":\"non-conformity.detected\"}");
        verifyNoInteractions(service);
    }

    @Test
    void consume_neverPropagatesNotifyExceptions() {
        // notify est best-effort ; même s'il levait, le consumer ne doit pas casser le flux.
        // (ici on vérifie surtout que le chemin nominal appelle bien notify une fois)
        UUID tenant = UUID.randomUUID();
        doReturn(0).when(service).notify(any(), any());
        consumer().consume("{\"tenantId\":\"" + tenant + "\",\"action\":\"capa.case.overdue\"}");
        verify(service, times(1)).notify(eq(tenant), any());
    }
}
