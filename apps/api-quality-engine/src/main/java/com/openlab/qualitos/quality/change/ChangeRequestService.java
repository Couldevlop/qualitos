package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestration des change requests + impacts + approbations (§4.8).
 *
 * Lifecycle (toute transition non listée = 409) :
 *   DRAFT        → SUBMITTED (submit; au moins 1 approbateur requis)
 *   SUBMITTED    → UNDER_REVIEW (auto à la 1re décision non-PENDING)
 *   UNDER_REVIEW → APPROVED      (toutes les approbations APPROVED)
 *   UNDER_REVIEW → REJECTED      (au moins une REJECTED) — terminal
 *   APPROVED     → IMPLEMENTED   (transition explicite) — terminal
 *   DRAFT|SUBMITTED|UNDER_REVIEW → CANCELLED (par requester/owner) — terminal
 *
 * Les impacts et approbateurs ne peuvent être modifiés QUE en DRAFT
 * (avant le passage SUBMITTED, pour ne pas tordre la fenêtre de revue).
 */
@Service
public class ChangeRequestService {

    private final ChangeRequestRepository requestRepo;
    private final ChangeImpactRepository impactRepo;
    private final ChangeApprovalRepository approvalRepo;
    private final Clock clock;

    public ChangeRequestService(ChangeRequestRepository requestRepo,
                                ChangeImpactRepository impactRepo,
                                ChangeApprovalRepository approvalRepo) {
        this(requestRepo, impactRepo, approvalRepo, Clock.systemUTC());
    }

    ChangeRequestService(ChangeRequestRepository requestRepo,
                         ChangeImpactRepository impactRepo,
                         ChangeApprovalRepository approvalRepo,
                         Clock clock) {
        this.requestRepo = requestRepo;
        this.impactRepo = impactRepo;
        this.approvalRepo = approvalRepo;
        this.clock = clock;
    }

    // ---------- CRUD ----------

