package com.openlab.qualitos.quality.apqp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApqpDto {

    private ApqpDto() {}

    /**
     * Une phase telle que l'écran la reçoit.
     *
     * <p>{@code level} est calculé, pas stocké : c'est le rang du jalon dans le
     * V (1 en haut, le plus grand au point bas). Le déduire côté serveur évite
     * que deux écrans en donnent deux lectures différentes le jour où l'on
     * ajoute une phase.
     */
    public record PhaseResponse(
            UUID id,
            int position,
            int level,
            String title,
            String purpose,
            String question,
            List<DeliverableResponse> deliverables) {}

    /**
     * Une ligne du contenu d'un livrable.
     *
     * <p>Une seule forme pour les deux genres qui en portent : un sous-point
     * n'utilise que {@code label} et {@code checked}, une mesure que
     * {@code label}, {@code value}, {@code unit} et {@code measuredAt}. Deux
     * records auraient obligé l'écran à choisir son type avant d'avoir lu le
     * genre du livrable.
     */
    public record DataRow(
            @NotBlank @Size(max = 200) String label,
            @Size(max = 60) String value,
            @Size(max = 20) String unit,
            LocalDate measuredAt,
            Boolean checked) {}

    /**
     * Un livrable tel que l'écran le reçoit.
     *
     * <p>{@code evidenceCount} plutôt que les pièces elles-mêmes : la liste d'une
     * phase doit pouvoir dire « prouvé » sans réclamer une URL présignée par
     * fichier, qui coûterait un appel au stockage pour une icône.
     */
    public record DeliverableResponse(
            UUID id,
            int position,
            String label,
            boolean ppap,
            ApqpDeliverableKind kind,
            boolean done,
            Instant doneAt,
            UUID doneBy,
            String comment,
            List<DataRow> data,
            ApqpLinkedKind linkedKind,
            UUID linkedId,
            int evidenceCount) {}

    /**
     * Le cycle, et l'état du dossier PPAP.
     *
     * <p>Le compte voyage AVEC le cycle plutôt que dans un appel à part : la
     * section PPAP surmonte le même cycle que le schéma, et deux requêtes
     * pourraient se répondre sur deux états différents.
     */
    public record CycleResponse(List<PhaseResponse> phases, int ppapDone, int ppapTotal) {}

    /**
     * Création d'une phase. Les livrables se posent ensuite, un par un : une
     * phase naît d'un intitulé, on la remplit après.
     */
    public record CreatePhaseRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 500) String purpose,
            @Size(max = 500) String question) {}

    /**
     * Modification d'une phase.
     *
     * <p>L'intitulé reste obligatoire : une phase sans nom n'est plus repérable
     * dans le V, qui n'affiche que cela.
     */
    public record UpdatePhaseRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 500) String purpose,
            @Size(max = 500) String question) {}

    /**
     * Création ou reformulation d'un livrable : son intitulé, son genre, sa marque.
     *
     * <p>Le genre est exigé et non déduit du libellé : deviner qu'« Control plan »
     * renvoie au module des plans de surveillance marcherait sur le référentiel et
     * sur rien d'autre, et personne ne comprendrait pourquoi son propre libellé
     * n'ouvre pas le même formulaire.
     */
    public record DeliverableRequest(
            @NotBlank @Size(max = 500) String label,
            boolean ppap,
            @NotNull ApqpDeliverableKind kind) {}

    /**
     * Ce qu'un utilisateur déclare d'un livrable.
     *
     * <p>Ni {@code doneAt} ni {@code doneBy} : l'heure vient du serveur et
     * l'acteur du jeton. Les accepter du corps laisserait antidater un livrable
     * et l'attribuer à quelqu'un d'autre — exactement ce qu'une piste d'audit
     * doit empêcher.
     */
    public record CompletionRequest(
            boolean done,
            @Size(max = 2000) String comment,
            @Valid List<DataRow> data,
            ApqpLinkedKind linkedKind,
            UUID linkedId) {}

    /**
     * Réorganisation du cycle : la liste des identifiants dans leur nouvel
     * ordre.
     *
     * <p>Tout le cycle d'un coup, et non un déplacement à la fois : le V se lit
     * comme un ensemble, et une suite de déplacements unitaires laisserait
     * l'écran dans des états intermédiaires qui n'ont pas de sens.
     */
    public record ReorderRequest(List<UUID> phaseIds) {}
}
