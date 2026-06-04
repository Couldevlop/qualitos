package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests unitaires (sans contexte Spring) du 2e consommateur Kafka qui déclenche les
 * webhooks. Mocke {@link WebhookService} ; l'{@link ObjectMapper} est réel.
 */
class WebhookKafkaConsumerTest {

    private static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";

    private WebhookService webhookService;
    private WebhookKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        webhookService = Mockito.mock(WebhookService.class);
        consumer = new WebhookKafkaConsumer(webhookService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        // Garantit qu'aucun test ne laisse le ThreadLocal sale pour les suivants.
        TenantContext.clear();
    }

    @Test
    void publishes_event_and_clears_tenant_context_when_action_maps_to_event_type() {
        String envelope = """
                {
                  "tenantId": "%s",
                  "sequenceNo": 42,
                  "action": "webhook.test.ping",
                  "resourceType": "Audit",
                  "resourceId": "22222222-2222-2222-2222-222222222222",
                  "summary": "ping de test",
                  "occurredAt": "2026-06-03T10:15:30Z"
                }
                """.formatted(TENANT_ID);

        consumer.consume(envelope);

        verify(webhookService, times(1)).publish(eq(EventType.TEST_PING), anyMap());
        // Le ThreadLocal doit être nettoyé après l'appel (finally).
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void ignores_action_that_is_not_a_subscribable_event_type() {
        String envelope = """
                {
                  "tenantId": "%s",
                  "sequenceNo": 7,
                  "action": "capa.created"
                }
                """.formatted(TENANT_ID);

        consumer.consume(envelope);

        verifyNoInteractions(webhookService);
    }

    @Test
    void ignores_malformed_json_without_throwing() {
        consumer.consume("{ this is not valid json ");

        verifyNoInteractions(webhookService);
    }

    @Test
    void ignores_envelope_without_tenant_id() {
        String envelope = """
                {
                  "sequenceNo": 3,
                  "action": "webhook.test.ping"
                }
                """;

        consumer.consume(envelope);

        verifyNoInteractions(webhookService);
    }
}
