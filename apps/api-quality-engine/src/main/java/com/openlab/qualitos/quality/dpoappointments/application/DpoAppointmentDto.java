package com.openlab.qualitos.quality.dpoappointments.application;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class DpoAppointmentDto {

    private DpoAppointmentDto() {}

    public record ProposeRequest(
            String reference,
            String dpoFullName,
            String dpoEmail,
            String dpoPhone,
            DpoType dpoType,
            String externalCompanyName,
            String qualifications,
            String scope,
            Set<UUID> linkedProcessingActivityIds,
            UUID createdByUserId) {}

    public record EditRequest(
            String dpoFullName,
            String dpoEmail,
            String dpoPhone,
            DpoType dpoType,
            String externalCompanyName,
            String qualifications,
            Set<UUID> linkedProcessingActivityIds) {}

    public record ActivateRequest(
            Instant effectiveFrom,
            Instant regulatorNotifiedAt,
            String regulatorNotificationReference) {}

    public record EndRequest(String reason, Instant effectiveTo) {}
    public record CancelRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference,
            String dpoFullName, String dpoEmail, String dpoPhone,
            DpoType dpoType, String externalCompanyName, String qualifications,
            String scope,
            Instant effectiveFrom, Instant effectiveTo,
            Instant regulatorNotifiedAt, String regulatorNotificationReference,
            Set<UUID> linkedProcessingActivityIds,
            DpoAppointmentStatus status, String endReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(DpoAppointment a) {
            return new View(
                    a.getId(), a.getTenantId(), a.getReference(),
                    a.getDpoFullName(), a.getDpoEmail(), a.getDpoPhone(),
                    a.getDpoType(), a.getExternalCompanyName(), a.getQualifications(),
                    a.getScope(),
                    a.getEffectiveFrom(), a.getEffectiveTo(),
                    a.getRegulatorNotifiedAt(), a.getRegulatorNotificationReference(),
                    a.getLinkedProcessingActivityIds(),
                    a.getStatus(), a.getEndReason(),
                    a.getCreatedByUserId(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
