package com.openlab.qualitos.quality.calibration;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service unique pour le module §4.10 — équipement, plan, records, MSA.
 *
 * Règles structurantes :
 *  - Le {@code nextDueOn} du plan est dérivé du dernier {@link CalibrationRecord} :
 *    {@code performedOn + frequencyMonths}, sauf override explicite sur le record.
 *  - Un équipement RETIRED ne peut plus recevoir de mutation (plan/record/MSA).
 *  - Un équipement OUT_OF_SERVICE n'accepte plus de records (mais l'admin peut
 *    encore retirer ou réactiver via update).
 *  - Le drapeau {@code critical} force un dernier record PASS pour repasser
 *    {@code OUT_OF_SERVICE → ACTIVE} : un opérateur ne peut pas remettre en
 *    service un instrument critique sans calibration concluante (§4.10).
 */
@Service
public class CalibrationService {

    private final CalibrationEquipmentRepository equipmentRepo;
    private final CalibrationPlanRepository planRepo;
    private final CalibrationRecordRepository recordRepo;
    private final MsaStudyRepository msaRepo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CalibrationService(CalibrationEquipmentRepository equipmentRepo,
                              CalibrationPlanRepository planRepo,
                              CalibrationRecordRepository recordRepo,
                              MsaStudyRepository msaRepo) {
        this(equipmentRepo, planRepo, recordRepo, msaRepo, Clock.systemUTC());
    }

    CalibrationService(CalibrationEquipmentRepository equipmentRepo,
                       CalibrationPlanRepository planRepo,
                       CalibrationRecordRepository recordRepo,
                       MsaStudyRepository msaRepo,
                       Clock clock) {
        this.equipmentRepo = equipmentRepo;
        this.planRepo = planRepo;
        this.recordRepo = recordRepo;
        this.msaRepo = msaRepo;
        this.clock = clock;
    }

    // ---------- Equipment ----------

    @Transactional
    public CalibrationDto.EquipmentResponse createEquipment(CalibrationDto.CreateEquipmentRequest req) {
        UUID tenantId = requireTenantId();
        equipmentRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(e -> {
            throw new CalibrationStateException("Equipment code already exists: " + req.code());
        });
        CalibrationEquipment e = new CalibrationEquipment();
        e.setTenantId(tenantId);
        e.setCode(req.code());
        e.setName(req.name());
        e.setManufacturer(req.manufacturer());
        e.setModel(req.model());
        e.setSerialNumber(req.serialNumber());
        e.setLocation(req.location());
        e.setCritical(req.critical());
        e.setIotDeviceId(req.iotDeviceId());
        e.setOwnerUserId(req.ownerUserId());
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setCreatedBy(req.createdBy());
        return toResponse(equipmentRepo.save(e));
    }

