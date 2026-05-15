package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock WebhookSubscriptionRepository subRepo;
    @Mock WebhookDeliveryRepository deliveryRepo;
    @Mock WebhookDispatcher dispatcher;
    @Mock RetryBackoff backoff;
    @InjectMocks WebhookService service;

    final ObjectMapper objectMapper = new ObjectMapper();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        TenantContext.setTenantId(TENANT.toString());
        // inject ObjectMapper since @InjectMocks ne le voit pas (pas mocke)
        var f = WebhookService.class.getDeclaredField("objectMapper");
        f.setAccessible(true);
        f.set(service, objectMapper);
    }

    @AfterEach
    void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void create_success() {
        WebhookDto.CreateSubscriptionRequest req = new WebhookDto.CreateSubscriptionRequest(
                "Slack channel #qualite", "https://hooks.example.com/abc",
                List.of(EventType.PDCA_CYCLE_ADVANCED, EventType.CAPA_CASE_RESOLVED),
                "supersecret-1234", 5, USER);
        when(subRepo.save(any())).thenAnswer(inv -> {
            WebhookSubscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now()); s.setUpdatedAt(Instant.now());
            return s;
        });
        WebhookDto.CreatedSubscriptionResponse r = service.createSubscription(req);
        assertThat(r.subscription().status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(r.subscription().eventTypes())
                .contains(EventType.PDCA_CYCLE_ADVANCED.wire(), EventType.CAPA_CASE_RESOLVED.wire());
        assertThat(r.secret()).isEqualTo("supersecret-1234");
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        WebhookDto.CreateSubscriptionRequest req = new WebhookDto.CreateSubscriptionRequest(
                "n", "https://x", List.of(EventType.TEST_PING), "k1234567890123456", 5, USER);
        assertThatThrownBy(() -> service.createSubscription(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- list/get ---
    @Test
    void list_paginated() {
        Pageable p = PageRequest.of(0, 10);
        WebhookSubscription s = subscription();
        when(subRepo.findByTenantId(TENANT, p)).thenReturn(new PageImpl<>(List.of(s)));
        Page<WebhookDto.SubscriptionResponse> r = service.listSubscriptions(p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void get_found() {
        WebhookSubscription s = subscription();
        when(subRepo.findByIdAndTenantId(s.getId(), TENANT)).thenReturn(Optional.of(s));
        assertThat(service.getSubscription(s.getId()).id()).isEqualTo(s.getId());
    }

    @Test
    void get_notFound() {
        UUID id = UUID.randomUUID();
        when(subRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSubscription(id))
                .isInstanceOf(WebhookSubscriptionNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_changesFields() {
        WebhookSubscription s = subscription();
        when(subRepo.findByIdAndTenantId(s.getId(), TENANT)).thenReturn(Optional.of(s));
        when(subRepo.save(s)).thenReturn(s);
        service.updateSubscription(s.getId(), new WebhookDto.UpdateSubscriptionRequest(
                "new-name", "https://new.example.com/h",
                List.of(EventType.AUDIT_PLAN_COMPLETED),
                "newsecret-12345", 3, SubscriptionStatus.PAUSED));
        assertThat(s.getName()).isEqualTo("new-name");
        assertThat(s.getEndpointUrl()).isEqualTo("https://new.example.com/h");
        assertThat(s.getEventTypes()).contains(EventType.AUDIT_PLAN_COMPLETED.wire());
        assertThat(s.getMaxRetries()).isEqualTo(3);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }

    @Test
    void update_reactivatingDisabled_resetsCounter() {
        WebhookSubscription s = subscription();
        s.setStatus(SubscriptionStatus.DISABLED_ON_ERRORS);
        s.setConsecutiveFailures(10);
        when(subRepo.findByIdAndTenantId(s.getId(), TENANT)).thenReturn(Optional.of(s));
        when(subRepo.save(s)).thenReturn(s);
        service.updateSubscription(s.getId(), new WebhookDto.UpdateSubscriptionRequest(
                null, null, null, null, null, SubscriptionStatus.ACTIVE));
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getConsecutiveFailures()).isZero();
    }

    @Test
    void delete_success() {
        WebhookSubscription s = subscription();
        when(subRepo.findByIdAndTenantId(s.getId(), TENANT)).thenReturn(Optional.of(s));
        service.deleteSubscription(s.getId());
        verify(subRepo).delete(s);
    }

    // --- publish ---
    @Test
    void publish_noSubscriptions_returnsEmpty() {
        when(subRepo.findByTenantIdAndStatus(TENANT, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        List<UUID> ids = service.publish(EventType.PDCA_CYCLE_CREATED, Map.of("cycleId", "c1"));
        assertThat(ids).isEmpty();
        verify(dispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void publish_matchingSubscription_creates_dispatches_success() {
        WebhookSubscription s = subscription();
        s.setEventTypes(EventType.PDCA_CYCLE_CREATED.wire());
        when(subRepo.findByTenantIdAndStatus(TENANT, SubscriptionStatus.ACTIVE)).thenReturn(List.of(s));
        when(deliveryRepo.save(any())).thenAnswer(inv -> {
            WebhookDelivery d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            if (d.getCreatedAt() == null) d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });
        when(dispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(true, 200, "ok", null));

        List<UUID> ids = service.publish(EventType.PDCA_CYCLE_CREATED, Map.of("cycleId", "c1"));

        assertThat(ids).hasSize(1);
        verify(dispatcher).dispatch(eq(s), eq(EventType.PDCA_CYCLE_CREATED), any(), any());
        assertThat(s.getConsecutiveFailures()).isZero();
        assertThat(s.getLastSuccessAt()).isNotNull();
    }

    @Test
    void publish_subscriptionDoesNotMatchEvent_skipped() {
        WebhookSubscription s = subscription();
        s.setEventTypes(EventType.AUDIT_PLAN_COMPLETED.wire()); // ne matche pas l'event publié
        when(subRepo.findByTenantIdAndStatus(TENANT, SubscriptionStatus.ACTIVE)).thenReturn(List.of(s));

        List<UUID> ids = service.publish(EventType.PDCA_CYCLE_CREATED, Map.of("cycleId", "c1"));

        assertThat(ids).isEmpty();
        verify(dispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void publish_dispatchFails_marksRetrying() {
        WebhookSubscription s = subscription();
        s.setEventTypes(EventType.PDCA_CYCLE_CREATED.wire());
        s.setMaxRetries(3);
        when(subRepo.findByTenantIdAndStatus(TENANT, SubscriptionStatus.ACTIVE)).thenReturn(List.of(s));
        when(deliveryRepo.save(any())).thenAnswer(inv -> {
            WebhookDelivery d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            if (d.getCreatedAt() == null) d.setCreatedAt(Instant.now());
            return d;
        });
        when(dispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(false, 503, "boom", null));
        Instant next = Instant.now().plusSeconds(30);
        when(backoff.nextRetryAt(anyInt())).thenReturn(next);

        service.publish(EventType.PDCA_CYCLE_CREATED, Map.of());

        // Le delivery save final doit avoir status=RETRYING + nextRetryAt
        ArgumentCaptor<WebhookDelivery> cap = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepo, atLeastOnce()).save(cap.capture());
        assertThat(cap.getAllValues()).anyMatch(d ->
                d.getStatus() == DeliveryStatus.RETRYING && d.getNextRetryAt() != null);
        assertThat(s.getConsecutiveFailures()).isOne();
    }

    @Test
    void attemptDispatch_maxedOutRetries_marksDeadLetter() {
        WebhookSubscription s = subscription();
        s.setMaxRetries(2);
        WebhookDelivery d = delivery(s);
        d.setAttemptCount(2); // déjà 2 essais → la prochaine sera la 3e (max+1)
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(false, null, null, "timeout"));

        WebhookDelivery out = service.attemptDispatch(d);

        assertThat(out.getStatus()).isEqualTo(DeliveryStatus.DEAD_LETTER);
        assertThat(out.getNextRetryAt()).isNull();
    }

    @Test
    void attemptDispatch_consecutiveFailures_disableSubscription() {
        WebhookSubscription s = subscription();
        s.setConsecutiveFailures(WebhookService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE - 1);
        s.setMaxRetries(10);
        WebhookDelivery d = delivery(s);
        d.setAttemptCount(0);
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(false, 500, null, null));
        when(backoff.nextRetryAt(anyInt())).thenReturn(Instant.now().plusSeconds(30));

        service.attemptDispatch(d);

        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.DISABLED_ON_ERRORS);
    }

    // --- testPing ---
    @Test
    void testPing_dispatchesTestEvent() {
        WebhookSubscription s = subscription();
        when(subRepo.findByIdAndTenantId(s.getId(), TENANT)).thenReturn(Optional.of(s));
        when(deliveryRepo.save(any())).thenAnswer(inv -> {
            WebhookDelivery d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            if (d.getCreatedAt() == null) d.setCreatedAt(Instant.now());
            return d;
        });
        when(dispatcher.dispatch(any(), eq(EventType.TEST_PING), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(true, 200, "pong", null));

        WebhookDto.TestPingResponse r = service.testPing(s.getId());

        assertThat(r.status()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(r.responseStatusCode()).isEqualTo(200);
    }

    // --- retry ---
    @Test
    void retry_resendsFailedDelivery() {
        WebhookSubscription s = subscription();
        WebhookDelivery d = delivery(s);
        d.setStatus(DeliveryStatus.DEAD_LETTER);
        d.setAttemptCount(5);
        when(deliveryRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(new WebhookDispatcher.DispatchResult(true, 200, "ok", null));

        WebhookDto.DeliveryResponse r = service.retryDelivery(d.getId());

        assertThat(r.status()).isEqualTo(DeliveryStatus.SUCCESS);
    }

    @Test
    void retry_alreadySuccess_throws() {
        WebhookSubscription s = subscription();
        WebhookDelivery d = delivery(s);
        d.setStatus(DeliveryStatus.SUCCESS);
        when(deliveryRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.retryDelivery(d.getId()))
                .isInstanceOf(WebhookStateException.class);
    }

    @Test
    void retry_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(deliveryRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.retryDelivery(id))
                .isInstanceOf(WebhookDeliveryNotFoundException.class);
    }

    // --- list deliveries ---
    @Test
    void listDeliveries_byStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(deliveryRepo.findByTenantIdAndStatus(TENANT, DeliveryStatus.DEAD_LETTER, p))
                .thenReturn(new PageImpl<>(List.of(delivery(subscription()))));
        service.listDeliveries(null, DeliveryStatus.DEAD_LETTER, p);
        verify(deliveryRepo).findByTenantIdAndStatus(TENANT, DeliveryStatus.DEAD_LETTER, p);
    }

    @Test
    void listDeliveries_bySubscription() {
        Pageable p = PageRequest.of(0, 10);
        UUID subId = UUID.randomUUID();
        when(deliveryRepo.findBySubscriptionIdAndTenantId(subId, TENANT, p))
                .thenReturn(new PageImpl<>(List.of()));
        service.listDeliveries(subId, null, p);
        verify(deliveryRepo).findBySubscriptionIdAndTenantId(subId, TENANT, p);
    }

    @Test
    void listDeliveries_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(deliveryRepo.findByTenantId(TENANT, p)).thenReturn(new PageImpl<>(List.of()));
        service.listDeliveries(null, null, p);
        verify(deliveryRepo).findByTenantId(TENANT, p);
    }

    // --- subscription.matches() ---
    @Test
    void subscription_matches_eventInList() {
        WebhookSubscription s = subscription();
        s.setEventTypes(EventType.PDCA_CYCLE_CREATED.wire() + "," + EventType.AUDIT_PLAN_COMPLETED.wire());
        assertThat(s.matches(EventType.PDCA_CYCLE_CREATED)).isTrue();
        assertThat(s.matches(EventType.AUDIT_PLAN_COMPLETED)).isTrue();
        assertThat(s.matches(EventType.CAPA_CASE_RESOLVED)).isFalse();
    }

    @Test
    void subscription_matches_blankList_returnsFalse() {
        WebhookSubscription s = subscription();
        s.setEventTypes("");
        assertThat(s.matches(EventType.PDCA_CYCLE_CREATED)).isFalse();
    }

    // --- helpers ---
    private WebhookSubscription subscription() {
        WebhookSubscription s = new WebhookSubscription();
        s.setId(UUID.randomUUID());
        s.setTenantId(TENANT);
        s.setName("test-sub");
        s.setEndpointUrl("https://x.example.com/h");
        s.setEventTypes(EventType.PDCA_CYCLE_CREATED.wire());
        s.setSecret("supersecret-12345");
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setMaxRetries(5);
        s.setCreatedBy(USER);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private WebhookDelivery delivery(WebhookSubscription s) {
        WebhookDelivery d = new WebhookDelivery();
        d.setId(UUID.randomUUID());
        d.setTenantId(TENANT);
        d.setSubscription(s);
        d.setEventId(UUID.randomUUID().toString());
        d.setEventType(EventType.PDCA_CYCLE_CREATED);
        d.setPayload("{}");
        d.setStatus(DeliveryStatus.PENDING);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }

    static org.mockito.ArgumentMatcher<WebhookDelivery> argThat(java.util.function.Predicate<WebhookDelivery> p) {
        return p::test;
    }

    static int anyInt() { return org.mockito.ArgumentMatchers.anyInt(); }
    static <T> T eq(T t) { return org.mockito.ArgumentMatchers.eq(t); }
}
