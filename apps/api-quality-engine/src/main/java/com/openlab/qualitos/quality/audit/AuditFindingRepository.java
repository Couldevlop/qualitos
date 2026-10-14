package com.openlab.qualitos.quality.audit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, UUID> {

    Optional<AuditFinding> findByIdAndPlanId(UUID id, UUID planId);

    /** Le constat sous verrou d'écriture : deux ouvertures de CAPA simultanées ne créent pas deux dossiers. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditFinding> findForUpdateByIdAndPlanId(UUID id, UUID planId);
}
