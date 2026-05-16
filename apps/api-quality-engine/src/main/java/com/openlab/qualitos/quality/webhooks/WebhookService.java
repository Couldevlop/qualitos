package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    /** Au-delà de ce nombre d'échecs consécutifs, on désactive la souscription. */
    static final int MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE = 10;

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher dispatcher;
    private final RetryBackoff backoff;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookSubscriptionRepository subscriptionRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          WebhookDispatcher dispatcher,
                          RetryBackoff backoff,
                          ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.dispatcher = dispatcher;
        this.backoff = backoff;
        this.objectMapper = objectMapper;
    }

    // ===== Subscriptions =====

    @Transactional(readOnly = true)
    public Page<WebhookDto.SubscriptionResponse> listSubscriptions(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return subscriptionRepository.findByTenantId(tenantId, pageable).map(this::toSubscriptionResponse);
    }

    @Transactional(readOnly = true)
    public WebhookDto.SubscriptionResponse getSubscription(UUID id) {
        return toSubscriptionResponse(loadSubscription(id));
    }

    public WebhookDto.CreatedSubscriptionResponse createSubscription(WebhookDto.CreateSubscriptionRequest req) {
        UUID tenantId = requireTenantId();
        WebhookSubscription s = new WebhookSubscription();
        s.setTenantId(tenantId);
        s.setName(req.name());
        s.setEndpointUrl(req.endpointUrl());
        s.setEventTypes(req.eventTypes().stream().map(EventType::wire).collect(Collectors.joining(",")));
        s.setSecret(req.secret());
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setMaxRetries(req.maxRetries() != null ? req.maxRetries() : 5);
        s.setCreatedBy(req.createdBy());
        s = subscriptionRepository.save(s);
        return new WebhookDto.CreatedSubscriptionResponse(toSubscriptionResponse(s), req.secret());
    }

    public WebhookDto.SubscriptionResponse updateSubscription(UUID id, WebhookDto.UpdateSubscriptionRequest req) {
        WebhookSubscription s = loadSubscription(id);
        if (req.name() != null) s.setName(req.name());
        if (req.endpointUrl() != null) s.setEndpointUrl(req.endpointUrl());
        if (req.eventTypes() != null && !req.eventTypes().isEmpty()) {
            s.setEventTypes(req.eventTypes().stream().map(EventType::wire).collect(Collectors.joining(",")));
        }
        if (req.secret() != null) s.setSecret(req.secret());
        if (req.maxRetries() != null) s.setMaxRetries(req.maxRetries());
        if (req.status() != null) {
            // Si on réactive, on remet le compteur a zero pour donner sa chance.
            if (req.status() == SubscriptionStatus.ACTIVE
                    && s.getStatus() == SubscriptionStatus.DISABLED_ON_ERRORS) {
                s.setConsecutiveFailures(0);
            }
            s.setStatus(req.status());
        }
        return toSubscriptionResponse(subscriptionRepository.save(s));
    }

    public void deleteSubscription(UUID id) {
        WebhookSubscription s = loadSubscription(id);
        subscriptionRepository.delete(s);
    }

    // ===== Deliveries =====

    @Transactional(readOnly = true)
    public Page<WebhookDto.DeliveryResponse> listDeliveries(UUID subscriptionId,
                                                            DeliveryStatus status,
                                                            Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<WebhookDelivery> page;
        if (subscriptionId != null) {
            page = deliveryRepository.findBySubscriptionIdAndTenantId(subscriptionId, tenantId, pageable);
        } else if (status != null) {
            page = deliveryRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = deliveryRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toDeliveryResponse);
    }

    @Transactional(readOnly = true)
    public WebhookDto.DeliveryResponse getDelivery(UUID id) {
        return toDeliveryResponse(loadDelivery(id));
    }

    /**
     * Re-essai manuel : remet une livraison en pending et la dispatch.
     * Utilisable même après DEAD_LETTER pour forcer un nouveau test.
     */
    public WebhookDto.DeliveryResponse retryDelivery(UUID id) {
        WebhookDelivery d = loadDelivery(id);
        if (d.getStatus() == DeliveryStatus.SUCCESS) {
            throw new WebhookStateException("Delivery already succeeded");
        }
        return toDeliveryResponse(attemptDispatch(d));
    }

    // ===== Publish (called by domain services or test endpoint) =====

    /**
     * Publie un évènement pour le tenant courant (TenantContext requis).
     * Crée une WebhookDelivery par souscription ACTIVE matchant l'event type,
     * tente la livraison immédiate (sync). Les échecs sont marqués RETRYING
     * avec next_retry_at planifié.
     *
     * @return liste des deliveries créées (peut être vide si aucun abonnement)
     */
    public List<UUID> publish(EventType type, Map<String, Object> payload) {
        UUID tenantId = requireTenantId();
        List<WebhookSubscription> subs = subscriptionRepository
                .findByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE);
        if (subs.isEmpty()) {
            return List.of();
        }

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("id", eventId);
        envelope.put("type", type.wire());
        envelope.put("tenant_id", tenantId.toString());
        envelope.put("occurred_at", Instant.now().toString());
        envelope.put("data", payload != null ? payload : Map.of());

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook event payload", e);
            return List.of();
        }

        List<UUID> deliveryIds = new ArrayList<>();
        for (WebhookSubscription s : subs) {
            if (!s.matches(type)) continue;
            WebhookDelivery d = new WebhookDelivery();
            d.setTenantId(tenantId);
            d.setSubscription(s);
            d.setEventId(eventId);
            d.setEventType(type);
            d.setPayload(json);
            d.setStatus(DeliveryStatus.PENDING);
            d = deliveryRepository.save(d);
            attemptDispatch(d);
            deliveryIds.add(d.getId());
        }
        return deliveryIds;
    }

    /**
     * Tente la dispatch immédiate. Met à jour status, response, next_retry_at.
     * Met également à jour les compteurs sur la souscription.
     */
    WebhookDelivery attemptDispatch(WebhookDelivery d) {
        WebhookSubscription s = d.getSubscription();
        WebhookDispatcher.DispatchResult result = dispatcher.dispatch(
                s, d.getEventType(), d.getEventId(), d.getPayload());

        d.setAttemptCount(d.getAttemptCount() + 1);
        d.setLastAttemptAt(Instant.now());
        d.setResponseStatusCode(result.statusCode());
        d.setResponseBody(result.responseBody());
        d.setErrorMessage(result.errorMessage());

        s.setLastTriggeredAt(Instant.now());

        if (result.success()) {
            d.setStatus(DeliveryStatus.SUCCESS);
            d.setNextRetryAt(null);
            s.setLastSuccessAt(Instant.now());
            s.setConsecutiveFailures(0);
        } else {
            s.setConsecutiveFailures(s.getConsecutiveFailures() + 1);
            if (d.getAttemptCount() >= s.getMaxRetries() + 1) {
                d.setStatus(DeliveryStatus.DEAD_LETTER);
                d.setNextRetryAt(null);
            } else {
                d.setStatus(DeliveryStatus.RETRYING);
                d.setNextRetryAt(backoff.nextRetryAt(d.getAttemptCount()));
            }
            if (s.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE) {
                s.setStatus(SubscriptionStatus.DISABLED_ON_ERRORS);
            }
        }
        subscriptionRepository.save(s);
        return deliveryRepository.save(d);
    }

    /**
     * Envoie un événement de test (TEST_PING) sur la souscription.
     * Retourne le résultat synchrone — utile pour tester la config endpoint
     * + secret depuis l'UI.
     */
    public WebhookDto.TestPingResponse testPing(UUID subscriptionId) {
        UUID tenantId = requireTenantId();
        WebhookSubscription s = subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new WebhookSubscriptionNotFoundException(subscriptionId));

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = Map.of(
                "id", eventId,
                "type", EventType.TEST_PING.wire(),
                "tenant_id", tenantId.toString(),
                "occurred_at", Instant.now().toString(),
                "data", Map.of("message", "Hello from QualitOS"));
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            return new WebhookDto.TestPingResponse(null, DeliveryStatus.FAILED, null, e.getMessage());
        }

        WebhookDelivery d = new WebhookDelivery();
        d.setTenantId(tenantId);
        d.setSubscription(s);
        d.setEventId(eventId);
        d.setEventType(EventType.TEST_PING);
        d.setPayload(json);
        d.setStatus(DeliveryStatus.PENDING);
        d = deliveryRepository.save(d);
        d = attemptDispatch(d);
        return new WebhookDto.TestPingResponse(
                d.getId(), d.getStatus(), d.getResponseStatusCode(), d.getErrorMessage());
    }

    // ===== helpers =====

    private WebhookSubscription loadSubscription(UUID id) {
        UUID tenantId = requireTenantId();
        return subscriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new WebhookSubscriptionNotFoundException(id));
    }

    private WebhookDelivery loadDelivery(UUID id) {
        UUID tenantId = requireTenantId();
        return deliveryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new WebhookDeliveryNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    WebhookDto.SubscriptionResponse toSubscriptionResponse(WebhookSubscription s) {
        List<String> types = s.getEventTypes() == null || s.getEventTypes().isBlank()
                ? List.of()
                : List.of(s.getEventTypes().split(","));
        return new WebhookDto.SubscriptionResponse(
                s.getId(), s.getTenantId(), s.getName(), s.getEndpointUrl(),
                types, s.getStatus(), s.getMaxRetries(), s.getConsecutiveFailures(),
                s.getLastTriggeredAt(), s.getLastSuccessAt(),
                s.getCreatedBy(), s.getCreatedAt(), s.getUpdatedAt());
    }

    WebhookDto.DeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return new WebhookDto.DeliveryResponse(
                d.getId(), d.getTenantId(), d.getSubscription().getId(),
                d.getEventId(), d.getEventType(), d.getPayload(),
                d.getStatus(), d.getAttemptCount(), d.getLastAttemptAt(), d.getNextRetryAt(),
                d.getResponseStatusCode(), d.getResponseBody(), d.getErrorMessage(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
