package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une phase du cycle APQP, propre à un client.
 *
 * <p>Les cinq phases du manuel AIAG servent d'amorçage, pas de contrainte : un
 * équipementier automobile n'attend pas les mêmes livrables qu'un fabricant de
 * dispositifs médicaux, et le nombre de phases lui-même se discute.
 *
 * <p>Le {@code position} dessine le V : la phase du milieu en est le point bas.
 * Il est porté explicitement plutôt que déduit de l'ordre d'insertion, sans quoi
 * insérer une phase entre deux autres deviendrait impossible.
 */
@Entity
@Table(name = "apqp_phases")
@Getter
@Setter
@NoArgsConstructor
public class ApqpPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Rang dans le cycle, à partir de 1. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 255)
    private String title;

    /** Ce que la phase établit, en une phrase. */
    @Column(length = 500)
    private String purpose;

    /** La question à laquelle ses livrables répondent. */
    @Column(length = 500)
    private String question;

    /**
     * Les livrables attendus, dans leur ordre.
     *
     * <p>{@code orphanRemoval} : un livrable retiré de la liste n'a pas
     * d'existence propre — il n'est rattaché nulle part ailleurs — et doit
     * disparaître plutôt que de survivre sans phase.
     */
    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ApqpDeliverable> deliverables = new ArrayList<>();

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

    /** Rattache un livrable des DEUX côtés : une seule moitié laisse un orphelin. */
    public void addDeliverable(ApqpDeliverable deliverable) {
        deliverable.setPhase(this);
        deliverable.setTenantId(this.tenantId);
        this.deliverables.add(deliverable);
    }
}
