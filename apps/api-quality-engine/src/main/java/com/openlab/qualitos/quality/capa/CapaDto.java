package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CapaDto {

    private CapaDto() {}

    public record CreateCaseRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull CapaType type,
            @NotNull CapaCriticity criticity,
            @NotNull CapaSourceType sourceType,
            @Size(max = 255) String sourceRef,
            @NotNull UUID ownerId,
            UUID rootCauseId,
            LocalDate dueDate
    ) {}

    /**
     * Ouverture d'un dossier ET de son action unique, en une seule écriture
     * (ADR 0088) — ce que remplit la fenêtre de création, d'où qu'on l'ouvre.
     *
     * <p>Une CAPA porte une action : l'intitulé, la nature, le responsable et
     * l'échéance du dossier SONT ceux de l'action. Le responsable devient le
     * pilote du dossier ; il est désigné dans l'annuaire du client, d'où son
     * identifiant et son libellé recopié.
     *
     * <p>Ni le tenant ni l'auteur n'y figurent : ils viennent du jeton. La
     * source non plus quand le dossier s'ouvre depuis une NC, un constat ou un
     * risque — c'est l'écran d'origine qui la fixe côté serveur ; les deux
     * champs ne servent qu'à la création libre depuis la liste.
     *
     * <p>La décision de vérification d'efficacité (ADR 0073) part avec
     * l'ouverture : c'est au moment où l'on décide de l'action qu'on décide si
     * son effet sera vérifié. {@code verificationRequired} est un
     * {@code Boolean} : {@code null} laisse la question ouverte, comme sur un
     * dossier ouvert par un automate ; {@code FALSE} la tranche. Les mêmes
     * gardes que la mise à jour s'appliquent — exiger sans désigner, ou
     * désigner sans exiger, est refusé.
     */
    public record OpenCaseRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @NotNull CapaType type,
            /**
             * La nature de l'ACTION du dossier, quand elle diffère de celle du
             * dossier : une mesure d'endiguement prise le jour même se tient dans
             * un dossier correctif, qui restera clôturable quand la cause sera
             * traitée (ISO 9001 §10.2, étape D3 de la 8D).
             *
             * <p>Absente, elle se déduit du type du dossier — le cas de presque
             * toutes les ouvertures.
             */
            CapaActionType actionType,
            @NotNull CapaCriticity criticity,
            CapaSourceType sourceType,
            @Size(max = 255) String sourceRef,
            @NotNull UUID ownerId,
            @Size(max = 255) String ownerName,
            @NotNull LocalDate dueDate,
            Boolean verificationRequired,
            UUID verificationAssigneeId,
            @Size(max = 255) String verificationAssigneeName,
            @Size(max = 4000) String verificationInstructions,
            /**
             * Le critère d'efficacité (ADR 0108) : la phrase qui dit à quoi on
             * reconnaîtra que l'action a marché — « zéro erreur de référence sur
             * 90 jours ».
             *
             * <p>Il part avec l'ouverture, et c'est tout l'objet. Sans lui, le
             * critère se décidait au moment de juger, par la personne qui avait
             * mené l'action, en regardant le résultat obtenu : un critère écrit
             * après la mesure n'est pas un critère, c'est un commentaire.
             */
            @Size(max = 2000) String effectivenessCriterion
    ) {
        /**
         * L'ouverture sans nature d'action explicite — la forme de presque tous
         * les appelants : l'action prend la nature que le type du dossier implique.
         */
        public OpenCaseRequest(String title, String description, CapaType type, CapaCriticity criticity,
                               CapaSourceType sourceType, String sourceRef, UUID ownerId, String ownerName,
                               LocalDate dueDate, Boolean verificationRequired, UUID verificationAssigneeId,
                               String verificationAssigneeName, String verificationInstructions) {
            this(title, description, type, null, criticity, sourceType, sourceRef, ownerId, ownerName,
                    dueDate, verificationRequired, verificationAssigneeId, verificationAssigneeName,
                    verificationInstructions, null);
        }
    }

    /**
     * Mise à jour PARTIELLE : un champ nul veut dire « ne touche pas ».
     *
     * <p>C'est pourquoi {@code verificationRequired} est un {@code Boolean} et
     * non un {@code boolean} : {@code null} laisse la décision en l'état,
     * {@code FALSE} la prend explicitement. Les deux ne disent pas la même chose.
     */
    public record UpdateCaseRequest(
            @Size(max = 255) String title,
            String description,
            CapaCriticity criticity,
            @Size(max = 255) String sourceRef,
            UUID rootCauseId,
            LocalDate dueDate,
            Boolean verificationRequired,
            UUID verificationAssigneeId,
            @Size(max = 255) String verificationAssigneeName,
            @Size(max = 4000) String verificationInstructions,
            /** Le critère d'efficacité (ADR 0108) ; null ne touche pas à l'existant. */
            @Size(max = 2000) String effectivenessCriterion
    ) {}

    /**
     * L'évaluation d'efficacité (ADR 0108).
     *
     * <p><b>Deux façons de dire le verdict, et une seule à la fois.</b>
     * {@code verdict} est la forme à employer ; {@code effective} est l'ancien
     * champ booléen, conservé parce que l'API est publique et versionnée (§13.1)
     * — un intégrateur qui envoie {@code {"effective": true}} depuis un ERP ne
     * doit pas découvrir la rupture en production.
     *
     * <p>Fournir les deux, ou n'en fournir aucun, est refusé en 400. Le contrôle
     * est au service et non ici : une annotation ne sait pas exprimer « exactement
     * l'un des deux », et un message rédigé vaut mieux qu'un refus muet.
     *
     * <p>L'ancien champ ne permet pas d'exprimer {@code PARTIAL}, ce qui est la
     * bonne incitation à passer à {@code verdict}.
     *
     * @param observed  ce qui a été CONSTATÉ ; obligatoire dès qu'on emploie
     *                  {@code verdict} — on juge sur un relevé, ou on ne juge
     *                  pas. Facultatif sur l'ancien champ, qui n'en demandait
     *                  aucun : l'exiger casserait les appelants existants.
     * @param baseline  la situation avant. Facultative : un problème nouveau n'a
     *                  pas d'antériorité chiffrée.
     * @param target    la cible. Facultative : le critère peut déjà la dire en
     *                  toutes lettres.
     */
    public record EffectivenessRequest(
            @Deprecated Boolean effective,
            EffectivenessVerdict verdict,
            @Size(max = 120) String baseline,
            @Size(max = 120) String target,
            @Size(max = 120) String observed,
            @Size(max = 4000) String comment
    ) {

        /** L'ancienne forme, pour les appelants et les tests qui l'emploient encore. */
        public EffectivenessRequest(Boolean effective) {
            this(effective, null, null, null, null, null);
        }
    }

    /**
     * Création ET mise à jour partielle d'une action.
     *
     * <p>{@code @NotBlank} ne vaut qu'à la création : la mise à jour est un
     * PATCH, où un champ absent signifie « ne touche pas ». Le contrôleur ne
     * valide donc pas ce record sur le PATCH, et c'est le service qui refuse
     * un libellé vide ou trop long — sans quoi l'édition en ligne du tableau
     * pourrait effacer le libellé d'une action.
     */
    public record ActionRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            CapaActionStatus status,
            // Nature de l'action. Absente à la création = CORRECTIVE, le cas de
            // loin le plus fréquent ; absente au PATCH = « ne touche pas ».
            CapaActionType actionType,
            UUID assigneeId,
            @Size(max = 255) String assigneeName,
            LocalDate decidedOn,
            LocalDate dueDate
    ) {}

    public record CaseResponse(
            UUID id,
            UUID tenantId,
            String title,
            String description,
            CapaType type,
            CapaCriticity criticity,
            CapaStatus status,
            CapaSourceType sourceType,
            String sourceRef,
            UUID ownerId,
            // Libellé du pilote recopié de l'annuaire (ADR 0088) ; nul sur les
            // dossiers antérieurs ou ouverts par un automate.
            String ownerName,
            UUID rootCauseId,
            LocalDate dueDate,
            Instant resolvedAt,
            Instant closedAt,
            // Projection du dernier verdict (ADR 0108) : EFFECTIVE -> true, les
            // deux autres -> false. Conservé parce que l'écran d'efficacité
            // MESURÉE l'affiche à côté du taux de récidive, et parce que les
            // dossiers antérieurs à l'ADR n'ont pas de verdict et n'en auront
            // jamais. Ce n'est plus la source de vérité.
            Boolean effectivenessVerified,
            Instant effectivenessVerifiedAt,
            // Le critère d'efficacité posé sur le dossier, et le dernier verdict
            // rendu. Nuls ensemble sur un dossier jamais évalué ; le critère seul
            // sur un dossier qui attend son jugement.
            String effectivenessCriterion,
            EffectivenessVerdict effectivenessVerdict,
            // L'EXIGENCE de vérification, à distinguer du constat qu'elle a eu
            // lieu. `null` = la question n'a pas été tranchée sur ce dossier.
            Boolean verificationRequired,
            UUID verificationAssigneeId,
            String verificationAssigneeName,
            String verificationInstructions,
            Instant createdAt,
            Instant updatedAt,
            List<ActionResponse> actions,
            // Non-conformité dont procède le dossier, donc dont procèdent ses
            // actions. Portée par le dossier et non répétée sur chaque action :
            // c'est une propriété du dossier, et la répéter N fois laisserait
            // croire qu'elle peut différer d'une ligne à l'autre. Nulle quand le
            // dossier ne vient pas d'un écart.
            LinkedNonConformity sourceNonConformity,
            // Ce qui s'oppose encore à la clôture, énuméré à l'avance. Renseigné
            // sur la FICHE seulement — comme sourceNonConformity, et pour la même
            // raison : la liste paginée n'en montre rien et le calcul coûterait
            // une requête par ligne. Jamais nul sur la fiche : une liste vide dit
            // « rien ne s'y oppose », ce qui est une information.
            List<ClosureBlocker> closureBlockers
    ) {}

    /**
     * Un motif de refus de clôture, énoncé AVANT le clic.
     *
     * <p>Un code et un décompte, pas une phrase : la phrase se construit côté
     * écran, dans la langue de l'utilisateur. Renvoyer un texte tout fait
     * l'aurait figé en français dans une interface qui parle six langues, et
     * aurait obligé le navigateur à afficher un message serveur qu'il ne peut
     * ni traduire ni mettre en forme.
     */
    public record ClosureBlocker(
            ClosureBlockerCode code,
            /** Nombre d'éléments concernés — actions restantes, écarts ouverts… */
            long count
    ) {}

    /** Écart d'origine, réduit à ce qu'un tableau doit en montrer. */
    public record LinkedNonConformity(
            UUID id,
            String reference,
            String title
    ) {}

    public record ActionResponse(
            UUID id,
            UUID capaId,
            String title,
            String description,
            CapaActionStatus status,
            CapaActionType actionType,
            UUID assigneeId,
            String assigneeName,
            LocalDate decidedOn,
            LocalDate dueDate,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
