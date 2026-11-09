package com.openlab.qualitos.quality.dmaic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Dispositif Poka-Yoke du **catalogue plateforme** (partage entre tenants,
 * meme philosophie que le Standards Hub).
 *
 * Un tenant peut "assigner" un device a un projet DMAIC via PokaYokeAssignment.
 */
@Entity
@Table(name = "pokayoke_devices")
@Getter
@Setter
@NoArgsConstructor
public class PokaYokeDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Le client a qui ce dispositif appartient, ou {@code null} pour le catalogue
     * de la plateforme, partage par tous (meme philosophie que le Standards Hub).
     *
     * <p>Un detrompeur ne d'une action DMAIC appartient au client qui l'a
     * concu : le verser au catalogue partage reviendrait a montrer a tous la
     * solution d'un seul. Il reste reutilisable sur ses autres lignes, et c'est
     * tout ce que la promesse demande.
     */
    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PokaYokeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PokaYokeMechanism mechanism;

    /** Secteurs ou la solution est typiquement applicable (CSV). */
    @Column(name = "applicable_industries", columnDefinition = "TEXT")
    private String applicableIndustries;

    /** Exemples concrets (texte libre). */
    @Column(columnDefinition = "TEXT")
    private String examples;

    /**
     * D'ou vient ce dispositif, cite en clair : « action du projet DMAIC X ».
     *
     * <p>Une citation plutot qu'une cle etrangere, comme au registre des
     * risques : la fiche de resolution evolue, ses lignes se renumerotent, et
     * un lien dur se briserait sur un dispositif qui, lui, reste valable.
     * {@code null} pour les dispositifs du catalogue de la plateforme.
     */
    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    /** Cout estimé d'implementation (faible/moyen/élevé) — pour aide a la priorisation. */
    @Column(name = "implementation_cost", length = 20)
    private String implementationCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
