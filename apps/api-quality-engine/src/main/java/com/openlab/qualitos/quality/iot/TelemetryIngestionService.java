package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.pdca.PdcaDto;
import com.openlab.qualitos.quality.pdca.PdcaService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    /** Statuts d'une CAPA encore « active » (anti-spam des CAPA auto-générées). */
    private static final List<CapaStatus> ACTIVE_CAPA_STATUSES =
            List.of(CapaStatus.OPEN, CapaStatus.IN_PROGRESS);

    private final IotDeviceRepository deviceRepo;
    private final IotTelemetryEventRepository eventRepo;
    private final IotThresholdRepository thresholdRepo;
    private final CapaService capaService;
    private final CapaCaseRepository capaCaseRepo;
    private final PdcaService pdcaService;

    public TelemetryIngestionService(IotDeviceRepository deviceRepo,
                                     IotTelemetryEventRepository eventRepo,
                                     IotThresholdRepository thresholdRepo,
                                     CapaService capaService,
                                     CapaCaseRepository capaCaseRepo,
                                     PdcaService pdcaService) {
        this.deviceRepo = deviceRepo;
        this.eventRepo = eventRepo;
        this.thresholdRepo = thresholdRepo;
        this.capaService = capaService;
        this.capaCaseRepo = capaCaseRepo;
        this.pdcaService = pdcaService;
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

        if (saved.getValueNumeric() != null) {
            evaluateThresholds(tenantId, saved);
        }

        return toResponse(saved);
    }

    /**
     * Détection de dérive (§9.9) : si la mesure dépasse un seuil configuré, ouvre
     * automatiquement une CAPA ({@code sourceType=IOT_ALERT}) dans la même transaction.
     * Anti-spam : une seule CAPA active par (device, métrique) — la {@code sourceRef}
     * est stable, le verrou est l'existence d'une CAPA non clôturée.
     */
    private void evaluateThresholds(UUID tenantId, IotTelemetryEvent ev) {
        double value = ev.getValueNumeric().doubleValue();
        List<IotThreshold> applicable = thresholdRepo.findApplicable(tenantId, ev.getDeviceId(), ev.getMetric());
        for (IotThreshold t : applicable) {
            if (!t.isBreached(value)) continue;

            String sourceRef = "iot:device:" + ev.getDeviceId() + ":metric:" + ev.getMetric();
            if (capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                    tenantId, CapaSourceType.IOT_ALERT, sourceRef, ACTIVE_CAPA_STATUSES)) {
                return; // une CAPA active couvre déjà cette origine
            }

            String unit = ev.getUnit() != null ? ev.getUnit() : "";
            String measured = ev.getValueNumeric().toPlainString() + unit;

            StringBuilder desc = new StringBuilder()
                    .append("Seuil ").append(describeBounds(t))
                    .append(" dépassé sur l'équipement ").append(ev.getDeviceId())
                    .append(" (mesure ").append(measured).append(" à ").append(ev.getRecordedAt()).append(").");

            // §9.9 — lien vers la fiche FMEA configurée sur le seuil.
            if (t.getFmeaItemId() != null) {
                desc.append(" Lien FMEA : ").append(t.getFmeaItemId()).append('.');
            }
            // §9.9 — déclenchement d'un cycle PDCA d'amélioration (même transaction).
            if (t.isOpenPdcaCycle()) {
                PdcaDto.CycleResponse cycle = pdcaService.createCycle(new PdcaDto.CreateCycleRequest(
                        truncate("Amélioration — dérive IoT " + ev.getMetric(), 255),
                        "Cycle ouvert automatiquement suite à une dérive capteur (" + measured + ").",
                        t.getCapaOwnerId()));
                desc.append(" Cycle PDCA déclenché : ").append(cycle.id()).append('.');
            }

            capaService.createCase(new CapaDto.CreateCaseRequest(
                    truncate("Dérive IoT " + ev.getMetric() + " = " + measured, 255),
                    desc.toString(),
                    CapaType.CORRECTIVE,
                    t.getCapaCriticity(),
                    CapaSourceType.IOT_ALERT,
                    sourceRef,
                    t.getCapaOwnerId(),
                    null,
                    null));
            return; // une seule CAPA par mesure même si plusieurs seuils matchent
        }
    }

    private static String describeBounds(IotThreshold t) {
        if (t.getMinValue() != null && t.getMaxValue() != null) {
            return "[" + t.getMinValue() + " .. " + t.getMaxValue() + "]";
        }
        if (t.getMinValue() != null) return ">= " + t.getMinValue();
        return "<= " + t.getMaxValue();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
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
