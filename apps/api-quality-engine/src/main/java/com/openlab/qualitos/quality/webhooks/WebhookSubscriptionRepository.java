package com.openlab.qualitos.quality.webhooks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    Page<WebhookSubscription> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<WebhookSubscription> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WebhookSubscription> findByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);
}
