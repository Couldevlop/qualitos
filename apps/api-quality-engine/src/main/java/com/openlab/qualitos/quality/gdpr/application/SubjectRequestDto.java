package com.openlab.qualitos.quality.gdpr.application;

import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;

import java.time.Instant;
import java.util.UUID;

public final class SubjectRequestDto {

    private SubjectRequestDto() {}

    /** subjectIdentifier en clair — sera HASHÉ par le service avant persistance. */
    public record ReceiveRequest(
            SubjectRequestType type,
            String subjectIdentifier,
            String subjectIdentifierLabel,
            UUID requestedByUserId) {}

    public record StartProcessingRequest(UUID handledByUserId) {}
    public record CompleteRequest(String resolutionNotes, String evidenceUrl, UUID handledByUserId) {}
    public record RejectRequest(String reason, UUID handledByUserId) {}
    public record ExtendDeadlineRequest(Instant newDeadline) {}

    public record View(
            UUID id, UUID tenantId, SubjectRequestType type,
            String subjectIdentifierHash, String subjectIdentifierLabel,
            SubjectRequestStatus status,
            Instant receivedAt, Instant deadlineAt, boolean extended,
            Instant inProgressAt, Instant completedAt,
            String rejectionReason, String resolutionNotes, String evidenceUrl,
            UUID requestedByUserId, UUID handledByUserId,
            Instant updatedAt, boolean overdue
    ) {
        public static View of(DataSubjectRequest r, Instant now) {
            return new View(
                    r.getId(), r.getTenantId(), r.getType(),
                    r.getSubjectIdentifierHash(), r.getSubjectIdentifierLabel(),
                    r.getStatus(), r.getReceivedAt(), r.getDeadlineAt(), r.isExtended(),
                    r.getInProgressAt(), r.getCompletedAt(),
                    r.getRejectionReason(), r.getResolutionNotes(), r.getEvidenceUrl(),
                    r.getRequestedByUserId(), r.getHandledByUserId(),
                    r.getUpdatedAt(), r.isOverdue(now));
        }
    }
}
