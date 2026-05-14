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

    public StandardsService(StandardRepository standardRepository,
                            StandardRequirementRepository requirementRepository,
                            TenantStandardRepository tenantStandardRepository,
                            RequirementEvidenceRepository evidenceRepository) {
        this.standardRepository = standardRepository;
        this.requirementRepository = requirementRepository;
        this.tenantStandardRepository = tenantStandardRepository;
        this.evidenceRepository = evidenceRepository;
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
        return toAdoptionResponse(tenantStandardRepository.save(ts));
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
