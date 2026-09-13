package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès Spring Data. La lecture métier porte TOUJOURS le tenant ; seule la
 * recherche par code de vérification ne le porte pas, parce qu'elle sert la route
 * publique où le code opaque est la seule autorité.
 */
public interface EightDReportJpaRepository extends JpaRepository<EightDReportJpaEntity, UUID> {

    Optional<EightDReportJpaEntity> findByTenantIdAndNcId(UUID tenantId, UUID ncId);

    Optional<EightDReportJpaEntity> findByVerificationCode(String verificationCode);
}
