package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;

final class AiIncidentMapper {
    private AiIncidentMapper() {}

    static AiIncidentJpaEntity toEntity(AiIncident i, AiIncidentJpaEntity target) {
        AiIncidentJpaEntity e = target != null ? target : new AiIncidentJpaEntity();
        if (i.getId() != null) e.setId(i.getId());
        e.setTenantId(i.getTenantId());
        e.setReference(i.getReference());
        e.setAiSystemId(i.getAiSystemId());
        e.setSeverity(i.getSeverity());
        e.setDescription(i.getDescription());
        e.setAffectedPersonsDescription(i.getAffectedPersonsDescription());
        e.setImmediateActionsTaken(i.getImmediateActionsTaken());
        e.setOccurredAt(i.getOccurredAt());
        e.setDetectedAt(i.getDetectedAt());
        e.setStatus(i.getStatus());
        e.setInvestigationStartedAt(i.getInvestigationStartedAt());
        e.setInvestigationLeadUserId(i.getInvestigationLeadUserId());
        e.setRootCauseAnalysis(i.getRootCauseAnalysis());
        e.setCorrectiveActions(i.getCorrectiveActions());
        e.setNotifiedRegulatorAt(i.getNotifiedRegulatorAt());
        e.setRegulatorReference(i.getRegulatorReference());
        e.setClosedAt(i.getClosedAt());
        e.setDismissedAt(i.getDismissedAt());
        e.setDismissalReason(i.getDismissalReason());
        e.setCreatedByUserId(i.getCreatedByUserId());
        e.setCreatedAt(i.getCreatedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }

    static AiIncident toDomain(AiIncidentJpaEntity e) {
        return new AiIncident(
                e.getId(), e.getTenantId(), e.getReference(), e.getAiSystemId(),
                e.getSeverity(), e.getDescription(),
                e.getAffectedPersonsDescription(), e.getImmediateActionsTaken(),
                e.getOccurredAt(), e.getDetectedAt(),
                e.getStatus(),
                e.getInvestigationStartedAt(), e.getInvestigationLeadUserId(),
                e.getRootCauseAnalysis(), e.getCorrectiveActions(),
                e.getNotifiedRegulatorAt(), e.getRegulatorReference(),
                e.getClosedAt(), e.getDismissedAt(), e.getDismissalReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
