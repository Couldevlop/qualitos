package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Connexion de communication sortante (un tenant peut avoir 0..N connexions, une par
 * provider/canal). Calquée sur {@code quality/itsm/ItsmConnection} (CLAUDE.md §13.3).
 *
 * <p>L'URL d'incoming-webhook est un secret (elle porte un jeton non devinable qui
 * autorise à poster dans le canal). Elle est donc chiffrée au repos via
 * {@code quality/itsm/SecretCipher} et stockée dans {@link #webhookUrlCipher} (base64).
 * On NE STOCKE JAMAIS l'URL en clair et on ne la ré-expose jamais dans une réponse API.
 *
 * <p>On réutilise {@link ConnectionStatus} (ACTIVE / DISABLED / DISABLED_ON_ERRORS) du
 * module itsm pour la cohérence de la couche connecteurs.
 */
@Entity
@Table(name = "comm_connections",
        uniqueConstraints = @UniqueConstraint(name = "uk_comm_tenant_name",
                columnNames = {"tenant_id", "name"}))
public class CommConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommProvider provider;

    /** Ciphertext base64 de l'URL d'incoming-webhook (le secret). */
    @Column(name = "webhook_url_cipher", nullable = false, length = 2048)
    private String webhookUrlCipher;

    /** Optionnel : canal cible affiché (#qualite) ou override Mattermost. */
    @Column(name = "channel", length = 200)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConnectionStatus status;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

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
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ConnectionStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CommProvider getProvider() { return provider; }
    public void setProvider(CommProvider provider) { this.provider = provider; }

    public String getWebhookUrlCipher() { return webhookUrlCipher; }
    public void setWebhookUrlCipher(String webhookUrlCipher) { this.webhookUrlCipher = webhookUrlCipher; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public Instant getLastNotifiedAt() { return lastNotifiedAt; }
    public void setLastNotifiedAt(Instant lastNotifiedAt) { this.lastNotifiedAt = lastNotifiedAt; }

    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
