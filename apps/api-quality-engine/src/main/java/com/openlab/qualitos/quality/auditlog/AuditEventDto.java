package com.openlab.qualitos.quality.auditlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditEventDto {

    private AuditEventDto() {}

    public record RecordEventRequest(
            Instant occurredAt,
            @NotNull ActorType actorType,
            UUID actorUserId,
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,99}$",
                    message = "action must be lowercase dot/snake (2..100 chars)")
            String action,
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$",
                    message = "resourceType must be lowercase kebab (2..64 chars)")
            String resourceType,
            UUID resourceId,
            @Size(max = 500) String summary,
            String payloadJson,
            @Size(max = 64) String ipAddress,
            @Size(max = 500) String userAgent
    ) {}

    public record EventResponse(
            UUID id,
            UUID tenantId,
            long sequenceNo,
            Instant occurredAt,
            Instant recordedAt,
            ActorType actorType,
            UUID actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            String summary,
            String payloadJson,
            String ipAddress,
            String userAgent,
            String integrityHash,
            String previousHash,
            String blockchainTxRef
    ) {}

    public record ChainBreak(
            UUID eventId,
            long sequenceNo,
            String reason
    ) {}

    public record ChainVerification(
            UUID tenantId,
            long fromSequenceNo,
            long toSequenceNo,
            long verifiedCount,
            boolean valid,
            List<ChainBreak> breaks
    ) {}

    public record AnchorRequest(
            @NotBlank @Size(max = 200) String blockchainTxRef
    ) {}
}
