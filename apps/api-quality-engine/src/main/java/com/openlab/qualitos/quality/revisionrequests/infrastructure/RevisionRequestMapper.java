package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Traduction explicite entre le domaine et la persistance.
 *
 * <p>Le changement proposé voyage en JSON dans une colonne texte. La conversion
 * est faite ici, à la frontière, et non par un convertisseur Hibernate : un type
 * JSON natif rendrait les tests dépendants d'un vrai PostgreSQL, et le schéma de
 * test est généré depuis les entités sur H2.
 */
final class RevisionRequestMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RevisionRequestMapper() {}

    static RevisionRequestJpaEntity toEntity(RevisionRequest r, RevisionRequestJpaEntity target) {
        RevisionRequestJpaEntity e = target != null ? target : new RevisionRequestJpaEntity();
        if (r.getId() != null) e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setProductId(r.getProductId());
        e.setTargetType(r.getTargetType().name());
        e.setTargetId(r.getTargetId());
        e.setTriggerType(r.getTriggerType().name());
        e.setTriggerRefId(r.getTriggerRefId());
        e.setTriggerRefLabel(r.getTriggerRefLabel());
        e.setRationale(r.getRationale());
        e.setProposedChange(writeChange(r.getChange()));
        e.setStatus(r.getStatus().name());
        e.setDecidedBy(r.getDecidedBy());
        e.setDecidedAt(r.getDecidedAt());
        e.setDecisionNote(r.getDecisionNote());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    static RevisionRequest toDomain(RevisionRequestJpaEntity e) {
        return RevisionRequest.rehydrate(
                e.getId(), e.getTenantId(), e.getProductId(),
                RevisionTargetType.valueOf(e.getTargetType()), e.getTargetId(),
                RevisionTriggerType.valueOf(e.getTriggerType()), e.getTriggerRefId(),
                e.getTriggerRefLabel(), e.getRationale(), readChange(e.getProposedChange()),
                RevisionRequestStatus.valueOf(e.getStatus()), e.getDecidedBy(),
                e.getDecidedAt(), e.getDecisionNote(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String writeChange(ProposedChange change) {
        try {
            return JSON.writeValueAsString(change);
        } catch (JsonProcessingException ex) {
            // Quatre chaînes : l'échec relève du bug, pas du cas limite métier.
            throw new IllegalStateException("Cannot serialize the proposed change", ex);
        }
    }

    private static ProposedChange readChange(String json) {
        try {
            return JSON.readValue(json, ProposedChange.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupted proposed change in database", ex);
        }
    }
}
