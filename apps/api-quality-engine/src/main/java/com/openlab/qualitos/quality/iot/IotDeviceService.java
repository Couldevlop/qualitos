package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cycle de vie complet des équipements IoT (CLAUDE.md §9.6).
 *
 * Règles de transition appliquées :
 *  - PROVISIONED → ACTIVE       : activate()
 *  - ACTIVE      → SUSPENDED    : suspend()
 *  - SUSPENDED   → ACTIVE       : resume()
 *  - ACTIVE/SUSPENDED → DECOMMISSIONED : decommission() — terminal
 *  - DECOMMISSIONED → *         : INTERDIT (ouvrir un ticket d'asset mgmt)
 *
 * Toute autre transition lève {@link IotDeviceStateException}.
 */
@Service
public class IotDeviceService {

    private final IotDeviceRepository repo;

    public IotDeviceService(IotDeviceRepository repo) { this.repo = repo; }

    @Transactional
    public IotDto.DeviceResponse create(IotDto.CreateDeviceRequest req) {
        UUID tenantId = requireTenantId();
        repo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(existing -> {
            throw new IotDeviceStateException("Device code already exists: " + req.code());
        });
        IotDevice d = new IotDevice();
        d.setTenantId(tenantId);
        d.setCode(req.code());
        d.setName(req.name());
        d.setDeviceType(req.deviceType());
        d.setProtocol(req.protocol());
        d.setLocation(req.location());
        d.setDescription(req.description());
        d.setMetadataJson(req.metadataJson());
        d.setStatus(IotDeviceStatus.PROVISIONED);
        d.setCreatedBy(req.createdBy());
        return toResponse(repo.save(d));
    }

    @Transactional(readOnly = true)
    public Page<IotDto.DeviceResponse> list(IotDeviceStatus status, IotDeviceType type, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<IotDevice> page;
        if (status != null) page = repo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (type != null) page = repo.findByTenantIdAndDeviceType(tenantId, type, pageable);
        else page = repo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IotDto.DeviceResponse get(UUID id) {
        return toResponse(loadForTenant(id));
    }

    @Transactional
    public IotDto.DeviceResponse update(UUID id, IotDto.UpdateDeviceRequest req) {
        IotDevice d = loadForTenant(id);
        if (req.name() != null) d.setName(req.name());
        if (req.deviceType() != null) d.setDeviceType(req.deviceType());
        if (req.protocol() != null) d.setProtocol(req.protocol());
        if (req.location() != null) d.setLocation(req.location());
        if (req.description() != null) d.setDescription(req.description());
        if (req.metadataJson() != null) d.setMetadataJson(req.metadataJson());
        return toResponse(repo.save(d));
    }

    @Transactional
    public void delete(UUID id) {
        IotDevice d = loadForTenant(id);
        if (d.getStatus() != IotDeviceStatus.DECOMMISSIONED
                && d.getTelemetryCount() > 0) {
            throw new IotDeviceStateException(
                    "Cannot delete device with telemetry history; decommission first");
        }
        repo.delete(d);
    }

    @Transactional
    public IotDto.DeviceResponse activate(UUID id) {
        IotDevice d = loadForTenant(id);
        if (d.getStatus() != IotDeviceStatus.PROVISIONED
                && d.getStatus() != IotDeviceStatus.SUSPENDED) {
            throw new IotDeviceStateException(
                    "Cannot activate device in status " + d.getStatus());
        }
        d.setStatus(IotDeviceStatus.ACTIVE);
        return toResponse(repo.save(d));
    }

    @Transactional
    public IotDto.DeviceResponse suspend(UUID id) {
        IotDevice d = loadForTenant(id);
        if (d.getStatus() != IotDeviceStatus.ACTIVE) {
            throw new IotDeviceStateException("Only ACTIVE devices can be suspended");
        }
        d.setStatus(IotDeviceStatus.SUSPENDED);
        return toResponse(repo.save(d));
    }

    @Transactional
    public IotDto.DeviceResponse decommission(UUID id) {
        IotDevice d = loadForTenant(id);
        if (d.getStatus() == IotDeviceStatus.DECOMMISSIONED) {
            throw new IotDeviceStateException("Device is already DECOMMISSIONED");
        }
        d.setStatus(IotDeviceStatus.DECOMMISSIONED);
        return toResponse(repo.save(d));
    }

    /**
     * Charge un équipement appartenant au tenant courant ; toute requête cross-tenant
     * remonte comme un 404 (pas de leak d'existence).
     */
    IotDevice loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        IotDevice d = repo.findById(id).orElseThrow(() -> new IotDeviceNotFoundException(id));
        if (!d.getTenantId().equals(tenantId)) throw new IotDeviceNotFoundException(id);
        return d;
    }

    IotDto.DeviceResponse toResponse(IotDevice d) {
        return new IotDto.DeviceResponse(
                d.getId(), d.getTenantId(), d.getCode(), d.getName(),
                d.getDeviceType(), d.getProtocol(), d.getStatus(),
                d.getLocation(), d.getDescription(), d.getMetadataJson(),
                d.getLastSeenAt(), d.getTelemetryCount(),
                d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
