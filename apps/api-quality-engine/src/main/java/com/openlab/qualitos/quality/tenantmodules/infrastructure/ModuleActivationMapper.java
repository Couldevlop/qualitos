package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;

final class ModuleActivationMapper {

    private ModuleActivationMapper() {}

    static ModuleActivation toDomain(ModuleActivationJpaEntity e) {
        return new ModuleActivation(
                e.getId(), e.getTenantId(), e.getModuleCode(),
                e.getStatus(), e.getBillingTier(), e.getConfigurationJson(),
                e.getTrialEndsAt(), e.getExpiresAt(),
                e.getActivatedAt(), e.getActivatedBy(),
                e.getStatusChangedAt(), e.getLastChangedBy(),
                e.getUpdatedAt());
    }

    static ModuleActivationJpaEntity toEntity(ModuleActivation a, ModuleActivationJpaEntity existing) {
        ModuleActivationJpaEntity e = existing != null ? existing : new ModuleActivationJpaEntity();
        if (a.getId() != null) e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setModuleCode(a.getModuleCode());
        e.setStatus(a.getStatus());
        e.setBillingTier(a.getBillingTier());
        e.setConfigurationJson(a.getConfigurationJson());
        e.setTrialEndsAt(a.getTrialEndsAt());
        e.setExpiresAt(a.getExpiresAt());
        e.setActivatedAt(a.getActivatedAt());
        e.setActivatedBy(a.getActivatedBy());
        e.setStatusChangedAt(a.getStatusChangedAt());
        e.setLastChangedBy(a.getLastChangedBy());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }
}
