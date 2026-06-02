package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.risk.FmeaItemNotFoundException;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRUD des seuils de surveillance IoT (CLAUDE.md §9.7).
 *
 * <p>Le tenant est toujours résolu depuis le JWT (jamais le body, §18.2 #2).
 * La détection effective et l'ouverture de CAPA sont faites par
 * {@link TelemetryIngestionService} lors de l'ingestion.
 */
@Service
public class IotThresholdService {

    private final IotThresholdRepository repo;
    private final IotDeviceRepository deviceRepo;
    private final FmeaItemRepository fmeaItemRepo;

    public IotThresholdService(IotThresholdRepository repo, IotDeviceRepository deviceRepo,
                               FmeaItemRepository fmeaItemRepo) {
        this.repo = repo;
        this.deviceRepo = deviceRepo;
        this.fmeaItemRepo = fmeaItemRepo;
    }

    @Transactional
    public IotDto.ThresholdResponse create(IotDto.ThresholdRequest req) {
        UUID tenantId = requireTenantId();
        ensureDeviceBelongsToTenant(req.deviceId(), tenantId);
        ensureFmeaItemBelongsToTenant(req.fmeaItemId(), tenantId);
        IotThreshold t = new IotThreshold();
        t.setTenantId(tenantId);
        t.setDeviceId(req.deviceId());
        t.setMetric(req.metric());
        t.setMinValue(req.minValue());
        t.setMaxValue(req.maxValue());
        t.setCapaCriticity(req.capaCriticity());
        t.setCapaOwnerId(req.capaOwnerId());
        t.setEnabled(req.enabled() == null || req.enabled());
        t.setFmeaItemId(req.fmeaItemId());
        t.setOpenPdcaCycle(Boolean.TRUE.equals(req.openPdcaCycle()));
        return toResponse(repo.save(t));
    }

    @Transactional(readOnly = true)
    public Page<IotDto.ThresholdResponse> list(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return repo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IotDto.ThresholdResponse get(UUID id) {
        return toResponse(loadForTenant(id));
    }

    @Transactional
    public IotDto.ThresholdResponse update(UUID id, IotDto.ThresholdRequest req) {
        UUID tenantId = requireTenantId();
        IotThreshold t = loadForTenant(id);
        ensureDeviceBelongsToTenant(req.deviceId(), tenantId);
        ensureFmeaItemBelongsToTenant(req.fmeaItemId(), tenantId);
        t.setDeviceId(req.deviceId());
        t.setMetric(req.metric());
        t.setMinValue(req.minValue());
        t.setMaxValue(req.maxValue());
        t.setCapaCriticity(req.capaCriticity());
        t.setCapaOwnerId(req.capaOwnerId());
        if (req.enabled() != null) t.setEnabled(req.enabled());
        t.setFmeaItemId(req.fmeaItemId());
        t.setOpenPdcaCycle(Boolean.TRUE.equals(req.openPdcaCycle()));
        return toResponse(repo.save(t));
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(loadForTenant(id));
    }

    private IotThreshold loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        return repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotThresholdNotFoundException(id));
    }

    /** Un seuil ciblant un device doit viser un équipement du même tenant. */
    private void ensureDeviceBelongsToTenant(UUID deviceId, UUID tenantId) {
        if (deviceId == null) return;
        IotDevice d = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new IotDeviceNotFoundException(deviceId));
        if (!d.getTenantId().equals(tenantId)) throw new IotDeviceNotFoundException(deviceId);
    }

    /** Le lien FMEA, s'il est fourni, doit pointer une fiche du même tenant (§9.9). */
    private void ensureFmeaItemBelongsToTenant(UUID fmeaItemId, UUID tenantId) {
        if (fmeaItemId == null) return;
        fmeaItemRepo.findByIdAndTenantId(fmeaItemId, tenantId)
                .orElseThrow(() -> new FmeaItemNotFoundException(fmeaItemId));
    }

    private IotDto.ThresholdResponse toResponse(IotThreshold t) {
        return new IotDto.ThresholdResponse(
                t.getId(), t.getTenantId(), t.getDeviceId(), t.getMetric(),
                t.getMinValue(), t.getMaxValue(), t.getCapaCriticity(),
                t.getCapaOwnerId(), t.isEnabled(), t.getFmeaItemId(),
                t.isOpenPdcaCycle(), t.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
