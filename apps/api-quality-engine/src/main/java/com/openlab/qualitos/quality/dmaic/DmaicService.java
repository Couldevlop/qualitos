package com.openlab.qualitos.quality.dmaic;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DmaicService {

    private final DmaicProjectRepository projectRepository;
    private final ProcessMeasureRepository measureRepository;
    private final PokaYokeDeviceRepository deviceRepository;
    private final PokaYokeAssignmentRepository assignmentRepository;
    private final CapabilityCalculator capabilityCalculator;

    public DmaicService(DmaicProjectRepository projectRepository,
                        ProcessMeasureRepository measureRepository,
                        PokaYokeDeviceRepository deviceRepository,
                        PokaYokeAssignmentRepository assignmentRepository,
                        CapabilityCalculator capabilityCalculator) {
        this.projectRepository = projectRepository;
        this.measureRepository = measureRepository;
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.capabilityCalculator = capabilityCalculator;
    }

    // ===== Projects =====

    @Transactional(readOnly = true)
    public Page<DmaicDto.ProjectResponse> listProjects(DmaicStatus status, DmaicPhase phase, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<DmaicProject> page;
        if (status != null) {
            page = projectRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (phase != null) {
            page = projectRepository.findByTenantIdAndPhase(tenantId, phase, pageable);
        } else {
            page = projectRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toProjectResponse);
    }

    @Transactional(readOnly = true)
    public DmaicDto.ProjectResponse getProject(UUID id) {
        return toProjectResponse(loadProject(id));
    }

    public DmaicDto.ProjectResponse createProject(DmaicDto.CreateProjectRequest req) {
        UUID tenantId = requireTenantId();
        DmaicProject p = new DmaicProject();
        p.setTenantId(tenantId);
        p.setTitle(req.title());
        p.setProblemStatement(req.problemStatement());
        p.setGoalStatement(req.goalStatement());
        p.setBlackBeltId(req.blackBeltId());
        p.setChampionId(req.championId());
        p.setTargetCompletionDate(req.targetCompletionDate());
        p.setSpecLowerLimit(req.specLowerLimit());
        p.setSpecUpperLimit(req.specUpperLimit());
        p.setSpecTarget(req.specTarget());
        p.setSpecUnit(req.specUnit());
        p.setEstimatedSavingsEur(req.estimatedSavingsEur());
        p.setPhase(DmaicPhase.DEFINE);
        p.setStatus(DmaicStatus.ACTIVE);
        return toProjectResponse(projectRepository.save(p));
    }

    public DmaicDto.ProjectResponse updateProject(UUID id, DmaicDto.UpdateProjectRequest req) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() == DmaicStatus.COMPLETED || p.getStatus() == DmaicStatus.CANCELLED) {
            throw new DmaicStateException("Cannot modify a " + p.getStatus() + " project");
        }
        if (req.title() != null) p.setTitle(req.title());
        if (req.problemStatement() != null) p.setProblemStatement(req.problemStatement());
        if (req.goalStatement() != null) p.setGoalStatement(req.goalStatement());
        if (req.blackBeltId() != null) p.setBlackBeltId(req.blackBeltId());
        if (req.championId() != null) p.setChampionId(req.championId());
        if (req.targetCompletionDate() != null) p.setTargetCompletionDate(req.targetCompletionDate());
        if (req.specLowerLimit() != null) p.setSpecLowerLimit(req.specLowerLimit());
        if (req.specUpperLimit() != null) p.setSpecUpperLimit(req.specUpperLimit());
        if (req.specTarget() != null) p.setSpecTarget(req.specTarget());
        if (req.specUnit() != null) p.setSpecUnit(req.specUnit());
        if (req.estimatedSavingsEur() != null) p.setEstimatedSavingsEur(req.estimatedSavingsEur());
        return toProjectResponse(projectRepository.save(p));
    }

    public DmaicDto.ProjectResponse advancePhase(UUID id) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() != DmaicStatus.ACTIVE) {
            throw new DmaicStateException("Only ACTIVE projects can advance phase");
        }
        DmaicPhase next = p.getPhase().next();
        if (next == null) {
            // Transition CONTROL -> COMPLETED
            p.setStatus(DmaicStatus.COMPLETED);
            p.setCompletedAt(Instant.now());
        } else {
            p.setPhase(next);
        }
        return toProjectResponse(projectRepository.save(p));
    }

    public DmaicDto.ProjectResponse hold(UUID id) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() != DmaicStatus.ACTIVE) {
            throw new DmaicStateException("Only ACTIVE projects can be put on hold");
        }
        p.setStatus(DmaicStatus.ON_HOLD);
        return toProjectResponse(projectRepository.save(p));
    }

    public DmaicDto.ProjectResponse resume(UUID id) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() != DmaicStatus.ON_HOLD) {
            throw new DmaicStateException("Only ON_HOLD projects can be resumed");
        }
        p.setStatus(DmaicStatus.ACTIVE);
        return toProjectResponse(projectRepository.save(p));
    }

    public DmaicDto.ProjectResponse cancel(UUID id) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() == DmaicStatus.COMPLETED) {
            throw new DmaicStateException("Completed project cannot be cancelled");
        }
        if (p.getStatus() == DmaicStatus.CANCELLED) {
            throw new DmaicStateException("Project already cancelled");
        }
        p.setStatus(DmaicStatus.CANCELLED);
        return toProjectResponse(projectRepository.save(p));
    }

    public void deleteProject(UUID id) {
        DmaicProject p = loadProject(id);
        if (p.getStatus() == DmaicStatus.COMPLETED) {
            throw new DmaicStateException("Completed project cannot be deleted");
        }
        projectRepository.delete(p);
    }

    // ===== Measures =====

    public DmaicDto.MeasureResponse addMeasure(UUID projectId, DmaicDto.AddMeasureRequest req) {
        DmaicProject p = loadProject(projectId);
        if (p.getStatus() != DmaicStatus.ACTIVE) {
            throw new DmaicStateException("Measures only on ACTIVE projects");
        }
        ProcessMeasure m = new ProcessMeasure();
        m.setProject(p);
        m.setValue(req.value());
        m.setSubgroupId(req.subgroupId());
        m.setSourceRef(req.sourceRef());
        m.setRecordedAt(req.recordedAt() != null ? req.recordedAt() : Instant.now());
        m.setOperatorId(req.operatorId());
        m.setNote(req.note());
        return toMeasureResponse(measureRepository.save(m));
    }

    public void deleteMeasure(UUID projectId, UUID measureId) {
        loadProject(projectId);
        ProcessMeasure m = measureRepository.findByIdAndProjectId(measureId, projectId)
                .orElseThrow(() -> new ProcessMeasureNotFoundException(measureId));
        measureRepository.delete(m);
    }

    @Transactional(readOnly = true)
    public DmaicDto.CapabilityResponse computeCapability(UUID projectId) {
        DmaicProject p = loadProject(projectId);
        List<Double> values = measureRepository.findByProjectIdOrderByRecordedAtAsc(projectId).stream()
                .map(ProcessMeasure::getValue).toList();
        return capabilityCalculator.compute(values,
                p.getSpecLowerLimit(), p.getSpecUpperLimit(), p.getSpecTarget());
    }

    // ===== Poka-Yoke catalog =====

    @Transactional(readOnly = true)
    public Page<DmaicDto.DeviceSummary> listDevices(PokaYokeType type, PokaYokeMechanism mechanism,
                                                    Pageable pageable) {
        Page<PokaYokeDevice> page;
        if (type != null) {
            page = deviceRepository.findByType(type, pageable);
        } else if (mechanism != null) {
            page = deviceRepository.findByMechanism(mechanism, pageable);
        } else {
            page = deviceRepository.findAll(pageable);
        }
        return page.map(d -> new DmaicDto.DeviceSummary(
                d.getId(), d.getCode(), d.getName(), d.getType(), d.getMechanism(),
                d.getApplicableIndustries(), d.getImplementationCost()));
    }

    @Transactional(readOnly = true)
    public DmaicDto.DeviceDetail getDevice(UUID id) {
        PokaYokeDevice d = deviceRepository.findById(id)
                .orElseThrow(() -> new PokaYokeDeviceNotFoundException(id));
        return toDeviceDetail(d);
    }

    @Transactional(readOnly = true)
    public DmaicDto.DeviceDetail getDeviceByCode(String code) {
        PokaYokeDevice d = deviceRepository.findByCode(code)
                .orElseThrow(() -> new PokaYokeDeviceNotFoundException(code));
        return toDeviceDetail(d);
    }

    // ===== Assignments =====

    public DmaicDto.AssignmentResponse assignDevice(UUID projectId,
                                                    DmaicDto.AssignPokaYokeRequest req) {
        UUID tenantId = requireTenantId();
        DmaicProject p = loadProject(projectId);
        if (p.getStatus() != DmaicStatus.ACTIVE) {
            throw new DmaicStateException("Poka-Yoke assignment only on ACTIVE projects");
        }
        PokaYokeDevice d = deviceRepository.findById(req.deviceId())
                .orElseThrow(() -> new PokaYokeDeviceNotFoundException(req.deviceId()));

        PokaYokeAssignment a = new PokaYokeAssignment();
        a.setTenantId(tenantId);
        a.setProject(p);
        a.setDevice(d);
        a.setNote(req.note());
        a.setStatus(PokaYokeAssignmentStatus.PROPOSED);
        return toAssignmentResponse(assignmentRepository.save(a));
    }

    public DmaicDto.AssignmentResponse updateAssignment(UUID projectId, UUID assignmentId,
                                                       DmaicDto.UpdateAssignmentRequest req) {
        loadProject(projectId);
        PokaYokeAssignment a = assignmentRepository.findByIdAndProjectId(assignmentId, projectId)
                .orElseThrow(() -> new PokaYokeAssignmentNotFoundException(assignmentId));
        if (req.note() != null) a.setNote(req.note());
        if (req.defectReductionPct() != null) a.setDefectReductionPct(req.defectReductionPct());
        if (req.status() != null) {
            validateAssignmentTransition(a.getStatus(), req.status());
            a.setStatus(req.status());
            switch (req.status()) {
                case IMPLEMENTED -> a.setImplementedAt(Instant.now());
                case VERIFIED -> a.setVerifiedAt(Instant.now());
                default -> {}
            }
        }
        return toAssignmentResponse(assignmentRepository.save(a));
    }

    public void deleteAssignment(UUID projectId, UUID assignmentId) {
        loadProject(projectId);
        PokaYokeAssignment a = assignmentRepository.findByIdAndProjectId(assignmentId, projectId)
                .orElseThrow(() -> new PokaYokeAssignmentNotFoundException(assignmentId));
        if (a.getStatus() == PokaYokeAssignmentStatus.VERIFIED) {
            throw new DmaicStateException("Verified assignment cannot be deleted (audit trail)");
        }
        assignmentRepository.delete(a);
    }

    static void validateAssignmentTransition(PokaYokeAssignmentStatus current,
                                             PokaYokeAssignmentStatus next) {
        boolean ok = switch (current) {
            case PROPOSED -> next == PokaYokeAssignmentStatus.IN_DESIGN
                          || next == PokaYokeAssignmentStatus.ABANDONED;
            case IN_DESIGN -> next == PokaYokeAssignmentStatus.IMPLEMENTED
                           || next == PokaYokeAssignmentStatus.ABANDONED;
            case IMPLEMENTED -> next == PokaYokeAssignmentStatus.VERIFIED
                             || next == PokaYokeAssignmentStatus.ABANDONED;
            case VERIFIED, ABANDONED -> false;
        };
        if (!ok) {
            throw new DmaicStateException("Invalid transition " + current + " -> " + next);
        }
    }

    // ===== helpers =====

    private DmaicProject loadProject(UUID id) {
        UUID tenantId = requireTenantId();
        return projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DmaicProjectNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private DmaicDto.ProjectResponse toProjectResponse(DmaicProject p) {
        return new DmaicDto.ProjectResponse(
                p.getId(), p.getTenantId(), p.getTitle(),
                p.getProblemStatement(), p.getGoalStatement(),
                p.getPhase(), p.getStatus(),
                p.getChampionId(), p.getBlackBeltId(),
                p.getTargetCompletionDate(),
                p.getSpecLowerLimit(), p.getSpecUpperLimit(), p.getSpecTarget(), p.getSpecUnit(),
                p.getEstimatedSavingsEur(),
                p.getMeasures().size(),
                p.getPokaYokeAssignments().size(),
                p.getStartedAt(), p.getCompletedAt(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private DmaicDto.MeasureResponse toMeasureResponse(ProcessMeasure m) {
        return new DmaicDto.MeasureResponse(
                m.getId(), m.getProject().getId(), m.getValue(),
                m.getSubgroupId(), m.getSourceRef(),
                m.getRecordedAt(), m.getOperatorId(),
                m.getNote(), m.getCreatedAt());
    }

    private DmaicDto.DeviceDetail toDeviceDetail(PokaYokeDevice d) {
        return new DmaicDto.DeviceDetail(
                d.getId(), d.getCode(), d.getName(), d.getDescription(),
                d.getType(), d.getMechanism(),
                d.getApplicableIndustries(), d.getExamples(),
                d.getImplementationCost(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    private DmaicDto.AssignmentResponse toAssignmentResponse(PokaYokeAssignment a) {
        return new DmaicDto.AssignmentResponse(
                a.getId(), a.getProject().getId(),
                a.getDevice().getId(), a.getDevice().getCode(), a.getDevice().getName(),
                a.getDevice().getType(),
                a.getStatus(), a.getNote(),
                a.getImplementedAt(), a.getVerifiedAt(), a.getDefectReductionPct(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