    @Transactional(readOnly = true)
    public Page<CalibrationDto.EquipmentResponse> listEquipment(EquipmentStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<CalibrationEquipment> page = status == null
                ? equipmentRepo.findByTenantId(tenantId, pageable)
                : equipmentRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CalibrationDto.EquipmentResponse getEquipment(UUID id) {
        return toResponse(loadForTenant(id));
    }

    @Transactional
    public CalibrationDto.EquipmentResponse updateEquipment(UUID id,
                                                            CalibrationDto.UpdateEquipmentRequest req) {
        CalibrationEquipment e = loadForTenant(id);
        if (e.getStatus() == EquipmentStatus.RETIRED) {
            throw new CalibrationStateException("Cannot edit a RETIRED equipment");
        }
        if (req.name() != null) e.setName(req.name());
        if (req.manufacturer() != null) e.setManufacturer(req.manufacturer());
        if (req.model() != null) e.setModel(req.model());
        if (req.serialNumber() != null) e.setSerialNumber(req.serialNumber());
        if (req.location() != null) e.setLocation(req.location());
        if (req.critical() != null) e.setCritical(req.critical());
        if (req.iotDeviceId() != null) e.setIotDeviceId(req.iotDeviceId());
        if (req.ownerUserId() != null) e.setOwnerUserId(req.ownerUserId());
        return toResponse(equipmentRepo.save(e));
    }

    @Transactional
    public CalibrationDto.EquipmentResponse setEquipmentStatus(UUID id, EquipmentStatus target) {
        CalibrationEquipment e = loadForTenant(id);
        if (target == null) throw new CalibrationStateException("Target status is required");
        if (e.getStatus() == EquipmentStatus.RETIRED) {
            throw new CalibrationStateException("RETIRED is terminal");
        }
        if (target == EquipmentStatus.ACTIVE
                && e.getStatus() == EquipmentStatus.OUT_OF_SERVICE
                && e.isCritical()) {
            CalibrationResult last = recordRepo.findByEquipmentIdOrderByPerformedOnDesc(
                            id, Pageable.ofSize(1))
                    .stream().findFirst().map(CalibrationRecord::getResult).orElse(null);
            if (last != CalibrationResult.PASS) {
                throw new CalibrationStateException(
                        "Critical equipment requires a PASS calibration before returning to ACTIVE");
            }
        }
        e.setStatus(target);
        return toResponse(equipmentRepo.save(e));
    }

    @Transactional
    public void deleteEquipment(UUID id) {
        CalibrationEquipment e = loadForTenant(id);
        recordRepo.deleteByEquipmentId(id);
        msaRepo.deleteByEquipmentId(id);
        planRepo.deleteByEquipmentId(id);
        equipmentRepo.delete(e);
    }

    // ---------- Plan ----------

    @Transactional
    public CalibrationDto.PlanResponse upsertPlan(UUID equipmentId,
                                                  CalibrationDto.UpsertPlanRequest req) {
        CalibrationEquipment e = loadForTenant(equipmentId);
        if (e.getStatus() == EquipmentStatus.RETIRED) {
            throw new CalibrationStateException("Cannot configure plan on a RETIRED equipment");
        }
        CalibrationPlan p = planRepo.findByEquipmentId(equipmentId).orElseGet(() -> {
            CalibrationPlan fresh = new CalibrationPlan();
            fresh.setTenantId(e.getTenantId());
            fresh.setEquipmentId(equipmentId);
            return fresh;
        });
        p.setFrequencyMonths(req.frequencyMonths());
        p.setProcedureReference(req.procedureReference());
        p.setTolerance(req.tolerance());
        p.setAccreditationRef(req.accreditationRef());
        if (p.getId() == null) {
            // Création : on prend la date initiale fournie.
            p.setNextDueOn(req.firstDueOn());
        }
        // Update : on ne touche pas à nextDueOn (les records pilotent), sauf si pas encore défini.
        if (p.getNextDueOn() == null) p.setNextDueOn(req.firstDueOn());
        return toResponse(planRepo.save(p));
    }

    @Transactional(readOnly = true)
    public Optional<CalibrationDto.PlanResponse> getPlan(UUID equipmentId) {
        loadForTenant(equipmentId);
        return planRepo.findByEquipmentId(equipmentId).map(this::toResponse);
    }

    @Transactional
    public void deletePlan(UUID equipmentId) {
        CalibrationEquipment e = loadForTenant(equipmentId);
        planRepo.findByEquipmentId(equipmentId)
                .orElseThrow(() -> new CalibrationChildNotFoundException("Plan", equipmentId));
        if (e.isCritical()) {
            throw new CalibrationStateException(
                    "Cannot delete plan of a critical equipment; archive equipment instead");
        }
        planRepo.deleteByEquipmentId(equipmentId);
    }

    @Transactional(readOnly = true)
    public Page<CalibrationDto.PlanResponse> overdue(LocalDate cutoff, Pageable pageable) {
        UUID tenantId = requireTenantId();
        LocalDate ref = cutoff != null ? cutoff : LocalDate.now(clock);
        return planRepo.findByTenantIdAndNextDueOnBefore(tenantId, ref, pageable)
                .map(this::toResponse);
    }

    // ---------- Records ----------

    @Transactional
    public CalibrationDto.RecordResponse addRecord(UUID equipmentId,
                                                   CalibrationDto.CreateRecordRequest req) {
        CalibrationEquipment e = loadForTenant(equipmentId);
        if (e.getStatus() != EquipmentStatus.ACTIVE) {
            throw new CalibrationStateException(
                    "Cannot record calibration on a " + e.getStatus() + " equipment");
        }
        CalibrationRecord r = new CalibrationRecord();
        r.setTenantId(e.getTenantId());
        r.setEquipmentId(equipmentId);
        r.setPerformedOn(req.performedOn());
        r.setPerformedByUserId(req.performedByUserId());
        r.setPerformedByOrg(req.performedByOrg());
        r.setResult(req.result());
        r.setMeasurements(req.measurements());
        r.setCertificateReference(req.certificateReference());
        r.setNextDueOverride(req.nextDueOverride());
        CalibrationRecord saved = recordRepo.save(r);

        // Recalcul du plan si présent.
        planRepo.findByEquipmentId(equipmentId).ifPresent(p -> {
            p.setLastCalibratedOn(saved.getPerformedOn());
            LocalDate computed = saved.getNextDueOverride() != null
                    ? saved.getNextDueOverride()
                    : saved.getPerformedOn().plusMonths(p.getFrequencyMonths());
            p.setNextDueOn(computed);
            planRepo.save(p);
        });

        // Calibration FAIL sur équipement critique → mise hors service automatique.
        if (saved.getResult() == CalibrationResult.FAIL && e.isCritical()) {
            e.setStatus(EquipmentStatus.OUT_OF_SERVICE);
            equipmentRepo.save(e);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CalibrationDto.RecordResponse> listRecords(UUID equipmentId, Pageable pageable) {
        loadForTenant(equipmentId);
        return recordRepo.findByEquipmentIdOrderByPerformedOnDesc(equipmentId, pageable)
                .map(this::toResponse);
    }

    // ---------- MSA ----------

    @Transactional
    public CalibrationDto.MsaResponse addMsa(UUID equipmentId,
                                             CalibrationDto.CreateMsaRequest req) {
        CalibrationEquipment e = loadForTenant(equipmentId);
        if (e.getStatus() == EquipmentStatus.RETIRED) {
            throw new CalibrationStateException("Cannot add MSA on a RETIRED equipment");
        }
        MsaStudy m = new MsaStudy();
        m.setTenantId(e.getTenantId());
        m.setEquipmentId(equipmentId);
        m.setType(req.type());
        m.setPerformedOn(req.performedOn());
        m.setStudyValue(req.studyValue());
        m.setPassingThreshold(req.passingThreshold());
        m.setResult(req.result());
        m.setNotes(req.notes());
        m.setCreatedBy(req.createdBy());
        return toResponse(msaRepo.save(m));
    }

    @Transactional(readOnly = true)
    public Page<CalibrationDto.MsaResponse> listMsa(UUID equipmentId, Pageable pageable) {
        loadForTenant(equipmentId);
        return msaRepo.findByEquipmentIdOrderByPerformedOnDesc(equipmentId, pageable)
                .map(this::toResponse);
    }

    // ---------- Summary ----------

    @Transactional(readOnly = true)
    public CalibrationDto.EquipmentSummary summary(UUID equipmentId) {
        CalibrationEquipment e = loadForTenant(equipmentId);
        Optional<CalibrationPlan> plan = planRepo.findByEquipmentId(equipmentId);
        LocalDate today = LocalDate.now(clock);
        CalibrationResult last = recordRepo.findByEquipmentIdOrderByPerformedOnDesc(
                        equipmentId, Pageable.ofSize(1))
                .stream().findFirst().map(CalibrationRecord::getResult).orElse(null);
        return new CalibrationDto.EquipmentSummary(
                equipmentId, e.getStatus(), e.isCritical(),
                plan.map(CalibrationPlan::getLastCalibratedOn).orElse(null),
                plan.map(CalibrationPlan::getNextDueOn).orElse(null),
                plan.map(p -> p.isOverdue(today)).orElse(false),
                last,
                recordRepo.countByEquipmentIdAndResult(equipmentId, CalibrationResult.PASS),
                recordRepo.countByEquipmentIdAndResult(equipmentId, CalibrationResult.FAIL),
                recordRepo.countByEquipmentIdAndResult(equipmentId, CalibrationResult.CONDITIONAL),
                msaRepo.countByEquipmentIdAndResult(equipmentId, MsaResult.PASS),
                msaRepo.countByEquipmentIdAndResult(equipmentId, MsaResult.FAIL));
    }

    // ---------- helpers ----------

    CalibrationEquipment loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        CalibrationEquipment e = equipmentRepo.findById(id)
                .orElseThrow(() -> new CalibrationEquipmentNotFoundException(id));
        if (!e.getTenantId().equals(tenantId)) throw new CalibrationEquipmentNotFoundException(id);
        return e;
    }

    private CalibrationDto.EquipmentResponse toResponse(CalibrationEquipment e) {
        return new CalibrationDto.EquipmentResponse(
                e.getId(), e.getTenantId(), e.getCode(), e.getName(),
                e.getManufacturer(), e.getModel(), e.getSerialNumber(),
                e.getLocation(), e.getStatus(), e.isCritical(),
                e.getIotDeviceId(), e.getOwnerUserId(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CalibrationDto.PlanResponse toResponse(CalibrationPlan p) {
        LocalDate today = LocalDate.now(clock);
        return new CalibrationDto.PlanResponse(
                p.getId(), p.getTenantId(), p.getEquipmentId(),
                p.getFrequencyMonths(), p.getProcedureReference(),
                p.getTolerance(), p.getAccreditationRef(),
                p.getLastCalibratedOn(), p.getNextDueOn(),
                p.isOverdue(today),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private CalibrationDto.RecordResponse toResponse(CalibrationRecord r) {
        return new CalibrationDto.RecordResponse(
                r.getId(), r.getTenantId(), r.getEquipmentId(),
                r.getPerformedOn(), r.getPerformedByUserId(), r.getPerformedByOrg(),
                r.getResult(), r.getMeasurements(),
                r.getCertificateReference(), r.getNextDueOverride(),
                r.getCreatedAt());
    }

    private CalibrationDto.MsaResponse toResponse(MsaStudy m) {
        return new CalibrationDto.MsaResponse(
                m.getId(), m.getTenantId(), m.getEquipmentId(),
                m.getType(), m.getPerformedOn(),
                m.getStudyValue(), m.getPassingThreshold(),
                m.getResult(), m.getNotes(),
                m.getCreatedBy(), m.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
