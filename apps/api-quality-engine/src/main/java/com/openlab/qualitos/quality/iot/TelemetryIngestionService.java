package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Ingestion d'événements de télémétrie (CLAUDE.md §9.7).
 *
 * Garde-fous :
 *  - Seuls les équipements ACTIVE acceptent de la télémétrie. PROVISIONED, SUSPENDED
 *    et DECOMMISSIONED renvoient un 409.
 *  - Le tenant est toujours résolu depuis le JWT, jamais le body.
 *  - Le compteur {@code telemetryCount} et le {@code lastSeenAt} du device sont
 *    mis à jour sur la même transaction — pas d'écart possible entre l'historique
 *    et le compteur affiché dans le dashboard.
 */
@Service
public class TelemetryIngestionService {

    private final IotDeviceRepository deviceRepo;
    private final IotTelemetryEventRepository eventRepo;

    public TelemetryIngestionService(IotDeviceRepository deviceRepo,
                                     IotTelemetryEventRepository eventRepo) {
        this.deviceRepo = deviceRepo;
        this.eventRepo = eventRepo;
    }

    @Transactional
    public IotDto.TelemetryResponse ingest(UUID deviceId, IotDto.TelemetryIngestRequest req) {
        UUID tenantId = requireTenantId();
        IotDevice d = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new IotDeviceNotFoundException(deviceId));
        if (!d.getTenantId().equals(tenantId)) throw new IotDeviceNotFoundException(deviceId);
        if (d.getStatus() != IotDeviceStatus.ACTIVE) {
            throw new IotDeviceStateException(
                    "Cannot ingest telemetry for device in status " + d.getStatus());
        }
        if (req.valueNumeric() == null && (req.valueText() == null || req.valueText().isBlank())) {
            throw new IotDeviceStateException("At least one of valueNumeric/valueText must be provided");
        }

        IotTelemetryEvent e = new IotTelemetryEvent();
        e.setTenantId(tenantId);
        e.setDeviceId(deviceId);
        e.setMetric(req.metric());
        e.setValueNumeric(req.valueNumeric());
        e.setValueText(req.valueText());
        e.setUnit(req.unit());
        e.setSource(req.source() != null ? req.source() : IotProtocol.MANUAL);
        e.setRecordedAt(req.recordedAt() != null ? req.recordedAt() : Instant.now());
        IotTelemetryEvent saved = eventRepo.save(e);

        d.setTelemetryCount(d.getTelemetryCount() + 1);
        d.setLastSeenAt(saved.getIngestedAt());
        deviceRepo.save(d);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IotDto.TelemetryResponse> recent(UUID deviceId, Pageable pageable) {
        UUID tenantId = requireTenantId();
        ensureSameTenant(deviceId, tenantId);
        return eventRepo.findByTenantIdAndDeviceIdOrderByRecordedAtDesc(tenantId, deviceId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<IotDto.TelemetryResponse> range(UUID deviceId, String metric,
                                                Instant from, Instant to, Pageable pageable) {
        UUID tenantId = requireTenantId();
        ensureSameTenant(deviceId, tenantId);
        return eventRepo.findByTenantIdAndDeviceIdAndMetricAndRecordedAtBetweenOrderByRecordedAtAsc(
                tenantId, deviceId, metric, from, to, pageable).map(this::toResponse);
    }

    @Transactional
    public long purgeBefore(Instant cutoff) {
        // Pas de filtre tenant : opération admin uniquement (cf. RBAC futur).
        return eventRepo.deleteByRecordedAtBefore(cutoff);
    }

    private void ensureSameTenant(UUID deviceId, UUID tenantId) {
        IotDevice d = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new IotDeviceNotFoundException(deviceId));
        if (!d.getTenantId().equals(tenantId)) throw new IotDeviceNotFoundException(deviceId);
    }

    private IotDto.TelemetryResponse toResponse(IotTelemetryEvent e) {
        return new IotDto.TelemetryResponse(
                e.getId(), e.getTenantId(), e.getDeviceId(), e.getMetric(),
                e.getValueNumeric(), e.getValueText(), e.getUnit(), e.getSource(),
                e.getRecordedAt(), e.getIngestedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
