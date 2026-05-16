package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class DpoAppointmentMapper {
    private DpoAppointmentMapper() {}

    static DpoAppointmentJpaEntity toEntity(DpoAppointment a, DpoAppointmentJpaEntity target) {
        DpoAppointmentJpaEntity e = target != null ? target : new DpoAppointmentJpaEntity();
        if (a.getId() != null) e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setReference(a.getReference());
        e.setDpoFullName(a.getDpoFullName());
        e.setDpoEmail(a.getDpoEmail());
        e.setDpoPhone(a.getDpoPhone());
        e.setDpoType(a.getDpoType());
        e.setExternalCompanyName(a.getExternalCompanyName());
        e.setQualifications(a.getQualifications());
        e.setScope(a.getScope());
        e.setEffectiveFrom(a.getEffectiveFrom());
        e.setEffectiveTo(a.getEffectiveTo());
        e.setRegulatorNotifiedAt(a.getRegulatorNotifiedAt());
        e.setRegulatorNotificationReference(a.getRegulatorNotificationReference());
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(a.getLinkedProcessingActivityIds()));
        e.setStatus(a.getStatus());
        e.setEndReason(a.getEndReason());
        e.setCreatedByUserId(a.getCreatedByUserId());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    static DpoAppointment toDomain(DpoAppointmentJpaEntity e) {
        return new DpoAppointment(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getDpoFullName(), e.getDpoEmail(), e.getDpoPhone(),
                e.getDpoType(), e.getExternalCompanyName(), e.getQualifications(),
                e.getScope(),
                e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getRegulatorNotifiedAt(), e.getRegulatorNotificationReference(),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                e.getStatus(), e.getEndReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String uuidSetToCsv(Set<UUID> s) {
        if (s == null || s.isEmpty()) return null;
        return s.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private static Set<UUID> csvToUuidSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
