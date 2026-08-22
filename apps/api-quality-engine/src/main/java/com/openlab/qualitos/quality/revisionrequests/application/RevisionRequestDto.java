package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Vues des demandes de révision. La justification est exposée telle quelle :
 * une proposition qu'on ne peut pas contester n'est pas une proposition qu'on
 * peut accepter en conscience.
 */
public final class RevisionRequestDto {

    private RevisionRequestDto() {}

    public record RejectCommand(String note) {}

    public record View(
            UUID id, UUID productId, RevisionTargetType targetType, UUID targetId,
            RevisionTriggerType triggerType, UUID triggerRefId, String triggerRefLabel,
            String rationale, String field, String from, String to, String draftJson,
            RevisionRequestStatus status, UUID decidedBy, Instant decidedAt, String decisionNote,
            Instant createdAt, Instant updatedAt) {

        public static View of(RevisionRequest r) {
            return new View(
                    r.getId(), r.getProductId(), r.getTargetType(), r.getTargetId(),
                    r.getTriggerType(), r.getTriggerRefId(), r.getTriggerRefLabel(),
                    r.getRationale(), r.getChange().field(), r.getChange().from(),
                    r.getChange().to(), r.getChange().draftJson(), r.getStatus(),
                    r.getDecidedBy(), r.getDecidedAt(), r.getDecisionNote(),
                    r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
