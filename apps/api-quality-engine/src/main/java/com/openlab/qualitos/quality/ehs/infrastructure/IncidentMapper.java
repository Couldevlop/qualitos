package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.ehs.domain.Incident;

/**
 * Mapper bidirectionnel entre l'agrégat domaine et l'entité JPA.
 * Isolé pour faciliter les tests et garder le service application pur.
 */
final class IncidentMapper {

    private IncidentMapper() {}

    static Incident toDomain(IncidentJpaEntity e) {
        return new Incident(
                e.getId(), e.getTenantId(), e.getCode(), e.getTitle(), e.getDescription(),
                e.getType(), e.getSeverity(), e.getStatus(),
                e.getOccurredAt(), e.getReportedAt(),
                e.getMitigatedAt(), e.getClosedAt(),
                e.getLocation(), e.getPersonsInvolved(),
                e.getRootCause(), e.getCorrectiveActions(), e.getStandardsCsv(),
                e.getCapaCaseId(), e.getNcId(), e.getOwnerUserId(), e.getReportedBy(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    /** Met à jour {@code e} à partir de {@code i} (création si {@code e == null}). */
    static IncidentJpaEntity toEntity(Incident i, IncidentJpaEntity existing) {
        IncidentJpaEntity e = existing != null ? existing : new IncidentJpaEntity();
        if (i.getId() != null) e.setId(i.getId());
        e.setTenantId(i.getTenantId());
        e.setCode(i.getCode());
        e.setTitle(i.getTitle());
        e.setDescription(i.getDescription());
        e.setType(i.getType());
        e.setSeverity(i.getSeverity());
        e.setStatus(i.getStatus());
        e.setOccurredAt(i.getOccurredAt());
        e.setReportedAt(i.getReportedAt());
        e.setMitigatedAt(i.getMitigatedAt());
        e.setClosedAt(i.getClosedAt());
        e.setLocation(i.getLocation());
        e.setPersonsInvolved(i.getPersonsInvolved());
        e.setRootCause(i.getRootCause());
        e.setCorrectiveActions(i.getCorrectiveActions());
        e.setStandardsCsv(i.getStandardsCsv());
        e.setCapaCaseId(i.getCapaCaseId());
        e.setNcId(i.getNcId());
        e.setOwnerUserId(i.getOwnerUserId());
        e.setReportedBy(i.getReportedBy());
        e.setCreatedAt(i.getCreatedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }
}
