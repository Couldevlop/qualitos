package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CommDto {

    private CommDto() {}

    public record CreateConnectionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull CommProvider provider,
            // L'URL d'incoming-webhook EST le secret : exigée en https:// (SSRF + confidentialité).
            @NotBlank @Size(min = 8, max = 1024)
            @Pattern(regexp = "^https://.+", message = "webhookUrl must be https://")
            String webhookUrl,
            @Size(max = 200) String channel,
            @NotNull UUID createdBy
    ) {}

    public record UpdateConnectionRequest(
            @Size(max = 120) String name,
            @Size(min = 8, max = 1024)
            @Pattern(regexp = "^https://.+", message = "webhookUrl must be https://")
            String webhookUrl,
            @Size(max = 200) String channel,
            ConnectionStatus status
    ) {}

    /** Réponse connexion : N'EXPOSE JAMAIS l'URL webhook (le secret). */
    public record ConnectionResponse(
            UUID id,
            UUID tenantId,
            String name,
            CommProvider provider,
            String channel,
            ConnectionStatus status,
            int consecutiveFailures,
            Instant lastNotifiedAt,
            Instant lastSuccessAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Résultat d'un envoi de test sur une connexion. */
    public record TestResult(
            UUID connectionId,
            boolean success,
            String errorMessage
    ) {}
}
