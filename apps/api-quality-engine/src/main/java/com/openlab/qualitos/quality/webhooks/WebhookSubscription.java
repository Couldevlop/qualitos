package com.openlab.qualitos.quality.webhooks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "endpoint_url", nullable = false, length = 2048)
    private String endpointUrl;

    /** Liste CSV des EventType.wire() souscrits. Vide = aucun. */
    @Column(name = "event_types", nullable = false, columnDefinition = "TEXT")
    private String eventTypes;

    /**
     * Clé partagée HMAC utilisée pour signer les payloads.
     * En prod : chiffrer au repos via Vault Transit (cf. CLAUDE.md §11.1 A02).
     * Pour le MVP : plain text en DB avec accès role-restricted.
     */
    @Column(nullable = false, length = 128)
    private String secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SubscriptionStatus.ACTIVE;
        }
        if (this.maxRetries == 0) {
            this.maxRetries = 5;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Vrai si l'abonnement matche l'event type passé en argument. */
    public boolean matches(EventType type) {
        if (eventTypes == null || eventTypes.isBlank()) return false;
        String wire = type.wire();
        for (String t : eventTypes.split(",")) {
            if (t.trim().equals(wire)) return true;
        }
        return false;
    }
}
