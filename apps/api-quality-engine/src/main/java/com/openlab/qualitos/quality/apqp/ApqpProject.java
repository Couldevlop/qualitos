package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un projet APQP : un cycle, et le dossier PPAP qu'il produit.
 *
 * <p>Le cycle appartenait au client, un seul pour tout le tenant (ADR 0066). Or
 * une organisation en mène plusieurs de front — une introduction de produit pour
 * un client, un transfert d'outillage pour un autre — et chacun a son propre
 * dossier PPAP à remettre. Un cycle unique les mélangeait : on ne pouvait ni
 * dire de quel programme venait un livrable coché, ni remettre le dossier de
 * l'un sans y trouver les pièces de l'autre.
 *
 * <p>Le projet est donc l'unité de travail, comme l'est un projet PFMEA. Les
 * phases lui appartiennent, et le tenant reste le propriétaire de tout.
 */
@Entity
@Table(name = "apqp_projects")
@Getter
@Setter
@NoArgsConstructor
public class ApqpProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    /** Ce que le projet ouvre. Voir {@link ApqpProjectType}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApqpProjectType type = ApqpProjectType.OTHER;

    /**
     * Le client destinataire du dossier PPAP.
     *
     * <p>Facultatif : un projet interne n'en a pas. Mais c'est la première chose
     * qu'on cherche dans une liste de programmes, avant même leur intitulé.
     */
    @Column(length = 255)
    private String customer;

    /** Référence du programme chez le client, ou repère interne. */
    @Column(length = 120)
    private String reference;

    @Column(length = 2000)
    private String description;

    /**
     * Qui a ouvert le projet, pris du jeton — jamais du corps de la requête.
     *
     * <p>Même règle que partout : l'accepter de l'appelant laisserait attribuer
     * un projet à quelqu'un d'autre.
     */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant maintenant = Instant.now();
        this.createdAt = maintenant;
        this.updatedAt = maintenant;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
