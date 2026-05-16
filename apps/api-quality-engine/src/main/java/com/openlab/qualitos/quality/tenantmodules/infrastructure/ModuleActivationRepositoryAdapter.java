package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ModuleActivationRepositoryAdapter implements ModuleActivationRepository {

    private final ModuleActivationJpaRepository jpa;

    public ModuleActivationRepositoryAdapter(ModuleActivationJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional
    public ModuleActivation save(ModuleActivation activation) {
        ModuleActivationJpaEntity existing = activation.getId() != null
                ? jpa.findById(activation.getId()).orElse(null)
                : null;
        ModuleActivationJpaEntity saved = jpa.save(ModuleActivationMapper.toEntity(activation, existing));
        if (activation.getId() == null) activation.assignId(saved.getId());
        return ModuleActivationMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModuleActivation> findById(UUID id) {
        return jpa.findById(id).map(ModuleActivationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModuleActivation> findOpenByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.findOpen(tenantId, code).map(ModuleActivationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleActivation> findAllByTenantId(UUID tenantId) {
        return jpa.findByTenantIdOrderByActivatedAtDesc(tenantId)
                .stream().map(ModuleActivationMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleActivation> findEnabledByTenantId(UUID tenantId) {
        return jpa.findEnabled(tenantId).stream().map(ModuleActivationMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleActivation> findDueForExpiration(Instant now, int limit) {
        return jpa.findDueForExpiration(now, PageRequest.of(0, limit))
                .stream().map(ModuleActivationMapper::toDomain).toList();
    }
}
