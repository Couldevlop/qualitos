package com.openlab.qualitos.quality.erpconnector;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Connexion ERP (un tenant peut avoir 0..N connexions, une par provider/instance) —
 * CLAUDE.md §13.3. Calquée sur {@code itsm.ItsmConnection}.
 *
 * <p>Le secret (mot de passe Basic SAP, token Bearer Oracle/Dynamics) est chiffré au repos
 * via {@code itsm.SecretCipher} (AES-256-GCM) avant persistance. Le champ
 * {@link #credentialCipher} contient le ciphertext base64. On NE STOCKE JAMAIS le secret
 * en clair, et on ne le ré-expose JAMAIS dans une réponse API.
 *
 * <p>Note Vault : à terme on stockera un alias Vault Transit ; pour V1 on garde l'approche
 * autonome (chiffrement local, cf. §10.2/§11.2).
 */
@Entity
@Table(name = "erp_connections",
        uniqueConstraints = @UniqueConstraint(name = "uk_erp_tenant_name",
                columnNames = {"tenant_id", "name"}))
public class ErpConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ErpProvider provider;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    /** Identifiant principal côté ERP (compte technique SAP / utilisateur Oracle). */
    @Column(name = "username", length = 200)
    private String username;

    /** Ciphertext base64 du secret (password/token). */
    @Column(name = "credential_cipher", nullable = false, length = 2048)
    private String credentialCipher;

    /**
     * Optionnel : portée fonctionnelle côté ERP (société/Company Code SAP, Business Unit
     * Oracle, environnement Dynamics). Sert à filtrer les imports.
     */
    @Column(name = "external_scope", length = 200)
    private String externalScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ErpConnectionStatus status;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

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
        if (status == null) status = ErpConnectionStatus.ACTIVE;
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

    public ErpProvider getProvider() { return provider; }
    public void setProvider(ErpProvider provider) { this.provider = provider; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCredentialCipher() { return credentialCipher; }
    public void setCredentialCipher(String credentialCipher) { this.credentialCipher = credentialCipher; }

    public String getExternalScope() { return externalScope; }
    public void setExternalScope(String externalScope) { this.externalScope = externalScope; }

    public ErpConnectionStatus getStatus() { return status; }
    public void setStatus(ErpConnectionStatus status) { this.status = status; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
