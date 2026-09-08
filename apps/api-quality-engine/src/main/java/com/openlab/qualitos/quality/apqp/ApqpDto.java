package com.openlab.qualitos.quality.apqp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record DeliverableResponse(UUID id, int position, String label) {}

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

    public record DeliverableRequest(@NotBlank @Size(max = 500) String label) {}

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
