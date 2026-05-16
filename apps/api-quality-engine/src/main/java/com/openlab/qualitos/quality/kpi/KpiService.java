package com.openlab.qualitos.quality.kpi;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * KPI catalog + measurements + status/trend (§6).
 *
 * Règles :
 *  - Une mesure ne peut être enregistrée que sur un KPI ACTIVE.
 *  - Updates : interdits sur ARCHIVED. La direction n'est jamais modifiable
 *    (changer la direction invaliderait l'interprétation des mesures passées).
 *  - Period: periodStart < periodEnd, sinon 409.
 *  - Lifecycle: DRAFT → ACTIVE (activate, idempotent) → DRAFT (reopen) → ARCHIVED.
 *    ARCHIVED est terminal.
 */
@Service
public class KpiService {

    private final KpiDefinitionRepository defRepo;
    private final KpiMeasurementRepository measureRepo;

    public KpiService(KpiDefinitionRepository defRepo, KpiMeasurementRepository measureRepo) {
        this.defRepo = defRepo;
        this.measureRepo = measureRepo;
    }

    // ---- Definitions ----

    @Transactional
    public KpiDto.KpiResponse create(KpiDto.CreateKpiRequest req) {
        UUID tenantId = requireTenantId();
        defRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(d -> {
            throw new KpiStateException("KPI code already exists: " + req.code());
        });
        KpiDefinition d = new KpiDefinition();
        d.setTenantId(tenantId);
        d.setCode(req.code());
        d.setName(req.name());
        d.setDescription(req.description());
        d.setCategory(req.category());
        d.setUnit(req.unit());
        d.setDirection(req.direction());
        d.setFrequency(req.frequency() == null ? KpiFrequency.MONTHLY : req.frequency());
        d.setTargetValue(req.targetValue());
        d.setThresholdWarning(req.thresholdWarning());
        d.setThresholdCritical(req.thresholdCritical());
        d.setApplicableIndustriesCsv(req.applicableIndustriesCsv());
        d.setOwnerUserId(req.ownerUserId());
        d.setCreatedBy(req.createdBy());
        d.setStatus(KpiStatus.DRAFT);
        return toResponse(defRepo.save(d));
    }

    @Transactional(readOnly = true)
    public Page<KpiDto.KpiResponse> list(KpiStatus status, String category, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<KpiDefinition> page;
        if (status != null) page = defRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (category != null) page = defRepo.findByTenantIdAndCategory(tenantId, category, pageable);
        else page = defRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public KpiDto.KpiResponse get(UUID id) { return toResponse(loadForTenant(id)); }

    @Transactional
    public KpiDto.KpiResponse update(UUID id, KpiDto.UpdateKpiRequest req) {
        KpiDefinition d = loadForTenant(id);
        if (d.getStatus() == KpiStatus.ARCHIVED) {
            throw new KpiStateException("Cannot edit an ARCHIVED KPI");
        }
        if (req.name() != null) d.setName(req.name());
        if (req.description() != null) d.setDescription(req.description());
        if (req.category() != null) d.setCategory(req.category());
        if (req.unit() != null) d.setUnit(req.unit());
        if (req.frequency() != null) d.setFrequency(req.frequency());
        if (req.targetValue() != null) d.setTargetValue(req.targetValue());
        if (req.thresholdWarning() != null) d.setThresholdWarning(req.thresholdWarning());
        if (req.thresholdCritical() != null) d.setThresholdCritical(req.thresholdCritical());
        if (req.applicableIndustriesCsv() != null) d.setApplicableIndustriesCsv(req.applicableIndustriesCsv());
        if (req.ownerUserId() != null) d.setOwnerUserId(req.ownerUserId());
        return toResponse(defRepo.save(d));
    }

    @Transactional
    public void delete(UUID id) {
        KpiDefinition d = loadForTenant(id);
        if (d.getStatus() == KpiStatus.ACTIVE) {
            throw new KpiStateException("Cannot delete an ACTIVE KPI; archive it first");
        }
        measureRepo.deleteByKpiId(id);
        defRepo.delete(d);
    }

    @Transactional
    public KpiDto.KpiResponse activate(UUID id) {
        KpiDefinition d = loadForTenant(id);
        if (d.getStatus() == KpiStatus.ARCHIVED) {
            throw new KpiStateException("Cannot reactivate an ARCHIVED KPI");
        }
        if (d.getStatus() == KpiStatus.ACTIVE) return toResponse(d); // idempotent
        d.setStatus(KpiStatus.ACTIVE);
        return toResponse(defRepo.save(d));
    }

    @Transactional
    public KpiDto.KpiResponse reopen(UUID id) {
        KpiDefinition d = loadForTenant(id);
        if (d.getStatus() != KpiStatus.ACTIVE) {
            throw new KpiStateException("Only ACTIVE KPIs can be re-opened");
        }
        d.setStatus(KpiStatus.DRAFT);
        return toResponse(defRepo.save(d));
    }

    @Transactional
    public KpiDto.KpiResponse archive(UUID id) {
        KpiDefinition d = loadForTenant(id);
        if (d.getStatus() == KpiStatus.ARCHIVED) {
            throw new KpiStateException("KPI is already ARCHIVED");
        }
        d.setStatus(KpiStatus.ARCHIVED);
        return toResponse(defRepo.save(d));
    }

    // ---- Measurements ----

    @Transactional
    public KpiDto.MeasurementResponse record(UUID kpiId, KpiDto.RecordMeasurementRequest req) {
        KpiDefinition d = loadForTenant(kpiId);
        if (d.getStatus() != KpiStatus.ACTIVE) {
            throw new KpiStateException(
                    "KPI must be ACTIVE to record measurements (current: " + d.getStatus() + ")");
        }
        if (!req.periodStart().isBefore(req.periodEnd())) {
            throw new KpiStateException("periodStart must be strictly before periodEnd");
        }
        KpiMeasurement m = measureRepo.findByKpiIdAndPeriodStart(kpiId, req.periodStart())
                .orElseGet(() -> {
                    KpiMeasurement fresh = new KpiMeasurement();
                    fresh.setTenantId(d.getTenantId());
                    fresh.setKpiId(kpiId);
                    fresh.setPeriodStart(req.periodStart());
                    return fresh;
                });
        m.setPeriodEnd(req.periodEnd());
        m.setValue(req.value());
        m.setUnit(req.unit() != null ? req.unit() : d.getUnit());
        m.setSource(req.source() != null ? req.source() : MeasurementSource.MANUAL);
        m.setRecordedByUserId(req.recordedByUserId());
        m.setNotes(req.notes());
        KpiMeasurement saved = measureRepo.save(m);
        return toResponse(saved, KpiEvaluator.evaluate(d, saved.getValue()));
    }

    @Transactional
    public void deleteMeasurement(UUID kpiId, UUID measurementId) {
        KpiDefinition d = loadForTenant(kpiId);
        if (d.getStatus() == KpiStatus.ARCHIVED) {
            throw new KpiStateException("Cannot delete measurement of an ARCHIVED KPI");
        }
        KpiMeasurement m = measureRepo.findById(measurementId)
                .orElseThrow(() -> new KpiMeasurementNotFoundException(measurementId));
        if (!m.getKpiId().equals(kpiId) || !m.getTenantId().equals(d.getTenantId())) {
            throw new KpiMeasurementNotFoundException(measurementId);
        }
        measureRepo.delete(m);
    }

    @Transactional(readOnly = true)
    public Page<KpiDto.MeasurementResponse> listMeasurements(UUID kpiId, Pageable pageable) {
        KpiDefinition d = loadForTenant(kpiId);
        return measureRepo.findByKpiIdOrderByPeriodStartDesc(kpiId, pageable)
                .map(m -> toResponse(m, KpiEvaluator.evaluate(d, m.getValue())));
    }

    // ---- Status / trend ----

    @Transactional(readOnly = true)
    public KpiDto.KpiCurrentStatus currentStatus(UUID kpiId) {
        KpiDefinition d = loadForTenant(kpiId);
        Optional<KpiMeasurement> latest = measureRepo
                .findTop24ByKpiIdOrderByPeriodStartDesc(kpiId)
                .stream().findFirst();
        KpiHealth health = latest.map(m -> KpiEvaluator.evaluate(d, m.getValue()))
                .orElse(KpiHealth.UNKNOWN);
        return new KpiDto.KpiCurrentStatus(
                d.getId(), d.getCode(), d.getName(), d.getStatus(), d.getDirection(),
                latest.map(KpiMeasurement::getValue).orElse(null),
                d.getUnit(),
                latest.map(KpiMeasurement::getPeriodStart).orElse(null),
                latest.map(KpiMeasurement::getPeriodEnd).orElse(null),
                health,
                d.getTargetValue(), d.getThresholdWarning(), d.getThresholdCritical());
    }

    @Transactional(readOnly = true)
    public KpiDto.KpiTrend trend(UUID kpiId) {
        KpiDefinition d = loadForTenant(kpiId);
        List<KpiMeasurement> recent = measureRepo.findTop24ByKpiIdOrderByPeriodStartDesc(kpiId);
        List<KpiDto.KpiTrendPoint> points = new ArrayList<>(recent.size());
        // Renvoie en ordre chronologique croissant pour la consommation directe par charts.
        for (int i = recent.size() - 1; i >= 0; i--) {
            KpiMeasurement m = recent.get(i);
            points.add(new KpiDto.KpiTrendPoint(
                    m.getPeriodStart(), m.getPeriodEnd(), m.getValue(),
                    KpiEvaluator.evaluate(d, m.getValue())));
        }
        return new KpiDto.KpiTrend(d.getId(), d.getCode(), points.size(), points);
    }

    // ---- helpers ----

    KpiDefinition loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        KpiDefinition d = defRepo.findById(id).orElseThrow(() -> new KpiNotFoundException(id));
        if (!d.getTenantId().equals(tenantId)) throw new KpiNotFoundException(id);
        return d;
    }

    private KpiDto.KpiResponse toResponse(KpiDefinition d) {
        return new KpiDto.KpiResponse(
                d.getId(), d.getTenantId(), d.getCode(), d.getName(), d.getDescription(),
                d.getCategory(), d.getUnit(),
                d.getDirection(), d.getFrequency(),
                d.getTargetValue(), d.getThresholdWarning(), d.getThresholdCritical(),
                d.getStatus(), d.getApplicableIndustriesCsv(),
                d.getOwnerUserId(), d.getCreatedBy(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    private KpiDto.MeasurementResponse toResponse(KpiMeasurement m, KpiHealth health) {
        return new KpiDto.MeasurementResponse(
                m.getId(), m.getTenantId(), m.getKpiId(),
                m.getPeriodStart(), m.getPeriodEnd(),
                m.getValue(), m.getUnit(),
                m.getSource(), m.getRecordedByUserId(),
                m.getNotes(), health, m.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
