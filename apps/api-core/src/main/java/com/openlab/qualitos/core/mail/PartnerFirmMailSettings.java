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
 * La messagerie d'un cabinet partenaire.
 *
 * <p>Mêmes champs que celle d'un client, autre règle d'accès : c'est une donnée
 * de <b>plateforme</b>, gardée par le rôle, comme le cabinet lui-même. Un
 * cabinet sert plusieurs clients ; il n'a pas de client d'appartenance, et sa
 * messagerie non plus.
 *
 * <p>Ce qu'un cabinet ne peut <b>pas</b> faire : régler celle de ses clients. Il
 * peut la demander, pas l'écrire — chaque organisation reste maîtresse de
 * l'adresse d'où partent les courriels qui l'engagent.
 */
@Entity
@Table(name = "partner_firm_mail_settings")
public class PartnerFirmMailSettings {

    @Id
    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Embedded
    private MailAccount account;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PartnerFirmMailSettings() {
        // JPA
    }

    public PartnerFirmMailSettings(UUID firmId, MailAccount account) {
        this.firmId = firmId;
        this.account = account;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getFirmId() {
        return firmId;
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
