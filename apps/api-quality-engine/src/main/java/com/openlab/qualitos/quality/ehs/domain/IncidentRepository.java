package com.openlab.qualitos.quality.ehs.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — contrat de persistance défini par le domaine. L'application dépend de
 * cette interface, pas de Spring Data. L'adapter JPA vit en infrastructure.
 */
public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(UUID id);

    Optional<Incident> findByTenantIdAndCode(UUID tenantId, String code);

    /** Liste paginée logique ; l'adapter mappe vers Spring Data Pageable. */
    PagedResult<Incident> list(UUID tenantId, IncidentFilter filter, int page, int size);

    long countByTenantIdAndStatus(UUID tenantId, IncidentStatus status);

    long countByTenantIdAndType(UUID tenantId, IncidentType type);

    void delete(Incident incident);

    /**
     * Filtre composable utilisé par {@link #list}. Toute combinaison est valide ;
     * les implémentations choisissent l'index le plus adapté.
     */
    record IncidentFilter(IncidentStatus status, IncidentType type, IncidentSeverity severity) {
        public static IncidentFilter empty() { return new IncidentFilter(null, null, null); }
    }

    record PagedResult<T>(List<T> content, long totalElements, int page, int size) {
        public boolean isEmpty() { return content.isEmpty(); }
    }
}
