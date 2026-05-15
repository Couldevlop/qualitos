package com.openlab.qualitos.quality.webhooks;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WebhookDto {

    private WebhookDto() {}

    public record CreateSubscriptionRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 2048)
            @Pattern(regexp = "^https?://.+", message = "endpointUrl must be http(s) URL")
            String endpointUrl,
            @NotEmpty List<EventType> eventTypes,
            @NotBlank @Size(min = 16, max = 128,
                    message = "secret must be 16..128 chars (HMAC key)")
            String secret,
            @Min(0) @Max(10) Integer maxRetries,
            @NotNull UUID createdBy
    ) {}

    public record UpdateSubscriptionRequest(
            @Size(max = 255) String name,
            @Size(max = 2048)
            @Pattern(regexp = "^https?://.+", message = "endpointUrl must be http(s) URL")
            String endpointUrl,
            List<EventType> eventTypes,
            @Size(min = 16, max = 128) String secret,
            @Min(0) @Max(10) Integer maxRetries,
            SubscriptionStatus status
    ) {}

    public record SubscriptionResponse(
            UUID id,
            UUID tenantId,
            String name,
            String endpointUrl,
            List<String> eventTypes,
            // secret never returned — only echoed once at creation
            SubscriptionStatus status,
            int maxRetries,
            int consecutiveFailures,
            Instant lastTriggeredAt,
            Instant lastSuccessAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreatedSubscriptionResponse(
            SubscriptionResponse subscription,
            /** Secret retourne UNE seule fois a la creation, pour copie cote client. */
            String secret
    ) {}

    public record DeliveryResponse(
            UUID id,
            UUID tenantId,
            UUID subscriptionId,
            String eventId,
            EventType eventType,
            String payload,
            DeliveryStatus status,
            int attemptCount,
            Instant lastAttemptAt,
            Instant nextRetryAt,
            Integer responseStatusCode,
            String responseBody,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TestPingResponse(
            UUID deliveryId,
            DeliveryStatus status,
            Integer responseStatusCode,
            String errorMessage
    ) {}
}
