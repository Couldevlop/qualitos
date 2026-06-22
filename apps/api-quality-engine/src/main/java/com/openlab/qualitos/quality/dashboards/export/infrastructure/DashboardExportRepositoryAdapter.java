package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExport;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DashboardExportRepositoryAdapter implements DashboardExportRepository {

    private final DashboardExportJpaRepository jpa;

    public DashboardExportRepositoryAdapter(DashboardExportJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DashboardExport save(DashboardExport export) {
        DashboardExportJpaEntity e = new DashboardExportJpaEntity();
        e.setId(export.getId() != null ? export.getId() : UUID.randomUUID());
        e.setTenantId(export.getTenantId());
        e.setUserId(export.getUserId());
        e.setDashboardId(export.getDashboardId());
        e.setDashboardName(export.getDashboardName());
        e.setVerificationCode(export.getVerificationCode());
        e.setSha256Hex(export.getSha256Hex());
        e.setSignatureEnvelope(export.getSignatureEnvelope());
        e.setAnchorTxRef(export.getAnchorTxRef());
        e.setCreatedAt(export.getCreatedAt());
        DashboardExportJpaEntity saved = jpa.save(e);
        if (export.getId() == null) {
            export.assignId(saved.getId());
        }
        return toDomain(saved);
    }

    @Override
    public Optional<DashboardExport> findByVerificationCode(String code) {
        return jpa.findByVerificationCode(code).map(DashboardExportRepositoryAdapter::toDomain);
    }

    static DashboardExport toDomain(DashboardExportJpaEntity e) {
        return new DashboardExport(
                e.getId(), e.getTenantId(), e.getUserId(), e.getDashboardId(),
                e.getDashboardName(), e.getVerificationCode(), e.getSha256Hex(),
                e.getSignatureEnvelope(), e.getAnchorTxRef(), e.getCreatedAt());
    }
}
