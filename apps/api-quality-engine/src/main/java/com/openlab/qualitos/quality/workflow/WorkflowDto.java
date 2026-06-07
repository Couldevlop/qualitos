package com.openlab.qualitos.quality.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class WorkflowDto {

    private WorkflowDto() {}

    /** Taille maximale du diagramme BPMN (512 Ko de caractères). */
    public static final int MAX_BPMN_CHARS = 512 * 1024;

    public record CreateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = MAX_BPMN_CHARS) String bpmnXml
    ) {}

    /** Mise à jour partielle : seuls les champs non nuls sont appliqués. */
    public record UpdateRequest(
            @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @Size(max = MAX_BPMN_CHARS) String bpmnXml
    ) {}

    public record Response(
            UUID id,
            UUID tenantId,
            String name,
            String description,
            String bpmnXml,
            WorkflowStatus status,
            int version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Vue allégée pour les listes (cartes) : sans le XML, potentiellement volumineux. */
    public record Summary(
            UUID id,
            UUID tenantId,
            String name,
            String description,
            WorkflowStatus status,
            int version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
