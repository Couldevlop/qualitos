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
