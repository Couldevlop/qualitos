package com.openlab.qualitos.quality.capa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "capa_actions")
@Getter
@Setter
@NoArgsConstructor
public class CapaAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capa_id", nullable = false, updatable = false)
    private CapaCase capa;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CapaActionStatus status;

    /**
     * Nature de l'action : endiguement, correction ou prévention.
     *
     * <p>Par défaut {@link CapaActionType#CORRECTIVE} — c'est ce que les lignes
     * antérieures à la colonne étaient implicitement, et le supposer ne prête
     * pas à conséquence : le seul risque serait de prendre une mesure
     * d'endiguement pour une correction, et personne n'a jamais pu enregistrer
     * un endiguement avant que ce type n'existe.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private CapaActionType actionType = CapaActionType.CORRECTIVE;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    /**
     * Nom lisible du porteur, figé au moment de la décision.
     *
     * <p>Il double {@link #assigneeId} au lieu de le remplacer : l'identifiant
     * reste ce qui rattache l'action à un compte, le nom est ce qui se lit dans
     * un dossier d'audit. Le nom est FIGÉ et non résolu à l'affichage — un
     * dossier doit montrer qui portait l'action au moment où elle a été décidée,
     * pas ce qu'un annuaire vivant répondrait après un départ (cf. ADR 0052).
     */
    @Column(name = "assignee_name", length = 255)
    private String assigneeName;

    /**
     * Jour où l'action a été DÉCIDÉE (comité, revue de direction, réunion CAPA).
     *
     * <p>Distinct de {@link #createdAt}, qui est la date de saisie dans l'outil.
     * Les deux coïncident souvent, jamais par construction : une action décidée
     * en comité et saisie trois semaines plus tard porterait sinon une date qui
     * ment sur la chronologie du dossier.
     */
    @Column(name = "decided_on")
    private LocalDate decidedOn;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

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
            this.status = CapaActionStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
