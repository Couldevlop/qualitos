package com.openlab.qualitos.quality.risk;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service de gestion des projets FMEA et de leurs lignes (CLAUDE.md §4.5).
 *
 * Règles principales :
 * - Les projets ARCHIVED sont immuables (pas d'ajout/modif/suppression d'items).
 * - Le RPN est toujours recalculé côté serveur depuis severity*occurrence*detection ;
 *   le champ envoyé par le client (s'il existe) est ignoré.
 * - Transitions :
 *      DRAFT     → ACTIVE      (activate, incrémente la revision si on revient d'ACTIVE)
 *      ACTIVE    → DRAFT       (reopen, pour révision)
 *      DRAFT/ACTIVE → ARCHIVED (archive, terminal)
 *      ARCHIVED → *            INTERDIT
 */
@Service
public class FmeaService {

    private final FmeaProjectRepository projectRepo;
    private final FmeaItemRepository itemRepo;

    public FmeaService(FmeaProjectRepository projectRepo, FmeaItemRepository itemRepo) {
        this.projectRepo = projectRepo;
        this.itemRepo = itemRepo;
    }

    // ---------- Projects ----------

    @Transactional
    public FmeaDto.ProjectResponse createProject(FmeaDto.CreateProjectRequest req) {
        UUID tenantId = requireTenantId();
        projectRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(p -> {
            throw new FmeaStateException("FMEA project code already exists: " + req.code());
        });
        FmeaProject p = new FmeaProject();
        p.setTenantId(tenantId);
        p.setCode(req.code());
        p.setName(req.name());
        p.setScope(req.scope());
        p.setType(req.type());
        p.setStatus(FmeaStatus.DRAFT);
        p.setRevision(1);
        p.setCriticalRpnThreshold(req.criticalRpnThreshold() == null ? 100 : req.criticalRpnThreshold());
        p.setOwnerUserId(req.ownerUserId());
        p.setCreatedBy(req.createdBy());
        return toResponse(projectRepo.save(p));
    }

    @Transactional(readOnly = true)
    public Page<FmeaDto.ProjectResponse> listProjects(FmeaStatus status, FmeaType type, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<FmeaProject> page;
        if (status != null) page = projectRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (type != null) page = projectRepo.findByTenantIdAndType(tenantId, type, pageable);
        else page = projectRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FmeaDto.ProjectResponse getProject(UUID id) {
        return toResponse(loadProjectForTenant(id));
    }

    @Transactional
    public FmeaDto.ProjectResponse updateProject(UUID id, FmeaDto.UpdateProjectRequest req) {
        FmeaProject p = loadProjectForTenant(id);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Cannot edit an ARCHIVED FMEA project");
        }
        if (req.name() != null) p.setName(req.name());
        if (req.scope() != null) p.setScope(req.scope());
        if (req.criticalRpnThreshold() != null) p.setCriticalRpnThreshold(req.criticalRpnThreshold());
        if (req.ownerUserId() != null) p.setOwnerUserId(req.ownerUserId());
        return toResponse(projectRepo.save(p));
    }

    @Transactional
    public void deleteProject(UUID id) {
        FmeaProject p = loadProjectForTenant(id);
        if (p.getStatus() == FmeaStatus.ACTIVE) {
            throw new FmeaStateException("Cannot delete an ACTIVE project; archive it first");
        }
        itemRepo.deleteByProjectId(id);
        projectRepo.delete(p);
    }

    @Transactional
    public FmeaDto.ProjectResponse activate(UUID id) {
        FmeaProject p = loadProjectForTenant(id);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Cannot reactivate an ARCHIVED project");
        }
        if (p.getStatus() == FmeaStatus.ACTIVE) return toResponse(p); // idempotent
        p.setStatus(FmeaStatus.ACTIVE);
        p.setLastReviewedAt(Instant.now());
        return toResponse(projectRepo.save(p));
    }

    @Transactional
    public FmeaDto.ProjectResponse reopen(UUID id) {
        FmeaProject p = loadProjectForTenant(id);
        if (p.getStatus() != FmeaStatus.ACTIVE) {
            throw new FmeaStateException("Only ACTIVE projects can be re-opened for revision");
        }
        p.setStatus(FmeaStatus.DRAFT);
        p.setRevision(p.getRevision() + 1);
        return toResponse(projectRepo.save(p));
    }

    @Transactional
    public FmeaDto.ProjectResponse archive(UUID id) {
        FmeaProject p = loadProjectForTenant(id);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Project is already ARCHIVED");
        }
        p.setStatus(FmeaStatus.ARCHIVED);
        return toResponse(projectRepo.save(p));
    }

    // ---------- Items ----------

    @Transactional
    public FmeaDto.ItemResponse addItem(UUID projectId, FmeaDto.CreateItemRequest req) {
        FmeaProject p = loadProjectForTenant(projectId);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Cannot add items to an ARCHIVED project");
        }
        FmeaItem i = new FmeaItem();
        i.setTenantId(p.getTenantId());
        i.setProjectId(p.getId());
        i.setSequenceNo(itemRepo.findMaxSequenceNo(p.getId()) + 1);
        i.setFunction(req.function());
        i.setFailureMode(req.failureMode());
        i.setFailureEffect(req.failureEffect());
        i.setFailureCause(req.failureCause());
        i.setCurrentControls(req.currentControls());
        i.setSeverity(req.severity());
        i.setOccurrence(req.occurrence());
        i.setDetection(req.detection());
        i.setRecommendedAction(req.recommendedAction());
        i.setActionOwnerUserId(req.actionOwnerUserId());
        i.setActionDueDate(req.actionDueDate());
        i.setResultingSeverity(req.resultingSeverity());
        i.setResultingOccurrence(req.resultingOccurrence());
        i.setResultingDetection(req.resultingDetection());
        i.recomputeRpn();
        return toResponse(itemRepo.save(i), p.getCriticalRpnThreshold());
    }

    @Transactional
    public FmeaDto.ItemResponse updateItem(UUID projectId, UUID itemId, FmeaDto.UpdateItemRequest req) {
        FmeaProject p = loadProjectForTenant(projectId);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Cannot edit items of an ARCHIVED project");
        }
        FmeaItem i = itemRepo.findById(itemId).orElseThrow(() -> new FmeaItemNotFoundException(itemId));
        if (!i.getProjectId().equals(projectId) || !i.getTenantId().equals(p.getTenantId())) {
            throw new FmeaItemNotFoundException(itemId);
        }
        if (req.function() != null) i.setFunction(req.function());
        if (req.failureMode() != null) i.setFailureMode(req.failureMode());
        if (req.failureEffect() != null) i.setFailureEffect(req.failureEffect());
        if (req.failureCause() != null) i.setFailureCause(req.failureCause());
        if (req.currentControls() != null) i.setCurrentControls(req.currentControls());
        if (req.severity() != null) i.setSeverity(req.severity());
        if (req.occurrence() != null) i.setOccurrence(req.occurrence());
        if (req.detection() != null) i.setDetection(req.detection());
        if (req.recommendedAction() != null) i.setRecommendedAction(req.recommendedAction());
        if (req.actionOwnerUserId() != null) i.setActionOwnerUserId(req.actionOwnerUserId());
        if (req.actionDueDate() != null) i.setActionDueDate(req.actionDueDate());
        if (req.resultingSeverity() != null) i.setResultingSeverity(req.resultingSeverity());
        if (req.resultingOccurrence() != null) i.setResultingOccurrence(req.resultingOccurrence());
        if (req.resultingDetection() != null) i.setResultingDetection(req.resultingDetection());
        i.recomputeRpn();
        return toResponse(itemRepo.save(i), p.getCriticalRpnThreshold());
    }

    @Transactional
    public void deleteItem(UUID projectId, UUID itemId) {
        FmeaProject p = loadProjectForTenant(projectId);
        if (p.getStatus() == FmeaStatus.ARCHIVED) {
            throw new FmeaStateException("Cannot delete items of an ARCHIVED project");
        }
        FmeaItem i = itemRepo.findById(itemId).orElseThrow(() -> new FmeaItemNotFoundException(itemId));
        if (!i.getProjectId().equals(projectId) || !i.getTenantId().equals(p.getTenantId())) {
            throw new FmeaItemNotFoundException(itemId);
        }
        itemRepo.delete(i);
    }

    @Transactional(readOnly = true)
    public Page<FmeaDto.ItemResponse> listItems(UUID projectId, Pageable pageable) {
        FmeaProject p = loadProjectForTenant(projectId);
        return itemRepo.findByProjectIdOrderBySequenceNoAsc(projectId, pageable)
                .map(i -> toResponse(i, p.getCriticalRpnThreshold()));
    }

    @Transactional(readOnly = true)
    public FmeaDto.ProjectStatistics statistics(UUID projectId) {
        FmeaProject p = loadProjectForTenant(projectId);
        long total = itemRepo.countByProjectId(projectId);
        long critical = itemRepo.countByProjectIdAndRpnGreaterThanEqual(projectId, p.getCriticalRpnThreshold());
        int max = itemRepo.maxRpn(projectId);
        double avg = itemRepo.averageRpn(projectId);
        return new FmeaDto.ProjectStatistics(projectId, total, critical, max, avg,
                p.getCriticalRpnThreshold());
    }

    // ---------- helpers ----------

    FmeaProject loadProjectForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        FmeaProject p = projectRepo.findById(id).orElseThrow(() -> new FmeaProjectNotFoundException(id));
        if (!p.getTenantId().equals(tenantId)) throw new FmeaProjectNotFoundException(id);
        return p;
    }

    private FmeaDto.ProjectResponse toResponse(FmeaProject p) {
        return new FmeaDto.ProjectResponse(
                p.getId(), p.getTenantId(), p.getCode(), p.getName(), p.getScope(),
                p.getType(), p.getStatus(), p.getCriticalRpnThreshold(), p.getRevision(),
                p.getOwnerUserId(), p.getLastReviewedAt(),
                p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private FmeaDto.ItemResponse toResponse(FmeaItem i, int threshold) {
        return new FmeaDto.ItemResponse(
                i.getId(), i.getTenantId(), i.getProjectId(), i.getSequenceNo(),
                i.getFunction(), i.getFailureMode(), i.getFailureEffect(),
                i.getFailureCause(), i.getCurrentControls(),
                i.getSeverity(), i.getOccurrence(), i.getDetection(), i.getRpn(),
                i.getRecommendedAction(), i.getActionOwnerUserId(), i.getActionDueDate(),
                i.getResultingSeverity(), i.getResultingOccurrence(), i.getResultingDetection(),
                i.getRpnAfter(), i.getRpn() >= threshold,
                i.getCreatedAt(), i.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
