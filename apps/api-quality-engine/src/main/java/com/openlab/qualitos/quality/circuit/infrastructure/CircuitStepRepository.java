package com.openlab.qualitos.quality.circuit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Les circuits réglés. Toutes les requêtes portent le client. */
public interface CircuitStepRepository extends JpaRepository<CircuitStepJpaEntity, CircuitStepJpaEntity.Key> {

    List<CircuitStepJpaEntity> findByTenantIdAndSubjectOrderByOrdinalAsc(UUID tenantId, String subject);

    @Modifying
    @Query("delete from CircuitStepJpaEntity s where s.tenantId = :tenantId and s.subject = :subject")
    void deleteCircuit(@Param("tenantId") UUID tenantId, @Param("subject") String subject);
}
