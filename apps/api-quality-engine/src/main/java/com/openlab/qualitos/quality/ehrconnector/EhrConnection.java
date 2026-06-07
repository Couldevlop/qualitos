package com.openlab.qualitos.quality.ehrconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Connexion EHR / HL7 FHIR (§13.3) : un tenant peut avoir 0..N connexions, une par
 * serveur FHIR (EHR/HIS) à interroger.
 *
 * <p>Le secret (mot de passe Basic ou jeton Bearer) est chiffré au repos via
 * {@link com.openlab.qualitos.quality.itsm.SecretCipher} (AES-256-GCM) avant
 * persistance. Le champ {@link #credentialCipher} contient le ciphertext base64.
 * On NE STOCKE JAMAIS le secret en clair (CLAUDE.md §11.1 / §18.2-3).</p>
 *
 * <p>On réutilise {@link ConnectionStatus} du module ITSM (même cycle de vie de
 * connexion : ACTIVE / DISABLED / DISABLED_ON_ERRORS), sans dupliquer l'enum.</p>
 */
@Entity
@Table(name = "ehr_connections",
        uniqueConstraints = @UniqueConstraint(name = "uk_ehr_tenant_name",
                columnNames = {"tenant_id", "name"}))
public class EhrConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EhrProvider provider;

    /** URL de base du serveur FHIR (ex. https://fhir.exemple.org/r5). */
    @Column(name = "fhir_base_url", nullable = false, length = 512)
    private String fhirBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_mode", nullable = false, length = 16)
    private EhrAuthMode authMode;

    /** Identifiant Basic (login). Null en mode Bearer. */
    @Column(name = "username", length = 200)
    private String username;

    /** Ciphertext base64 du secret (password Basic ou jeton Bearer). */
    @Column(name = "credential_cipher", nullable = false, length = 2048)
    private String credentialCipher;

    /**
     * Catégorie/flag FHIR utilisé pour filtrer les ressources « patient-safety /
     * adverse event / abnormal ». Optionnel : à défaut, on filtre sur les ressources
     * dont l'interprétation est anormale.
     */
    @Column(name = "resource_category", length = 120)
    private String resourceCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConnectionStatus status;

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

    public EhrProvider getProvider() { return provider; }
    public void setProvider(EhrProvider provider) { this.provider = provider; }

    public String getFhirBaseUrl() { return fhirBaseUrl; }
    public void setFhirBaseUrl(String fhirBaseUrl) { this.fhirBaseUrl = fhirBaseUrl; }

    public EhrAuthMode getAuthMode() { return authMode; }
    public void setAuthMode(EhrAuthMode authMode) { this.authMode = authMode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCredentialCipher() { return credentialCipher; }
    public void setCredentialCipher(String credentialCipher) { this.credentialCipher = credentialCipher; }

    public String getResourceCategory() { return resourceCategory; }
    public void setResourceCategory(String resourceCategory) { this.resourceCategory = resourceCategory; }

    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }

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
