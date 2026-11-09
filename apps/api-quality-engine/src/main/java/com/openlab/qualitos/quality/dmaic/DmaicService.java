package com.openlab.qualitos.quality.dmaic;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DmaicService {

    private final DmaicProjectRepository projectRepository;
    private final ProcessMeasureRepository measureRepository;
    private final PokaYokeDeviceRepository deviceRepository;
    private final PokaYokeAssignmentRepository assignmentRepository;
    private final CapabilityCalculator capabilityCalculator;
    /** La fiche de résolution, qui dit ce qui manque à une phase ; absente : pas de verrou. */
    private DmaicPhaseGate phaseGate;

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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPhaseGate(DmaicPhaseGate phaseGate) {
        this.phaseGate = phaseGate;
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
        // Une phase dont le questionnaire n'est pas rempli bloque la suivante.
        List<String> manque = phaseGate == null ? List.of() : phaseGate.missing(id, p.getPhase());
        if (!manque.isEmpty()) {
            throw new DmaicGateException("La phase n'est pas complète : " + manque.size()
                    + " réponse(s) manquante(s).", manque);
        }
        DmaicPhase next = p.getPhase().next();
        if (next == null) {
            // Transition CONTROL -> COMPLETED. Clore, ce n'est pas avancer d'un
            // cran de plus : c'est déclarer le problème résolu. Il y faut toute la
            // fiche renseignée ET toutes les actions à 100 % — un projet clos sur
            // un plan d'actions à 60 % affirme une résolution que rien ne soutient.
            List<String> empeche = phaseGate == null ? List.of() : phaseGate.blockingCompletion(id);
            if (!empeche.isEmpty()) {
                // La LISTE voyage, pas seulement son premier élément : un projet
                // bloqué par cinq points se soldait sinon cinq fois, à l'aveugle.
                throw new DmaicGateException("Le projet ne peut pas être clos : " + empeche.size()
                        + " point(s) à solder.", empeche);
            }
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

    @Transactional(readOnly = true)
    public List<DmaicDto.MeasureResponse> listMeasures(UUID projectId) {
        loadProject(projectId);
        return measureRepository.findByProjectIdOrderByRecordedAtAsc(projectId).stream()
                .map(this::toMeasureResponse).toList();
    }

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
                .map(ProcessMeasure::getValue).filter(Objects::nonNull).toList();
        return capabilityCalculator.compute(values,
                p.getSpecLowerLimit(), p.getSpecUpperLimit(), p.getSpecTarget());
    }

    // ===== Poka-Yoke catalog =====

    @Transactional(readOnly = true)
    public Page<DmaicDto.DeviceSummary> listDevices(PokaYokeType type, PokaYokeMechanism mechanism,
                                                    Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<PokaYokeDevice> page;
        if (type != null) {
            page = deviceRepository.findVisibleByType(tenantId, type, pageable);
        } else if (mechanism != null) {
            page = deviceRepository.findVisibleByMechanism(tenantId, mechanism, pageable);
        } else {
            page = deviceRepository.findVisible(tenantId, pageable);
        }
        return page.map(d -> new DmaicDto.DeviceSummary(
                d.getId(), d.getCode(), d.getName(), d.getType(), d.getMechanism(),
                d.getApplicableIndustries(), d.getImplementationCost(),
                d.getTenantId() != null, d.getSourceReference()));
    }

    @Transactional(readOnly = true)
    public DmaicDto.DeviceDetail getDevice(UUID id) {
        PokaYokeDevice d = deviceRepository.findVisibleById(id, requireTenantId())
                .orElseThrow(() -> new PokaYokeDeviceNotFoundException(id));
        return toDeviceDetail(d);
    }

    @Transactional(readOnly = true)
    public DmaicDto.DeviceDetail getDeviceByCode(String code) {
        PokaYokeDevice d = deviceRepository.findVisibleByCode(code, requireTenantId())
                .orElseThrow(() -> new PokaYokeDeviceNotFoundException(code));
        return toDeviceDetail(d);
    }

    /**
     * Inscrire un detrompeur a la bibliotheque du client — ce qu'une action
     * d'amelioration devient quand elle a fait la preuve qu'elle empeche
     * l'erreur : une solution reutilisable sur une autre ligne.
     *
     * <p>Le dispositif appartient au client, jamais au catalogue partage : la
     * solution d'un seul n'a pas a s'afficher chez tous. Son code est attribue
     * ici, parce qu'il doit rester unique la ou cohabitent le catalogue de la
     * plateforme et les bibliotheques de chaque client.
     *
     * <p>Quand un projet est designe, le dispositif y est deploye dans le meme
     * mouvement : inscrire sans deployer laisserait l'action a moitie faite, et
     * la bibliotheque se remplirait de solutions que personne n'a posees.
     */
    public DmaicDto.DeviceDetail createDevice(DmaicDto.CreateDeviceRequest req) {
        UUID tenantId = requireTenantId();
        PokaYokeDevice d = new PokaYokeDevice();
        d.setTenantId(tenantId);
        d.setCode(nextDeviceCode(tenantId));
        d.setName(req.name().trim());
        d.setDescription(blankToNull(req.description()));
        d.setType(req.type());
        d.setMechanism(req.mechanism());
        d.setApplicableIndustries(blankToNull(req.applicableIndustries()));
        d.setExamples(blankToNull(req.examples()));
        d.setImplementationCost(blankToNull(req.implementationCost()));
        d.setSourceReference(req.sourceReference().trim());
        PokaYokeDevice saved = deviceRepository.save(d);

        if (req.projectId() != null) {
            assignDevice(req.projectId(), new DmaicDto.AssignPokaYokeRequest(
                    saved.getId(), saved.getSourceReference()));
        }
        return toDeviceDetail(saved);
    }

    /**
     * Le prochain code libre de la bibliotheque du client.
     *
     * <p>Le compteur donne le numero attendu ; la boucle le passe tant qu'il est
     * pris, parce qu'un dispositif supprime laisse un trou que le compteur ne
     * voit pas, et qu'un code deja pris ferait echouer l'ecriture plutot que de
     * se decaler.
     */
    private String nextDeviceCode(UUID tenantId) {
        String prefixe = "PY-" + tenantId.toString().substring(0, 8).toUpperCase();
        long n = deviceRepository.countByTenantId(tenantId) + 1;
        String code = prefixe + "-" + String.format("%03d", n);
        while (deviceRepository.existsByCode(code)) {
            n++;
            code = prefixe + "-" + String.format("%03d", n);
        }
        return code;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    // ===== Assignments =====

    @Transactional(readOnly = true)
    public List<DmaicDto.AssignmentResponse> listAssignments(UUID projectId) {
        loadProject(projectId);
        return assignmentRepository.findByProjectId(projectId).stream()
                .map(this::toAssignmentResponse).toList();
    }

    public DmaicDto.AssignmentResponse assignDevice(UUID projectId,
                                                    DmaicDto.AssignPokaYokeRequest req) {
        UUID tenantId = requireTenantId();
        DmaicProject p = loadProject(projectId);
        if (p.getStatus() != DmaicStatus.ACTIVE) {
            throw new DmaicStateException("Poka-Yoke assignment only on ACTIVE projects");
        }
        PokaYokeDevice d = deviceRepository.findVisibleById(req.deviceId(), tenantId)
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
                d.getTenantId() != null, d.getSourceReference(),
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
