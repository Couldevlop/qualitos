package com.openlab.qualitos.quality.circuit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Les passages. Toutes les requêtes portent le client. */
public interface CircuitRunRepository extends JpaRepository<CircuitRunJpaEntity, UUID> {

    Optional<CircuitRunJpaEntity> findByTenantIdAndSubjectAndSubjectIdAndStatus(UUID tenantId, String subject,
                                                                               UUID subjectId, String status);

    Optional<CircuitRunJpaEntity> findFirstByTenantIdAndSubjectAndSubjectIdOrderByStartedAtDesc(
            UUID tenantId, String subject, UUID subjectId);
}
