package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un livrable attendu en sortie de phase.
 *
 * <p>Ce que la phase doit avoir produit avant qu'on passe à la suivante :
 * « AMDEC processus (PFMEA) », « Plan de surveillance de pré-lancement ». La
 * liste vient du manuel AIAG à l'amorçage, puis appartient au client.
 *
 * <p>Ses colonnes sont celles du classeur de suivi que le commanditaire tient
 * déjà — artefact attendu, PPAP requis, responsable, échéance, statut,
 * avancement, notes — et elles sont les MÊMES pour tous les livrables (ADR
 * 0072). Le genre fermé qui décidait autrefois du formulaire a disparu : il
 * empêchait de cocher la moitié des lignes et multipliait les écrans pour une
 * seule question, « où en est-on ? ».
 */
@Entity
@Table(name = "apqp_deliverables")
@Getter
@Setter
@NoArgsConstructor
public class ApqpDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * La phase qui l'attend.
     *
     * <p>{@code LAZY} : les listes de phases affichent parfois leurs livrables
     * sans avoir besoin de remonter à la phase depuis chacun d'eux.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private ApqpPhase phase;

    /** Rang dans la liste, à partir de 1. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 500)
    private String label;

    /**
     * Clé du référentiel, ou {@code null} si le livrable vient du client.
     *
     * <p>Même règle que pour la phase : traduit tant qu'il n'est pas retouché,
     * littéral ensuite.
     */
    @Column(name = "reference_key", length = 80)
    private String referenceKey;

    /**
     * L'artefact attendu — colonne D du classeur.
     *
     * <p>Le libellé dit CE QU'ON DOIT PRODUIRE, l'artefact dit SOUS QUELLE
     * FORME : « Plan de surveillance » et « Plan de surveillance de
     * pré-lancement (entrées, spécification, méthode, taille et fréquence
     * d'échantillon, plan de réaction) » ne se vérifient pas pareil. C'est ce
     * second texte qu'un auditeur confronte à la pièce versée.
     *
     * <p>{@code null} tant que personne ne l'a écrit : la lecture rend alors
     * celui du référentiel, dans la langue demandée.
     */
    @Column(name = "expected_artifact", length = 1000)
    private String expectedArtifact;

    /**
     * Élément du dossier PPAP — colonne E du classeur.
     *
     * <p>Porté par le livrable et non par une liste à part : le dossier PPAP est
     * une vue du cycle, qui appartient au client et qu'il adapte. Amorcé sur les
     * douze « Y » du classeur, puis piloté livrable par livrable — ce n'est plus
     * une marque figée du référentiel (ADR 0072).
     */
    @Column(nullable = false)
    private boolean ppap;

    /** Le responsable — colonne F. Un nom, pas un compte : il peut être externe. */
    @Column(length = 150)
    private String owner;

    /** L'échéance — colonne G. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Où en est le livrable — colonne H.
     *
     * <p>Tenu d'accord avec {@code done} par le service : cocher pose
     * {@code DONE}, décocher le retire.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApqpDeliverableStatus status = ApqpDeliverableStatus.NOT_STARTED;

    /** L'avancement en pourcentage — colonne I, de 0 à 100. */
    @Column(name = "percent_complete", nullable = false)
    private int percentComplete;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "done_at")
    private Instant doneAt;

    /**
     * Qui a coché, pris du jeton — jamais du corps de la requête.
     *
     * <p>L'accepter de l'appelant laisserait attribuer un achèvement à quelqu'un
     * d'autre, et c'est cette attribution que l'auditeur lit.
     */
    @Column(name = "done_by")
    private UUID doneBy;

    /** Notes — colonne J. */
    @Column(length = 2000)
    private String comment;

    /**
     * Module visé par le renvoi facultatif, ou {@code null}.
     *
     * <p>Renvoyer vers une AMDEC, un cycle PDCA ou une CAPA n'est plus un genre
     * de livrable mais un CHAMP de plus (ADR 0072) : tout livrable peut porter
     * son enregistrement, et aucun n'y est obligé. L'existence de
     * l'enregistrement reste vérifiée dans le tenant ({@code ApqpLinkResolver}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "linked_kind", length = 32)
    private ApqpLinkedKind linkedKind;

    @Column(name = "linked_id")
    private UUID linkedId;

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
