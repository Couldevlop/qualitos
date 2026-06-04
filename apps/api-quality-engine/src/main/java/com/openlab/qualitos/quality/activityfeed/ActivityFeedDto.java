package com.openlab.qualitos.quality.activityfeed;

import java.time.Instant;
import java.util.UUID;

/** DTOs du read-model "flux d'activité" (lecture seule). */
public final class ActivityFeedDto {

    private ActivityFeedDto() {}

    /** Vue exposée d'une entrée du flux d'activité. */
    public record View(
            UUID id,
            long sequenceNo,
            Instant occurredAt,
            Instant recordedAt,
            String action,
            String resourceType,
            UUID resourceId,
            UUID actorUserId,
            String summary) {

        /** Projection depuis l'entité du read-model. */
        public static View from(AuditActivityEntry e) {
            return new View(
                    e.getId(),
                    e.getSequenceNo(),
                    e.getOccurredAt(),
                    e.getRecordedAt(),
                    e.getAction(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getActorUserId(),
                    e.getSummary());
        }
    }
}
