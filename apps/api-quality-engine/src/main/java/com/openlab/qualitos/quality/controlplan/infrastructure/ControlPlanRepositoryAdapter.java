package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.controlplan.application.TenantProvider;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur de persistance. Toute lecture par identifiant filtre sur le tenant
 * courant : la ceinture applicative ne suffit pas si l'adaptateur, lui, accepte
 * de charger n'importe quelle ligne de la table.
 */
@Component
public class ControlPlanRepositoryAdapter implements ControlPlanRepository {

    private final ControlPlanJpaRepository jpa;
    private final ControlPlanLineJpaRepository jpaLine;
    private final TenantProvider tenants;

    public ControlPlanRepositoryAdapter(ControlPlanJpaRepository jpa,
                                        ControlPlanLineJpaRepository jpaLine,
                                        @Qualifier("controlPlanTenantContextProvider") TenantProvider tenants) {
        this.jpa = jpa;
        this.jpaLine = jpaLine;
        this.tenants = tenants;
    }

    @Override
    public ControlPlan save(ControlPlan plan) {
        UUID tenant = tenants.requireTenantId();
        if (!tenant.equals(plan.getTenantId())) {
            throw new IllegalStateException("Cross-tenant control plan save attempt");
        }
        ControlPlanJpaEntity existing = plan.getId() != null
                ? jpa.findByIdAndTenantId(plan.getId(), tenant).orElse(null)
                : null;
        ControlPlanJpaEntity saved = jpa.save(ControlPlanMapper.toEntity(plan, existing));
        plan.assignId(saved.getId());
        return ControlPlanMapper.toDomain(saved);
    }

    @Override
    public Optional<ControlPlan> findById(UUID id) {
        return jpa.findByIdAndTenantId(id, tenants.requireTenantId()).map(ControlPlanMapper::toDomain);
    }

    @Override
    public List<ControlPlan> findByProduct(UUID tenantId, UUID productId) {
        return jpa.findByTenantIdAndProductIdOrderByPhaseAscRevisionDesc(tenantId, productId).stream()
                .map(ControlPlanMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ControlPlan> findActive(UUID tenantId, UUID productId, ControlPlanPhase phase) {
        return jpa.findByTenantIdAndProductIdAndPhaseAndStatus(
                        tenantId, productId, phase.name(), ControlPlanStatus.ACTIVE.name())
                .map(ControlPlanMapper::toDomain);
    }

    @Override
    public Optional<ControlPlan> findDraft(UUID tenantId, UUID productId, ControlPlanPhase phase) {
        return jpa.findByTenantIdAndProductIdAndPhaseAndStatus(
                        tenantId, productId, phase.name(), ControlPlanStatus.DRAFT.name())
                .map(ControlPlanMapper::toDomain);
    }

    @Override
    public ControlPlanLine saveLine(ControlPlanLine line) {
        UUID tenant = tenants.requireTenantId();
        if (!tenant.equals(line.getTenantId())) {
            throw new IllegalStateException("Cross-tenant control plan line save attempt");
        }
        ControlPlanLineJpaEntity existing = line.getId() != null
                ? jpaLine.findByIdAndTenantId(line.getId(), tenant).orElse(null)
                : null;
        ControlPlanLineJpaEntity saved = jpaLine.save(ControlPlanMapper.toEntity(line, existing));
        line.assignId(saved.getId());
        return ControlPlanMapper.toDomain(saved);
    }

    @Override
    public List<ControlPlanLine> linesOf(UUID planId) {
        return jpaLine.findByPlanIdAndTenantIdOrderBySequenceNoAsc(planId, tenants.requireTenantId())
                .stream()
                .map(ControlPlanMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ControlPlanLine> findLine(UUID id) {
        return jpaLine.findByIdAndTenantId(id, tenants.requireTenantId())
                .map(ControlPlanMapper::toDomain);
    }

    @Override
    public void deleteLine(UUID id) {
        jpaLine.findByIdAndTenantId(id, tenants.requireTenantId()).ifPresent(jpaLine::delete);
    }
}
