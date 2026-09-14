package com.openlab.qualitos.quality.apqp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
     * Un projet tel que la liste le reçoit.
     *
     * <p>Les trois compteurs voyagent AVEC le projet : la liste dit d'un coup
     * d'œil où en est chaque programme, et les recalculer à l'ouverture de chacun
     * demanderait autant d'appels que de lignes.
     */
    public record ProjectResponse(
            UUID id,
            String name,
            ApqpProjectType type,
            String customer,
            String reference,
            String description,
            int deliverablesTotal,
            int deliverablesDone,
            int ppapTotal,
            int ppapDone,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * Ouverture d'un projet.
     *
     * <p>Le type est exigé et non déduit : c'est sur lui qu'on filtre la liste,
     * et le deviner de l'intitulé ne marcherait que sur les intitulés bien
     * choisis.
     */
    public record CreateProjectRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull ApqpProjectType type,
            @Size(max = 255) String customer,
            @Size(max = 120) String reference,
            @Size(max = 2000) String description) {}

    /** Modification d'un projet. Même forme : tout se corrige, sauf son cycle. */
    public record UpdateProjectRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull ApqpProjectType type,
            @Size(max = 255) String customer,
            @Size(max = 120) String reference,
            @Size(max = 2000) String description) {}

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
     * Un livrable tel que l'écran le reçoit.
     *
     * <p>Les colonnes du classeur du client, et elles seules : artefact attendu,
     * PPAP requis, responsable, échéance, statut, avancement, notes — plus la
     * case, ce qui la prouve, et le renvoi facultatif. Un seul jeu de champs pour
     * TOUS les livrables (ADR 0072).
     *
     * <p>{@code evidenceCount} plutôt que les pièces elles-mêmes : la liste d'une
     * phase doit pouvoir dire « prouvé » sans réclamer une URL présignée par
     * fichier, qui coûterait un appel au stockage pour une icône.
     */
    public record DeliverableResponse(
            UUID id,
            int position,
            String label,
            String expectedArtifact,
            boolean ppap,
            String owner,
            LocalDate dueDate,
            ApqpDeliverableStatus status,
            int percentComplete,
            boolean done,
            Instant doneAt,
            UUID doneBy,
            String comment,
            ApqpLinkedKind linkedKind,
            UUID linkedId,
            int evidenceCount) {}

    /**
     * Le cycle d'un projet, et l'état de son dossier PPAP.
     *
     * <p>Le compte voyage AVEC le cycle plutôt que dans un appel à part : la
     * section PPAP surmonte le même cycle que le schéma, et deux requêtes
     * pourraient se répondre sur deux états différents.
     */
    public record CycleResponse(
            UUID projectId,
            String projectName,
            ApqpProjectType projectType,
            String customer,
            List<PhaseResponse> phases,
            int ppapDone,
            int ppapTotal) {}

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
     * Création ou reformulation d'un livrable : ce qu'on attend, et sous quelle
     * forme.
     *
     * <p>Plus de genre : les quatre genres fermés qui décidaient du formulaire
     * ont disparu (ADR 0072). Reste l'artefact attendu, qui est la seule chose
     * que le genre prétendait dire — et qui la dit en clair.
     */
    public record DeliverableRequest(
            @NotBlank @Size(max = 500) String label,
            @Size(max = 1000) String expectedArtifact,
            boolean ppap) {}

    /**
     * Ce qu'un utilisateur déclare d'un livrable : le formulaire UNIQUE.
     *
     * <p>Ni {@code doneAt} ni {@code doneBy} : l'heure vient du serveur et
     * l'acteur du jeton. Les accepter du corps laisserait antidater un livrable
     * et l'attribuer à quelqu'un d'autre — exactement ce qu'une piste d'audit
     * doit empêcher.
     *
     * <p>{@code status} et {@code percentComplete} sont facultatifs : absents,
     * ils sont déduits de la case (ADR 0072), ce qui permet à la liste de cocher
     * sans rien savoir du reste du formulaire.
     */
    public record CompletionRequest(
            boolean done,
            @Size(max = 1000) String expectedArtifact,
            Boolean ppap,
            @Size(max = 150) String owner,
            LocalDate dueDate,
            ApqpDeliverableStatus status,
            @Min(0) @Max(100) Integer percentComplete,
            @Size(max = 2000) String comment,
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
