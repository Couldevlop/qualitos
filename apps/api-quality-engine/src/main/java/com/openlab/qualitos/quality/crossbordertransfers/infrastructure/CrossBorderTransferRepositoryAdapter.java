package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.application.TenantProvider;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferRepository;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CrossBorderTransferRepositoryAdapter implements CrossBorderTransferRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final CrossBorderTransferJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public CrossBorderTransferRepositoryAdapter(
            CrossBorderTransferJpaRepository jpa,
            @Qualifier("cbtTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public CrossBorderTransfer save(CrossBorderTransfer transfer) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(transfer.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        CrossBorderTransferJpaEntity existing = transfer.getId() != null
                ? jpa.findByIdAndTenantId(transfer.getId(), currentTenant).orElse(null)
                : null;
        CrossBorderTransferJpaEntity saved = jpa.save(
                CrossBorderTransferMapper.toEntity(transfer, existing));
        CrossBorderTransfer out = CrossBorderTransferMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<CrossBorderTransfer> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(CrossBorderTransferMapper::toDomain);
    }

    @Override
    public List<CrossBorderTransfer> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(CrossBorderTransferMapper::toDomain).getContent();
    }

    @Override
    public List<CrossBorderTransfer> findByTenantAndStatus(UUID tenantId,
                                                           CrossBorderTransferStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(CrossBorderTransferMapper::toDomain).getContent();
    }

    @Override
    public Optional<CrossBorderTransfer> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(CrossBorderTransferMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
