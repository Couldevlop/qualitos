package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DashboardExportJpaRepository extends JpaRepository<DashboardExportJpaEntity, UUID> {

    Optional<DashboardExportJpaEntity> findByVerificationCode(String verificationCode);
}
