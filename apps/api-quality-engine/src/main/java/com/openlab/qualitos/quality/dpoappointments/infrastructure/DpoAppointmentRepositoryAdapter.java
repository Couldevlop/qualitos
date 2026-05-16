package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.application.TenantProvider;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentRepository;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DpoAppointmentRepositoryAdapter implements DpoAppointmentRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final DpoAppointmentJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public DpoAppointmentRepositoryAdapter(
            DpoAppointmentJpaRepository jpa,
            @Qualifier("dpoTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public DpoAppointment save(DpoAppointment appointment) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(appointment.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        DpoAppointmentJpaEntity existing = appointment.getId() != null
                ? jpa.findByIdAndTenantId(appointment.getId(), currentTenant).orElse(null)
                : null;
        DpoAppointmentJpaEntity saved = jpa.save(
                DpoAppointmentMapper.toEntity(appointment, existing));
        DpoAppointment out = DpoAppointmentMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<DpoAppointment> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(DpoAppointmentMapper::toDomain);
    }

    @Override
    public List<DpoAppointment> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(DpoAppointmentMapper::toDomain).getContent();
    }

    @Override
    public List<DpoAppointment> findByTenantAndStatus(UUID tenantId, DpoAppointmentStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(DpoAppointmentMapper::toDomain).getContent();
    }

    @Override
    public Optional<DpoAppointment> findActiveByScope(UUID tenantId, String scope) {
        return jpa.findByTenantIdAndScopeAndStatus(tenantId, scope, DpoAppointmentStatus.ACTIVE)
                .map(DpoAppointmentMapper::toDomain);
    }

    @Override
    public Optional<DpoAppointment> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(DpoAppointmentMapper::toDomain);
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
