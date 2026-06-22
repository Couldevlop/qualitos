package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.AcademyCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademyCertificateRepository extends JpaRepository<AcademyCertificate, UUID> {

    /** Recherche publique par code (vérification QR — pas de filtre tenant). */
    Optional<AcademyCertificate> findByCode(String code);

    Optional<AcademyCertificate> findByTenantIdAndEnrollmentId(UUID tenantId, UUID enrollmentId);
}
