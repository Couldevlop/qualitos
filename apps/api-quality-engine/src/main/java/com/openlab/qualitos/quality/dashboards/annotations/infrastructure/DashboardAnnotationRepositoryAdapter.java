package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DashboardAnnotationRepositoryAdapter implements DashboardAnnotationRepository {

    private final DashboardAnnotationJpaRepository jpa;

    public DashboardAnnotationRepositoryAdapter(DashboardAnnotationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DashboardAnnotation save(DashboardAnnotation annotation) {
        DashboardAnnotationJpaEntity e = DashboardAnnotationMapper.toEntity(annotation);
        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }
        DashboardAnnotationJpaEntity saved = jpa.save(e);
        DashboardAnnotation out = DashboardAnnotationMapper.toDomain(saved);
        if (annotation.getId() == null) {
            annotation.assignId(saved.getId());
        }
        return out;
    }

    @Override
    public Optional<DashboardAnnotation> findByIdAndTenant(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(DashboardAnnotationMapper::toDomain);
    }

    @Override
    public List<DashboardAnnotation> findByTenantAndChartKey(UUID tenantId, String chartKey) {
        return jpa.findByTenantIdAndChartKeyOrderByCreatedAtDesc(tenantId, chartKey).stream()
                .map(DashboardAnnotationMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
