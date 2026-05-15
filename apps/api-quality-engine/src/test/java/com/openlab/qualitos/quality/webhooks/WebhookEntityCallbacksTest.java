package com.openlab.qualitos.quality.webhooks;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookEntityCallbacksTest {

    @Test
    void subscriptionPrePersist_defaults() throws Exception {
        WebhookSubscription s = new WebhookSubscription();
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getMaxRetries()).isEqualTo(5);
        assertThat(s.getCreatedAt()).isNotNull();
    }

    @Test
    void subscriptionPrePersist_preserves() throws Exception {
        WebhookSubscription s = new WebhookSubscription();
        s.setStatus(SubscriptionStatus.PAUSED);
        s.setMaxRetries(2);
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(s.getMaxRetries()).isEqualTo(2);
    }

    @Test
    void subscriptionPreUpdate_refreshes() throws Exception {
        WebhookSubscription s = new WebhookSubscription();
        s.setCreatedAt(Instant.now().minusSeconds(60));
        s.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = s.getUpdatedAt();
        Thread.sleep(5);
        invoke(s, "preUpdate");
        assertThat(s.getUpdatedAt()).isAfter(before);
    }

    @Test
    void deliveryPrePersist_defaultsPending() throws Exception {
        WebhookDelivery d = new WebhookDelivery();
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(d.getCreatedAt()).isNotNull();
    }

    @Test
    void deliveryPrePersist_preservesStatus() throws Exception {
        WebhookDelivery d = new WebhookDelivery();
        d.setStatus(DeliveryStatus.SUCCESS);
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
    }

    @Test
    void deliveryPreUpdate_refreshes() throws Exception {
        WebhookDelivery d = new WebhookDelivery();
        d.setCreatedAt(Instant.now().minusSeconds(60));
        d.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = d.getUpdatedAt();
        Thread.sleep(5);
        invoke(d, "preUpdate");
        assertThat(d.getUpdatedAt()).isAfter(before);
    }

    @Test
    void eventType_fromWire_roundTrip() {
        for (EventType t : EventType.values()) {
            assertThat(EventType.fromWire(t.wire())).isEqualTo(t);
        }
    }

    @Test
    void eventType_fromWire_unknown_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EventType.fromWire("bogus.event"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
