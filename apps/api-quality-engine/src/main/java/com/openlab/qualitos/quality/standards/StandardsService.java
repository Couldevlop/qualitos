package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class StandardsService {

    private final StandardRepository standardRepository;
    private final StandardRequirementRepository requirementRepository;
    private final TenantStandardRepository tenantStandardRepository;
    private final RequirementEvidenceRepository evidenceRepository;
    private final CertificationRoadmapStageRepository roadmapStageRepository;
    private final StandardDocumentTemplateRepository documentTemplateRepository;
    private final StandardProcessTemplateRepository processTemplateRepository;
    private final StandardRevisionRepository revisionRepository;

    public StandardsService(StandardRepository standardRepository,
                            StandardRequirementRepository requirementRepository,
                            TenantStandardRepository tenantStandardRepository,
                            RequirementEvidenceRepository evidenceRepository,
                            CertificationRoadmapStageRepository roadmapStageRepository,
                            StandardDocumentTemplateRepository documentTemplateRepository,
                            StandardProcessTemplateRepository processTemplateRepository,
                            StandardRevisionRepository revisionRepository) {
        this.standardRepository = standardRepository;
        this.requirementRepository = requirementRepository;
        this.tenantStandardRepository = tenantStandardRepository;
        this.evidenceRepository = evidenceRepository;
        this.roadmapStageRepository = roadmapStageRepository;
        this.documentTemplateRepository = documentTemplateRepository;
        this.processTemplateRepository = processTemplateRepository;
        this.revisionRepository = revisionRepository;
    }

    // ===== Catalog =====

    @Transactional(readOnly = true)
    public Page<StandardsDto.StandardSummary> listStandards(StandardStatus status, String family,
                                                            Pageable pageable) {
        Page<Standard> page;
        if (status != null) {
            page = standardRepository.findByStatus(status, pageable);
        } else if (family != null) {
            page = standardRepository.findByFamily(family, pageable);
        } else {
            page = standardRepository.findAll(pageable);
        }
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StandardsDto.StandardDetail getStandard(UUID id) {
        Standard s = standardRepository.findById(id)
                .orElseThrow(() -> new StandardNotFoundException(id));
        return toDetail(s);
    }

    @Transactional(readOnly = true)
    public StandardsDto.StandardDetail getStandardByCode(String code) {
        Standard s = standardRepository.findByCode(code)
                .orElseThrow(() -> new StandardNotFoundException(code));
        return toDetail(s);
    }

    // ===== Adoptions =====

    @Transactional(readOnly = true)
    public Page<StandardsDto.AdoptionResponse> listAdoptions(AdoptionStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<TenantStandard> page = status != null
                ? tenantStandardRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : tenantStandardRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toAdoptionResponse);
    }

    @Transactional(readOnly = true)
    public StandardsDto.AdoptionResponse getAdoption(UUID id) {
        return toAdoptionResponse(loadAdoption(id));
    }

    public StandardsDto.AdoptionResponse adopt(StandardsDto.AdoptRequest request) {
        UUID tenantId = requireTenantId();
        Standard s = standardRepository.findById(request.standardId())
                .orElseThrow(() -> new StandardNotFoundException(request.standardId()));
        if (s.getStatus() != StandardStatus.PUBLISHED) {
            throw new AdoptionConflictException(
                    "Cannot adopt a " + s.getStatus() + " standard");
        }
        if (tenantStandardRepository.existsByTenantIdAndStandardId(tenantId, s.getId())) {
            throw new AdoptionConflictException(
                    "Standard " + s.getCode() + " already adopted by this tenant");
        }
        TenantStandard ts = new TenantStandard();
        ts.setTenantId(tenantId);
        ts.setStandard(s);
        ts.setStatus(AdoptionStatus.PLANNING);
        ts.setScopeDescription(request.scopeDescription());
        ts.setTargetCertificationDate(request.targetCertificationDate());
        ts.setLeadAuditorId(request.leadAuditorId());
        ts.setCertificationBody(request.certificationBody());
        TenantStandard saved = tenantStandardRepository.save(ts);
        generateRoadmap(saved);
        return toAdoptionResponse(saved);
    }

    /** Instancie la trame générique des 19 étapes (§8.5) pour une adoption. */
    private void generateRoadmap(TenantStandard ts) {
        List<CertificationRoadmapStage> stages = new ArrayList<>();
        for (RoadmapTemplate.StageDefinition def : RoadmapTemplate.STAGES) {
            CertificationRoadmapStage stage = new CertificationRoadmapStage();
            stage.setTenantId(ts.getTenantId());
            stage.setTenantStandard(ts);
            stage.setStepNumber(def.stepNumber());
            stage.setName(def.name());
            stage.setDescription(def.description());
            stage.setTypicalDuration(def.typicalDuration());
            stage.setDeliverables(def.deliverables());
            stage.setResponsibleRole(def.responsibleRole());
            stage.setInvolvedModules(def.involvedModules());
            stage.setStatus(StageStatus.NOT_STARTED);
            stage.setOrderIndex(def.stepNumber());
            stages.add(stage);
        }
        roadmapStageRepository.saveAll(stages);
    }

    public StandardsDto.AdoptionResponse updateAdoption(UUID id,
                                                       StandardsDto.UpdateAdoptionRequest request) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() == AdoptionStatus.WITHDRAWN || ts.getStatus() == AdoptionStatus.EXPIRED) {
            throw new AdoptionStateException(
                    "Cannot modify a " + ts.getStatus() + " adoption");
        }
        if (request.scopeDescription() != null) ts.setScopeDescription(request.scopeDescription());
        if (request.targetCertificationDate() != null) ts.setTargetCertificationDate(request.targetCertificationDate());
        if (request.leadAuditorId() != null) ts.setLeadAuditorId(request.leadAuditorId());
        if (request.certificationBody() != null) ts.setCertificationBody(request.certificationBody());
        return toAdoptionResponse(tenantStandardRepository.save(ts));
    }

    public StandardsDto.AdoptionResponse startProgress(UUID id) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() != AdoptionStatus.PLANNING) {
            throw new AdoptionStateException("Only PLANNING adoptions can be moved to IN_PROGRESS");
        }
        ts.setStatus(AdoptionStatus.IN_PROGRESS);
        return toAdoptionResponse(tenantStandardRepository.save(ts));
    }

    public StandardsDto.AdoptionResponse certify(UUID id, StandardsDto.CertifyRequest request) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() != AdoptionStatus.IN_PROGRESS) {
            throw new AdoptionStateException(
                    "Only IN_PROGRESS adoptions can be certified (current: " + ts.getStatus() + ")");
        }
        ts.setStatus(AdoptionStatus.CERTIFIED);
        ts.setCertifiedAt(request.certifiedAt());
        Instant expires = request.expiresAt();
        if (expires == null && ts.getStandard().getRecertificationCycleMonths() != null) {
            expires = request.certifiedAt().plusSeconds(
                    ts.getStandard().getRecertificationCycleMonths() * 30L * 24 * 3600);
        }
        ts.setExpiresAt(expires);
        return toAdoptionResponse(tenantStandardRepository.save(ts));
    }

    public StandardsDto.AdoptionResponse markSurveillance(UUID id) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() != AdoptionStatus.CERTIFIED) {
            throw new AdoptionStateException("Surveillance applies only to CERTIFIED adoptions");
        }
        ts.setStatus(AdoptionStatus.SURVEILLANCE);
        return toAdoptionResponse(tenantStandardRepository.save(ts));
    }

    public StandardsDto.AdoptionResponse withdraw(UUID id) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() == AdoptionStatus.WITHDRAWN) {
            throw new AdoptionStateException("Adoption already withdrawn");
        }
        ts.setStatus(AdoptionStatus.WITHDRAWN);
        return toAdoptionResponse(tenantStandardRepository.save(ts));
    }

    public void deleteAdoption(UUID id) {
        TenantStandard ts = loadAdoption(id);
        if (ts.getStatus() == AdoptionStatus.CERTIFIED || ts.getStatus() == AdoptionStatus.SURVEILLANCE) {
            throw new AdoptionStateException(
                    "Certified adoption cannot be deleted; withdraw it first");
        }
        tenantStandardRepository.delete(ts);
    }

    // ===== Evidence =====

    public StandardsDto.EvidenceResponse linkEvidence(UUID adoptionId,
                                                     StandardsDto.LinkEvidenceRequest request) {
        UUID tenantId = requireTenantId();
        TenantStandard ts = loadAdoption(adoptionId);
        if (ts.getStatus() == AdoptionStatus.WITHDRAWN) {
            throw new AdoptionStateException("Cannot link evidence to a withdrawn adoption");
        }
        StandardRequirement req = requirementRepository.findById(request.requirementId())
                .orElseThrow(() -> new RequirementNotFoundException(request.requirementId()));
        // L'exigence doit appartenir au standard adopté.
        if (!req.getClause().getSection().getStandard().getId().equals(ts.getStandard().getId())) {
            throw new AdoptionStateException(
                    "Requirement does not belong to the adopted standard");
        }
        if (request.evidenceRefId() != null
                && evidenceRepository.existsByTenantStandardIdAndRequirementIdAndEvidenceRefId(
                    adoptionId, req.getId(), request.evidenceRefId())) {
            throw new AdoptionConflictException(
                    "This evidence is already linked to this requirement");
        }

        RequirementEvidence ev = new RequirementEvidence();
        ev.setTenantId(tenantId);
        ev.setTenantStandard(ts);
        ev.setRequirement(req);
        ev.setEvidenceType(request.evidenceType());
        ev.setEvidenceRefId(request.evidenceRefId());
        ev.setEvidenceUri(request.evidenceUri());
        ev.setNote(request.note());
        ev.setLinkedBy(request.linkedBy());
        return toEvidenceResponse(evidenceRepository.save(ev));
    }

    @Transactional(readOnly = true)
    public List<StandardsDto.EvidenceResponse> listEvidence(UUID adoptionId) {
        loadAdoption(adoptionId);
        return evidenceRepository.findByTenantStandardId(adoptionId).stream()
                .map(this::toEvidenceResponse).toList();
    }

    public void deleteEvidence(UUID adoptionId, UUID evidenceId) {
        loadAdoption(adoptionId);
        RequirementEvidence ev = evidenceRepository.findByIdAndTenantStandardId(evidenceId, adoptionId)
                .orElseThrow(() -> new EvidenceNotFoundException(evidenceId));
        evidenceRepository.delete(ev);
    }

    // ===== Alignment scoring =====

    @Transactional(readOnly = true)
    public StandardsDto.AlignmentReport computeAlignment(UUID adoptionId) {
        TenantStandard ts = loadAdoption(adoptionId);
        Standard std = ts.getStandard();

        Set<UUID> coveredRequirementIds = new HashSet<>();
        for (RequirementEvidence ev : evidenceRepository.findByTenantStandardId(adoptionId)) {
            coveredRequirementIds.add(ev.getRequirement().getId());
        }

        int totalReq = 0, coveredReq = 0, totalMust = 0, coveredMust = 0;
        List<StandardsDto.SectionAlignment> sectionResults = new ArrayList<>();

        for (StandardSection section : std.getSections()) {
            int secTotal = 0, secCovered = 0;
            List<StandardsDto.ClauseAlignment> clauseResults = new ArrayList<>();

            for (StandardClause clause : section.getClauses()) {
                int clTotal = clause.getRequirements().size();
                int clCovered = 0;
                for (StandardRequirement req : clause.getRequirements()) {
                    boolean covered = coveredRequirementIds.contains(req.getId());
                    if (covered) clCovered++;
                    if (req.getObligation() == ObligationLevel.MUST) {
                        totalMust++;
                        if (covered) coveredMust++;
                    }
                }
                double clScore = clTotal == 0 ? 0d : (clCovered * 100d) / clTotal;
                clauseResults.add(new StandardsDto.ClauseAlignment(
                        clause.getId(), clause.getCode(), clause.getTitle(),
                        clScore, clTotal, clCovered));
                secTotal += clTotal;
                secCovered += clCovered;
            }
            double secScore = secTotal == 0 ? 0d : (secCovered * 100d) / secTotal;
            sectionResults.add(new StandardsDto.SectionAlignment(
                    section.getId(), section.getCode(), section.getTitle(),
                    secScore, secTotal, secCovered, clauseResults));
            totalReq += secTotal;
            coveredReq += secCovered;
        }
        double overall = totalReq == 0 ? 0d : (coveredReq * 100d) / totalReq;

        return new StandardsDto.AlignmentReport(
                ts.getId(), std.getId(), std.getCode(),
                overall, totalReq, coveredReq, totalMust, coveredMust,
                sectionResults);
    }

    // ===== Audit blanc / gap analysis (§8.7) =====

    @Transactional(readOnly = true)
    public StandardsDto.AuditBlancReport computeAuditBlanc(UUID adoptionId) {
        TenantStandard ts = loadAdoption(adoptionId);
        Standard std = ts.getStandard();

        Set<UUID> covered = new HashSet<>();
        for (RequirementEvidence ev : evidenceRepository.findByTenantStandardId(adoptionId)) {
            covered.add(ev.getRequirement().getId());
        }

        int total = 0, coveredCount = 0, mustTotal = 0, mustCovered = 0;
        int critical = 0, major = 0, minor = 0;
        List<StandardsDto.AuditFinding> findings = new ArrayList<>();

        for (StandardSection section : std.getSections()) {
            for (StandardClause clause : section.getClauses()) {
                for (StandardRequirement req : clause.getRequirements()) {
                    total++;
                    boolean isCovered = covered.contains(req.getId());
                    boolean isMust = req.getObligation() == ObligationLevel.MUST;
                    if (isMust) mustTotal++;
                    if (isCovered) {
                        coveredCount++;
                        if (isMust) mustCovered++;
                        continue;
                    }
                    // Exigence non couverte → écart (finding).
                    String severity = severityOf(req);
                    switch (severity) {
                        case "CRITICAL" -> critical++;
                        case "MAJOR" -> major++;
                        default -> minor++;
                    }
                    findings.add(new StandardsDto.AuditFinding(
                            req.getId(), section.getCode(), clause.getCode(), req.getCode(),
                            req.getText(), req.getObligation(), req.getRiskIfMissing(),
                            severity, req.getEvidenceTypes(),
                            remediationFor(req), priorityOf(severity)));
                }
            }
        }

        // Score de préparation = couverture des exigences MUST (obligatoires pour la certif).
        double readiness = mustTotal == 0 ? 0d : (mustCovered * 100d) / mustTotal;
        findings.sort((a, b) -> Integer.compare(a.remediationPriority(), b.remediationPriority()));

        return new StandardsDto.AuditBlancReport(
                ts.getId(), std.getId(), std.getCode(), std.getFullName(),
                Instant.now(), readiness, total, coveredCount, mustTotal, mustCovered,
                critical, major, minor, verdict(readiness, critical), findings);
    }

    private String severityOf(StandardRequirement req) {
        boolean must = req.getObligation() == ObligationLevel.MUST;
        RiskLevel risk = req.getRiskIfMissing();
        if (must && (risk == RiskLevel.CRITICAL || risk == RiskLevel.HIGH)) return "CRITICAL";
        if (must) return "MAJOR";
        return "MINOR";
    }

    private int priorityOf(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 1;
            case "MAJOR" -> 2;
            default -> 3;
        };
    }

    private String remediationFor(StandardRequirement req) {
        String evidence = req.getEvidenceTypes();
        String base = "Couvrir l'exigence " + req.getCode()
                + " en produisant puis en liant une preuve.";
        if (evidence != null && !evidence.isBlank()) {
            return base + " Preuve attendue : " + evidence + ".";
        }
        return base;
    }

    private String verdict(double readiness, int criticalGaps) {
        if (criticalGaps == 0 && readiness >= 95d) return "PRÊT POUR L'AUDIT";
        if (criticalGaps == 0 && readiness >= 80d) return "QUASI PRÊT — écarts mineurs à traiter";
        if (readiness >= 50d) return "NON PRÊT — écarts majeurs à corriger";
        return "NON PRÊT — préparation insuffisante";
    }

    // ===== Roadmap de certification (§8.5) =====

    @Transactional(readOnly = true)
    public StandardsDto.RoadmapSummary getRoadmap(UUID adoptionId) {
        loadAdoption(adoptionId);
        List<CertificationRoadmapStage> stages =
                roadmapStageRepository.findByTenantStandardIdOrderByOrderIndexAsc(adoptionId);
        int total = stages.size();
        int done = 0, inProgress = 0, skipped = 0;
        List<StandardsDto.RoadmapStageResponse> responses = new ArrayList<>(total);
        for (CertificationRoadmapStage st : stages) {
            switch (st.getStatus()) {
                case DONE -> done++;
                case IN_PROGRESS -> inProgress++;
                case SKIPPED -> skipped++;
                default -> { /* NOT_STARTED */ }
            }
            responses.add(toStageResponse(st));
        }
        // Les étapes SKIPPED sortent du dénominateur de progression.
        int applicable = total - skipped;
        double completion = applicable == 0 ? 0d : (done * 100d) / applicable;
        return new StandardsDto.RoadmapSummary(
                adoptionId, total, done, inProgress, skipped, completion, responses);
    }

    public StandardsDto.RoadmapStageResponse updateStage(
            UUID adoptionId, UUID stageId, StandardsDto.UpdateStageRequest request) {
        loadAdoption(adoptionId);
        CertificationRoadmapStage stage = roadmapStageRepository
                .findByIdAndTenantStandardId(stageId, adoptionId)
                .orElseThrow(() -> new RoadmapStageNotFoundException(stageId));
        if (request.status() != null) stage.setStatus(request.status());
        if (request.assigneeId() != null) stage.setAssigneeId(request.assigneeId());
        if (request.plannedStartDate() != null) stage.setPlannedStartDate(request.plannedStartDate());
        if (request.plannedEndDate() != null) stage.setPlannedEndDate(request.plannedEndDate());
        if (request.actualStartDate() != null) stage.setActualStartDate(request.actualStartDate());
        if (request.actualEndDate() != null) stage.setActualEndDate(request.actualEndDate());
        if (request.notes() != null) stage.setNotes(request.notes());
        return toStageResponse(roadmapStageRepository.save(stage));
    }

    private StandardsDto.RoadmapStageResponse toStageResponse(CertificationRoadmapStage s) {
        return new StandardsDto.RoadmapStageResponse(
                s.getId(), s.getStepNumber(), s.getName(), s.getDescription(),
                s.getTypicalDuration(), s.getDeliverables(), s.getResponsibleRole(),
                s.getInvolvedModules(), s.getStatus(), s.getAssigneeId(),
                s.getPlannedStartDate(), s.getPlannedEndDate(),
                s.getActualStartDate(), s.getActualEndDate(),
                s.getNotes(), s.getOrderIndex());
    }

    // ===== Catalogue : bibliothèque documentaire, processus, veille (§8.4) =====

    @Transactional(readOnly = true)
    public List<StandardsDto.DocumentTemplateResponse> listDocumentTemplates(UUID standardId) {
        requireStandard(standardId);
        return documentTemplateRepository.findByStandardIdOrderByOrderIndexAscNameAsc(standardId)
                .stream().map(this::toDocumentTemplateResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StandardsDto.ProcessTemplateResponse> listProcessTemplates(UUID standardId) {
        requireStandard(standardId);
        return processTemplateRepository.findByStandardIdOrderByOrderIndexAsc(standardId)
                .stream().map(p -> new StandardsDto.ProcessTemplateResponse(
                        p.getId(), p.getCode(), p.getName(), p.getDescription(),
                        p.getMapsToClauses(), p.getBpmnUri(), p.getOrderIndex())).toList();
    }

    @Transactional(readOnly = true)
    public List<StandardsDto.RevisionResponse> listRevisions(UUID standardId) {
        requireStandard(standardId);
        return revisionRepository.findByStandardIdOrderByOrderIndexAsc(standardId)
                .stream().map(r -> new StandardsDto.RevisionResponse(
                        r.getId(), r.getVersion(), r.getStatus(), r.getPublishedDate(),
                        r.getEffectiveDate(), r.getSummary(), r.getImpactNote(),
                        r.getSourceUrl(), r.getOrderIndex())).toList();
    }

    /** Retourne l'URI classpath du modèle, validée, pour le téléchargement (§8.4 onglet 3). */
    @Transactional(readOnly = true)
    public String resolveTemplateUri(UUID standardId, UUID templateId) {
        StandardDocumentTemplate tpl = documentTemplateRepository
                .findByIdAndStandardId(templateId, standardId)
                .orElseThrow(() -> new DocumentTemplateNotFoundException(templateId));
        return tpl.getTemplateUri();
    }

    private void requireStandard(UUID standardId) {
        if (!standardRepository.existsById(standardId)) {
            throw new StandardNotFoundException(standardId);
        }
    }

    private StandardsDto.DocumentTemplateResponse toDocumentTemplateResponse(StandardDocumentTemplate t) {
        return new StandardsDto.DocumentTemplateResponse(
                t.getId(), t.getCode(), t.getName(), t.getObligation(),
                t.getCategory(), formatOf(t.getTemplateUri()), t.getMapsToClauses(),
                t.getDescription(), t.getTemplateUri() != null && !t.getTemplateUri().isBlank());
    }

    /** Format dérivé de l'extension du fichier modèle (MD, DOCX, BPMN, XLSX…). */
    private String formatOf(String uri) {
        if (uri == null) return null;
        int dot = uri.lastIndexOf('.');
        return dot < 0 ? null : uri.substring(dot + 1).toUpperCase();
    }

    // ===== helpers =====

    private TenantStandard loadAdoption(UUID id) {
        UUID tenantId = requireTenantId();
        return tenantStandardRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TenantStandardNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private StandardsDto.StandardSummary toSummary(Standard s) {
        return new StandardsDto.StandardSummary(
                s.getId(), s.getCode(), s.getFullName(), s.getPublisher(),
                s.getCurrentVersion(), s.getFamily(), s.getApplicableIndustries(),
                s.getStatus(), s.getRecertificationCycleMonths());
    }

    private StandardsDto.StandardDetail toDetail(Standard s) {
        List<StandardsDto.SectionDetail> sections = s.getSections().stream().map(sec ->
            new StandardsDto.SectionDetail(
                sec.getId(), sec.getCode(), sec.getTitle(), sec.getDescription(), sec.getOrderIndex(),
                sec.getClauses().stream().map(cl ->
                    new StandardsDto.ClauseDetail(
                        cl.getId(), cl.getCode(), cl.getTitle(), cl.getDescription(), cl.getOrderIndex(),
                        cl.getRequirements().stream().map(r ->
                            new StandardsDto.RequirementDetail(
                                r.getId(), r.getCode(), r.getText(), r.getObligation(),
                                r.getEvidenceTypes(), r.getMeasurableCriteria(), r.getRiskIfMissing(),
                                r.getOrderIndex())
                        ).toList())
                ).toList())
        ).toList();
        return new StandardsDto.StandardDetail(
                s.getId(), s.getCode(), s.getFullName(), s.getPublisher(),
                s.getCurrentVersion(), s.getPublicationDate(), s.getFamily(),
                s.getApplicableIndustries(), s.getDescription(),
                s.isCertificationBodyRequired(), s.getRecertificationCycleMonths(),
                s.getRelatedNormCodes(), s.getStatus(), sections);
    }

    private StandardsDto.AdoptionResponse toAdoptionResponse(TenantStandard ts) {
        Standard s = ts.getStandard();
        return new StandardsDto.AdoptionResponse(
                ts.getId(), ts.getTenantId(), s.getId(), s.getCode(), s.getFullName(),
                ts.getStatus(), ts.getScopeDescription(), ts.getTargetCertificationDate(),
                ts.getLeadAuditorId(), ts.getCertificationBody(),
                ts.getCertifiedAt(), ts.getExpiresAt(),
                ts.getCreatedAt(), ts.getUpdatedAt());
    }

    private StandardsDto.EvidenceResponse toEvidenceResponse(RequirementEvidence ev) {
        return new StandardsDto.EvidenceResponse(
                ev.getId(), ev.getTenantStandard().getId(),
                ev.getRequirement().getId(), ev.getRequirement().getCode(),
                ev.getEvidenceType(), ev.getEvidenceRefId(), ev.getEvidenceUri(),
                ev.getNote(), ev.getLinkedBy(), ev.getLinkedAt());
    }
}