    @Transactional
    public ChangeDto.ChangeResponse create(ChangeDto.CreateChangeRequest req) {
        UUID tenantId = requireTenantId();
        requestRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(c -> {
            throw new ChangeStateException("Change code already exists: " + req.code());
        });
        ChangeRequest c = new ChangeRequest();
        c.setTenantId(tenantId);
        c.setCode(req.code());
        c.setTitle(req.title());
        c.setDescription(req.description());
        c.setType(req.type());
        c.setPriority(req.priority() == null ? ChangeRequestPriority.MEDIUM : req.priority());
        c.setStatus(ChangeRequestStatus.DRAFT);
        c.setRequesterUserId(req.requesterUserId());
        c.setOwnerUserId(req.ownerUserId());
        c.setPlannedFor(req.plannedFor());
        c.setImpactSummary(req.impactSummary());
        c.setRiskAssessment(req.riskAssessment());
        return toResponse(requestRepo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ChangeDto.ChangeResponse> list(ChangeRequestStatus status, ChangeRequestType type,
                                               Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<ChangeRequest> page;
        if (status != null) page = requestRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (type != null) page = requestRepo.findByTenantIdAndType(tenantId, type, pageable);
        else page = requestRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ChangeDto.ChangeResponse get(UUID id) { return toResponse(loadChange(id)); }

    @Transactional
    public ChangeDto.ChangeResponse update(UUID id, ChangeDto.UpdateChangeRequest req) {
        ChangeRequest c = loadChange(id);
        if (isTerminal(c.getStatus())) {
            throw new ChangeStateException(
                    "Cannot edit a change request in terminal status " + c.getStatus());
        }
        if (req.title() != null) c.setTitle(req.title());
        if (req.description() != null) c.setDescription(req.description());
        if (req.priority() != null) c.setPriority(req.priority());
        if (req.ownerUserId() != null) c.setOwnerUserId(req.ownerUserId());
        if (req.plannedFor() != null) c.setPlannedFor(req.plannedFor());
        if (req.impactSummary() != null) c.setImpactSummary(req.impactSummary());
        if (req.riskAssessment() != null) c.setRiskAssessment(req.riskAssessment());
        return toResponse(requestRepo.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        ChangeRequest c = loadChange(id);
        if (c.getStatus() != ChangeRequestStatus.DRAFT
                && c.getStatus() != ChangeRequestStatus.CANCELLED) {
            throw new ChangeStateException(
                    "Cannot delete a change request in status " + c.getStatus()
                            + "; cancel it first");
        }
        impactRepo.deleteByChangeId(id);
        approvalRepo.deleteByChangeId(id);
        requestRepo.delete(c);
    }

    // ---------- Workflow ----------

    @Transactional
    public ChangeDto.ChangeResponse submit(UUID id) {
        ChangeRequest c = loadChange(id);
        if (c.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new ChangeStateException(
                    "Only DRAFT changes can be submitted (current: " + c.getStatus() + ")");
        }
        if (approvalRepo.countByChangeId(id) == 0) {
            throw new ChangeStateException("At least one approver must be assigned before submit");
        }
        c.setStatus(ChangeRequestStatus.SUBMITTED);
        return toResponse(requestRepo.save(c));
    }

    @Transactional
    public ChangeDto.ChangeResponse cancel(UUID id, String reason) {
        ChangeRequest c = loadChange(id);
        if (isTerminal(c.getStatus())) {
            throw new ChangeStateException(
                    "Cannot cancel a change in terminal status " + c.getStatus());
        }
        c.setStatus(ChangeRequestStatus.CANCELLED);
        if (reason != null) c.setRejectionReason(reason);
        return toResponse(requestRepo.save(c));
    }

    @Transactional
    public ChangeDto.ChangeResponse implement(UUID id, ChangeDto.ImplementRequest req) {
        ChangeRequest c = loadChange(id);
        if (c.getStatus() != ChangeRequestStatus.APPROVED) {
            throw new ChangeStateException(
                    "Only APPROVED changes can be implemented (current: " + c.getStatus() + ")");
        }
        c.setStatus(ChangeRequestStatus.IMPLEMENTED);
        c.setImplementedAt(req.implementedAt());
        return toResponse(requestRepo.save(c));
    }

    // ---------- Approvers ----------

    @Transactional
    public ChangeDto.ApprovalResponse addApprover(UUID changeId, ChangeDto.AddApproverRequest req) {
        ChangeRequest c = loadChange(changeId);
        if (c.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new ChangeStateException(
                    "Approvers can only be added while the change is in DRAFT");
        }
        approvalRepo.findByChangeIdAndApproverUserId(changeId, req.approverUserId())
                .ifPresent(a -> {
                    throw new ChangeStateException(
                            "Approver already assigned to this change");
                });
        ChangeApproval a = new ChangeApproval();
        a.setTenantId(c.getTenantId());
        a.setChangeId(changeId);
        a.setApproverUserId(req.approverUserId());
        a.setApprovalLevel(req.approvalLevel() == null ? 1 : req.approvalLevel());
        a.setDecision(ApprovalDecision.PENDING);
        return toResponse(approvalRepo.save(a));
    }

    @Transactional
    public void removeApprover(UUID changeId, UUID approverUserId) {
        ChangeRequest c = loadChange(changeId);
        if (c.getStatus() != ChangeRequestStatus.DRAFT) {
            throw new ChangeStateException("Approvers can only be removed while in DRAFT");
        }
        ChangeApproval a = approvalRepo.findByChangeIdAndApproverUserId(changeId, approverUserId)
                .orElseThrow(() -> new ChangeChildNotFoundException("Approval", approverUserId));
        approvalRepo.delete(a);
    }

    @Transactional
    public ChangeDto.ApprovalResponse decide(UUID changeId, ChangeDto.DecisionRequest req) {
        ChangeRequest c = loadChange(changeId);
        if (c.getStatus() != ChangeRequestStatus.SUBMITTED
                && c.getStatus() != ChangeRequestStatus.UNDER_REVIEW) {
            throw new ChangeStateException(
                    "Decisions are only accepted on SUBMITTED or UNDER_REVIEW changes (current: "
                            + c.getStatus() + ")");
        }
        if (req.decision() == ApprovalDecision.PENDING) {
            throw new ChangeStateException("Cannot record a PENDING decision");
        }
        ChangeApproval a = approvalRepo
                .findByChangeIdAndApproverUserId(changeId, req.approverUserId())
                .orElseThrow(() -> new ChangeChildNotFoundException(
                        "Approval", req.approverUserId()));
        if (a.getDecision() != ApprovalDecision.PENDING) {
            throw new ChangeStateException(
                    "Approver already decided: " + a.getDecision());
        }
        a.setDecision(req.decision());
        a.setComment(req.comment());
        a.setDecidedAt(Instant.now(clock));
        ChangeApproval saved = approvalRepo.save(a);

        // Recalcule statut du change.
        long total = approvalRepo.countByChangeId(changeId);
        long approved = approvalRepo.countByChangeIdAndDecision(changeId, ApprovalDecision.APPROVED);
        long rejected = approvalRepo.countByChangeIdAndDecision(changeId, ApprovalDecision.REJECTED);
        if (rejected > 0) {
            c.setStatus(ChangeRequestStatus.REJECTED);
            c.setRejectionReason(req.comment());
        } else if (approved == total) {
            c.setStatus(ChangeRequestStatus.APPROVED);
        } else {
            c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        }
        requestRepo.save(c);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChangeDto.ApprovalResponse> listApprovals(UUID changeId) {
        loadChange(changeId);
        return approvalRepo.findByChangeIdOrderByApprovalLevelAsc(changeId)
                .stream().map(this::toResponse).toList();
    }

    // ---------- Impacts ----------

    @Transactional
    public ChangeDto.ImpactResponse addImpact(UUID changeId, ChangeDto.AddImpactRequest req) {
        ChangeRequest c = loadChange(changeId);
        if (isTerminal(c.getStatus())) {
            throw new ChangeStateException(
                    "Cannot modify impacts on a change in terminal status " + c.getStatus());
        }
        impactRepo.findByChangeIdAndTargetTypeAndTargetId(changeId, req.targetType(), req.targetId())
                .ifPresent(i -> {
                    throw new ChangeStateException(
                            "Target already linked to this change");
                });
        ChangeImpact i = new ChangeImpact();
        i.setTenantId(c.getTenantId());
        i.setChangeId(changeId);
        i.setTargetType(req.targetType());
        i.setTargetId(req.targetId());
        i.setNotes(req.notes());
        return toResponse(impactRepo.save(i));
    }

    @Transactional
    public void removeImpact(UUID changeId, UUID impactId) {
        ChangeRequest c = loadChange(changeId);
        if (isTerminal(c.getStatus())) {
            throw new ChangeStateException(
                    "Cannot modify impacts on a change in terminal status " + c.getStatus());
        }
        ChangeImpact i = impactRepo.findById(impactId)
                .orElseThrow(() -> new ChangeChildNotFoundException("Impact", impactId));
        if (!i.getChangeId().equals(changeId) || !i.getTenantId().equals(c.getTenantId())) {
            throw new ChangeChildNotFoundException("Impact", impactId);
        }
        impactRepo.delete(i);
    }

    @Transactional(readOnly = true)
    public List<ChangeDto.ImpactResponse> listImpacts(UUID changeId) {
        loadChange(changeId);
        return impactRepo.findByChangeId(changeId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ChangeDto.ChangeSummary summary(UUID changeId) {
        ChangeRequest c = loadChange(changeId);
        long total = approvalRepo.countByChangeId(changeId);
        long approved = approvalRepo.countByChangeIdAndDecision(changeId, ApprovalDecision.APPROVED);
        long rejected = approvalRepo.countByChangeIdAndDecision(changeId, ApprovalDecision.REJECTED);
        long pending = approvalRepo.countByChangeIdAndDecision(changeId, ApprovalDecision.PENDING);
        List<ChangeDto.ApprovalResponse> approvals =
                approvalRepo.findByChangeIdOrderByApprovalLevelAsc(changeId)
                        .stream().map(this::toResponse).toList();
        List<ChangeDto.ImpactResponse> impacts =
                impactRepo.findByChangeId(changeId).stream().map(this::toResponse).toList();
        return new ChangeDto.ChangeSummary(
                changeId, c.getStatus(), total, approved, rejected, pending,
                impacts.size(), approvals, impacts);
    }

    // ---------- helpers ----------

    ChangeRequest loadChange(UUID id) {
        UUID tenantId = requireTenantId();
        ChangeRequest c = requestRepo.findById(id)
                .orElseThrow(() -> new ChangeRequestNotFoundException(id));
        if (!c.getTenantId().equals(tenantId)) throw new ChangeRequestNotFoundException(id);
        return c;
    }

    private static boolean isTerminal(ChangeRequestStatus s) {
        return s == ChangeRequestStatus.IMPLEMENTED
                || s == ChangeRequestStatus.REJECTED
                || s == ChangeRequestStatus.CANCELLED;
    }

    private ChangeDto.ChangeResponse toResponse(ChangeRequest c) {
        return new ChangeDto.ChangeResponse(
                c.getId(), c.getTenantId(), c.getCode(), c.getTitle(), c.getDescription(),
                c.getType(), c.getPriority(), c.getStatus(),
                c.getRequesterUserId(), c.getOwnerUserId(),
                c.getPlannedFor(), c.getImplementedAt(),
                c.getImpactSummary(), c.getRiskAssessment(), c.getRejectionReason(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private ChangeDto.ImpactResponse toResponse(ChangeImpact i) {
        return new ChangeDto.ImpactResponse(
                i.getId(), i.getTenantId(), i.getChangeId(),
                i.getTargetType(), i.getTargetId(), i.getNotes(), i.getCreatedAt());
    }

    private ChangeDto.ApprovalResponse toResponse(ChangeApproval a) {
        return new ChangeDto.ApprovalResponse(
                a.getId(), a.getTenantId(), a.getChangeId(), a.getApproverUserId(),
                a.getApprovalLevel(), a.getDecision(), a.getComment(),
                a.getDecidedAt(), a.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
