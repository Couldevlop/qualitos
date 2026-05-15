package com.openlab.qualitos.quality.webhooks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByTenantId(UUID tenantId, Pageable pageable);

    Page<WebhookDelivery> findByTenantIdAndStatus(UUID tenantId, DeliveryStatus status, Pageable pageable);

    Page<WebhookDelivery> findBySubscriptionIdAndTenantId(UUID subscriptionId, UUID tenantId, Pageable pageable);

    Optional<WebhookDelivery> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Pour le scheduler retry : livraisons en attente de re-essai avec next_retry_at passé. */
    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant cutoff);
}
