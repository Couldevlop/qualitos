package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    private final AuditPlanRepository planRepository;
    private final AuditChecklistItemRepository checklistRepository;
    private final AuditFindingRepository findingRepository;

    public AuditService(AuditPlanRepository planRepository,
                        AuditChecklistItemRepository checklistRepository,
                        AuditFindingRepository findingRepository) {
        this.planRepository = planRepository;
        this.checklistRepository = checklistRepository;
        this.findingRepository = findingRepository;
    }

    // --- plans ---

    @Transactional(readOnly = true)
    public Page<AuditDto.PlanResponse> findAll(AuditStatus status, AuditType type, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<AuditPlan> page;
        if (status != null) {
            page = planRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (type != null) {
            page = planRepository.findByTenantIdAndType(tenantId, type, pageable);
        } else {
            page = planRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditDto.PlanResponse findById(UUID id) {
        return toResponse(loadPlan(id));
    }

    public AuditDto.PlanResponse createPlan(AuditDto.CreatePlanRequest req) {
        UUID tenantId = requireTenantId();
        AuditPlan p = new AuditPlan();
        p.setTenantId(tenantId);
        p.setTitle(req.title());
        p.setScope(req.scope());
        p.setType(req.type());
        p.setStatus(AuditStatus.PLANNED);
        p.setStandard(req.standard());
        p.setLeadAuditorId(req.leadAuditorId());
        p.setAuditeeId(req.auditeeId());
        p.setScheduledDate(req.scheduledDate());
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse updatePlan(UUID id, AuditDto.UpdatePlanRequest req) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot modify a " + p.getStatus() + " audit plan");
        }
        if (req.title() != null) p.setTitle(req.title());
        if (req.scope() != null) p.setScope(req.scope());
        if (req.type() != null) p.setType(req.type());
        if (req.standard() != null) p.setStandard(req.standard());
        if (req.leadAuditorId() != null) p.setLeadAuditorId(req.leadAuditorId());
        if (req.auditeeId() != null) p.setAuditeeId(req.auditeeId());
        if (req.scheduledDate() != null) p.setScheduledDate(req.scheduledDate());
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse startPlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() != AuditStatus.PLANNED) {
            throw new AuditStateException("Only PLANNED audits can be started");
        }
        if (p.getChecklist().isEmpty()) {
            throw new AuditStateException("Cannot start an audit without checklist items");
        }
        p.setStatus(AuditStatus.IN_PROGRESS);
        p.setStartedAt(Instant.now());
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse completePlan(UUID id, AuditDto.CompleteRequest req) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Only IN_PROGRESS audits can be completed");
        }
        boolean allAnswered = p.getChecklist().stream()
                .allMatch(i -> i.getConformant() != null);
        if (!allAnswered) {
            throw new AuditStateException("All checklist items must be answered before completion");
        }
        p.setStatus(AuditStatus.COMPLETED);
        p.setCompletedAt(Instant.now());
        if (req != null && req.reportSummary() != null) {
            p.setReportSummary(req.reportSummary());
        }
        return toResponse(planRepository.save(p));
    }

    public AuditDto.PlanResponse cancelPlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Completed audit cannot be cancelled");
        }
        if (p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Audit is already cancelled");
        }
        p.setStatus(AuditStatus.CANCELLED);
        return toResponse(planRepository.save(p));
    }

    public void deletePlan(UUID id) {
        AuditPlan p = loadPlan(id);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Completed audit cannot be deleted");
        }
        planRepository.delete(p);
    }

    // --- checklist ---

    public AuditDto.ChecklistItemResponse addChecklistItem(UUID planId, AuditDto.ChecklistItemRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot edit checklist of a " + p.getStatus() + " audit");
        }
        AuditChecklistItem item = new AuditChecklistItem();
        item.setPlan(p);
        item.setQuestion(req.question());
        item.setClauseRef(req.clauseRef());
        item.setExpectedEvidence(req.expectedEvidence());
        item.setWeight(req.weight() != null ? req.weight() : 1);
        item.setOrderIndex(req.orderIndex() != null ? req.orderIndex() : p.getChecklist().size());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public AuditDto.ChecklistItemResponse updateChecklistItem(UUID planId, UUID itemId,
                                                              AuditDto.ChecklistItemRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED || p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot edit checklist of a " + p.getStatus() + " audit");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        if (req.question() != null) item.setQuestion(req.question());
        if (req.clauseRef() != null) item.setClauseRef(req.clauseRef());
        if (req.expectedEvidence() != null) item.setExpectedEvidence(req.expectedEvidence());
        if (req.weight() != null) item.setWeight(req.weight());
        if (req.orderIndex() != null) item.setOrderIndex(req.orderIndex());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public AuditDto.ChecklistItemResponse respondToChecklistItem(UUID planId, UUID itemId,
                                                                 AuditDto.ChecklistResponseRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Responses can only be recorded while audit is IN_PROGRESS");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        if (req.response() != null) item.setResponse(req.response());
        if (req.conformant() != null) item.setConformant(req.conformant());
        return toChecklistResponse(checklistRepository.save(item));
    }

    public void deleteChecklistItem(UUID planId, UUID itemId) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.PLANNED) {
            throw new AuditStateException("Checklist items can only be deleted while PLANNED");
        }
        AuditChecklistItem item = checklistRepository.findByIdAndPlanId(itemId, planId)
                .orElseThrow(() -> new AuditChecklistItemNotFoundException(itemId));
        checklistRepository.delete(item);
    }

    // --- findings ---

    public AuditDto.FindingResponse addFinding(UUID planId, AuditDto.FindingRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new AuditStateException("Findings can only be raised while audit is IN_PROGRESS");
        }
        AuditFinding f = new AuditFinding();
        f.setPlan(p);
        f.setType(req.type());
        f.setDescription(req.description());
        f.setClauseRef(req.clauseRef());
        f.setPhotoUrl(req.photoUrl());
        f.setCapaId(req.capaId());
        f.setRaisedBy(req.raisedBy());
        f.setRaisedAt(Instant.now());
        if (req.checklistItemId() != null) {
            AuditChecklistItem item = checklistRepository
                    .findByIdAndPlanId(req.checklistItemId(), planId)
                    .orElseThrow(() -> new AuditChecklistItemNotFoundException(req.checklistItemId()));
            f.setChecklistItem(item);
        }
        return toFindingResponse(findingRepository.save(f));
    }

    public AuditDto.FindingResponse updateFinding(UUID planId, UUID findingId,
                                                  AuditDto.UpdateFindingRequest req) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot modify findings on a cancelled audit");
        }
        AuditFinding f = findingRepository.findByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        if (req.type() != null) f.setType(req.type());
        if (req.description() != null) f.setDescription(req.description());
        if (req.clauseRef() != null) f.setClauseRef(req.clauseRef());
        if (req.photoUrl() != null) f.setPhotoUrl(req.photoUrl());
        if (req.capaId() != null) f.setCapaId(req.capaId());
        return toFindingResponse(findingRepository.save(f));
    }

    public void deleteFinding(UUID planId, UUID findingId) {
        AuditPlan p = loadPlan(planId);
        if (p.getStatus() == AuditStatus.COMPLETED) {
            throw new AuditStateException("Cannot delete findings on a completed audit");
        }
        AuditFinding f = findingRepository.findByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        findingRepository.delete(f);
    }

    // --- helpers ---

    private AuditPlan loadPlan(UUID id) {
        UUID tenantId = requireTenantId();
        return planRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /** Score pondéré 0..100 (conformes/total pondéré). */
    static Double computeConformityScore(AuditPlan p) {
        if (p.getChecklist().isEmpty()) return null;
        int totalWeight = 0;
        int conformantWeight = 0;
        for (AuditChecklistItem i : p.getChecklist()) {
            if (i.getConformant() == null) continue;
            int w = i.getWeight() != null ? i.getWeight() : 1;
            totalWeight += w;
            if (Boolean.TRUE.equals(i.getConformant())) {
                conformantWeight += w;
            }
        }
        if (totalWeight == 0) return null;
        return (conformantWeight * 100d) / totalWeight;
    }

    private AuditDto.PlanResponse toResponse(AuditPlan p) {
        return new AuditDto.PlanResponse(
                p.getId(), p.getTenantId(), p.getTitle(), p.getScope(),
                p.getType(), p.getStatus(), p.getStandard(),
                p.getLeadAuditorId(), p.getAuditeeId(), p.getScheduledDate(),
                p.getStartedAt(), p.getCompletedAt(), p.getReportSummary(),
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getChecklist().stream().map(this::toChecklistResponse).toList(),
                p.getFindings().stream().map(this::toFindingResponse).toList(),
                computeConformityScore(p));
    }

    private AuditDto.ChecklistItemResponse toChecklistResponse(AuditChecklistItem i) {
        return new AuditDto.ChecklistItemResponse(
                i.getId(), i.getPlan().getId(), i.getQuestion(), i.getClauseRef(),
                i.getExpectedEvidence(), i.getWeight(), i.getOrderIndex(),
                i.getResponse(), i.getConformant(), i.getCreatedAt(), i.getUpdatedAt());
    }

    private AuditDto.FindingResponse toFindingResponse(AuditFinding f) {
        return new AuditDto.FindingResponse(
                f.getId(), f.getPlan().getId(),
                f.getChecklistItem() != null ? f.getChecklistItem().getId() : null,
                f.getType(), f.getDescription(), f.getClauseRef(), f.getPhotoUrl(),
                f.getCapaId(), f.getRaisedBy(), f.getRaisedAt(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
