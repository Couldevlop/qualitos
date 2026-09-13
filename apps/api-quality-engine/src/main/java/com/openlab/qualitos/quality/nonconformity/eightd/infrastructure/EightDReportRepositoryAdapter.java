package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReport;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Implémente le port de persistance du domaine au-dessus de Spring Data. */
@Component
public class EightDReportRepositoryAdapter implements EightDReportRepository {

    private final EightDReportJpaRepository jpa;

    public EightDReportRepositoryAdapter(EightDReportJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EightDReport save(EightDReport report) {
        // Mise à jour en place quand la ligne existe : recréer une entité détachée
        // avec le même identifiant ferait un second INSERT sur la contrainte d'unicité.
        EightDReportJpaEntity existante = report.getId() == null
                ? null
                : jpa.findById(report.getId()).orElse(null);
        EightDReportJpaEntity saved = jpa.save(EightDReportMapper.toEntity(report, existante));
        report.assignId(saved.getId());
        return EightDReportMapper.toDomain(saved);
    }

    @Override
    public Optional<EightDReport> findByNc(UUID tenantId, UUID ncId) {
        return jpa.findByTenantIdAndNcId(tenantId, ncId).map(EightDReportMapper::toDomain);
    }

    @Override
    public Optional<EightDReport> findByVerificationCode(String verificationCode) {
        return jpa.findByVerificationCode(verificationCode).map(EightDReportMapper::toDomain);
    }
}
