package com.openlab.qualitos.core.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * La messagerie d'un client : une organisation, une boîte d'envoi.
 *
 * <p>L'identifiant du client <b>est</b> la clé primaire. Un identifiant
 * technique de plus aurait laissé exister deux messageries pour un même client,
 * dont une que personne ne regarde — et le courrier serait parti de l'autre.
 */
@Entity
@Table(name = "tenant_mail_settings")
public class TenantMailSettings {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Embedded
    private MailAccount account;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TenantMailSettings() {
        // JPA
    }

    public TenantMailSettings(UUID tenantId, MailAccount account) {
        this.tenantId = tenantId;
        this.account = account;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public MailAccount getAccount() {
        return account;
    }

    public void setAccount(MailAccount account) {
        this.account = account;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
