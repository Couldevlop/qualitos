package com.openlab.qualitos.quality.dashboards.infrastructure;

import com.openlab.qualitos.quality.dashboards.domain.DashboardLayout;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DashboardLayoutRepositoryAdapter implements DashboardLayoutRepository {

    private final DashboardLayoutJpaRepository jpa;

    public DashboardLayoutRepositoryAdapter(DashboardLayoutJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DashboardLayout save(DashboardLayout layout) {
        DashboardLayoutJpaEntity e = DashboardLayoutMapper.toEntity(layout);
        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }
        DashboardLayoutJpaEntity saved = jpa.save(e);
        DashboardLayout out = DashboardLayoutMapper.toDomain(saved);
        if (layout.getId() == null) {
            layout.assignId(saved.getId());
        }
        return out;
    }

    @Override
    public Optional<DashboardLayout> findById(UUID id) {
        return jpa.findById(id).map(DashboardLayoutMapper::toDomain);
    }

    @Override
    public List<DashboardLayout> findVisibleForUser(UUID tenantId, UUID userId) {
        return jpa.findVisibleForUser(tenantId, userId).stream()
                .map(DashboardLayoutMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantUserName(UUID tenantId, UUID userId, String name) {
        return jpa.existsByTenantIdAndUserIdAndName(tenantId, userId, name);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
