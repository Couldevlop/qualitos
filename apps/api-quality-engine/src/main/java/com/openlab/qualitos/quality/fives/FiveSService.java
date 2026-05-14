package com.openlab.qualitos.quality.fives;

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
public class FiveSService {

    private final FiveSAuditRepository auditRepository;
    private final FiveSAuditItemRepository itemRepository;

    public FiveSService(FiveSAuditRepository auditRepository, FiveSAuditItemRepository itemRepository) {
        this.auditRepository = auditRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<FiveSDto.AuditResponse> findAll(FiveSAuditStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<FiveSAudit> page = status != null
                ? auditRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : auditRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FiveSDto.AuditResponse findById(UUID id) {
        return toResponse(loadAudit(id));
    }

    public FiveSDto.AuditResponse createAudit(FiveSDto.CreateAuditRequest request) {
        UUID tenantId = requireTenantId();
        FiveSAudit audit = new FiveSAudit();
        audit.setTenantId(tenantId);
        audit.setZone(request.zone());
        audit.setDescription(request.description());
        audit.setAuditorId(request.auditorId());
        audit.setScheduledAt(request.scheduledAt());
        audit.setStatus(FiveSAuditStatus.DRAFT);
        return toResponse(auditRepository.save(audit));
    }

    public FiveSDto.AuditResponse updateAudit(UUID id, FiveSDto.UpdateAuditRequest request) {
        FiveSAudit audit = loadAudit(id);
        if (audit.getStatus() == FiveSAuditStatus.COMPLETED
                || audit.getStatus() == FiveSAuditStatus.CANCELLED) {
            throw new FiveSStateException("Cannot modify a " + audit.getStatus() + " audit");
        }
        if (request.zone() != null) audit.setZone(request.zone());
        if (request.description() != null) audit.setDescription(request.description());
        if (request.scheduledAt() != null) audit.setScheduledAt(request.scheduledAt());
        return toResponse(auditRepository.save(audit));
    }

    public FiveSDto.AuditResponse startAudit(UUID id) {
        FiveSAudit audit = loadAudit(id);
        if (audit.getStatus() != FiveSAuditStatus.DRAFT) {
            throw new FiveSStateException("Only DRAFT audits can be started");
        }
        audit.setStatus(FiveSAuditStatus.IN_PROGRESS);
        return toResponse(auditRepository.save(audit));
    }

    public FiveSDto.AuditResponse completeAudit(UUID id) {
        FiveSAudit audit = loadAudit(id);
        if (audit.getStatus() != FiveSAuditStatus.IN_PROGRESS) {
            throw new FiveSStateException("Only IN_PROGRESS audits can be completed");
        }
        if (audit.getItems().size() != FiveSPillar.values().length) {
            throw new FiveSStateException("All 5 pillars must be scored before completion");
        }
        double avg = audit.getItems().stream().mapToInt(FiveSAuditItem::getScore).average().orElse(0d);
        audit.setOverallScore(avg * 10d); // 0..10 par pilier -> 0..100 global
        audit.setStatus(FiveSAuditStatus.COMPLETED);
        audit.setCompletedAt(Instant.now());
        return toResponse(auditRepository.save(audit));
    }

    public FiveSDto.AuditResponse cancelAudit(UUID id) {
        FiveSAudit audit = loadAudit(id);
        if (audit.getStatus() == FiveSAuditStatus.COMPLETED) {
            throw new FiveSStateException("Completed audit cannot be cancelled");
        }
        if (audit.getStatus() == FiveSAuditStatus.CANCELLED) {
            throw new FiveSStateException("Audit is already cancelled");
        }
        audit.setStatus(FiveSAuditStatus.CANCELLED);
        return toResponse(auditRepository.save(audit));
    }

    public FiveSDto.ItemResponse scorePillar(UUID auditId, FiveSDto.ScoreRequest request) {
        FiveSAudit audit = loadAudit(auditId);
        if (audit.getStatus() != FiveSAuditStatus.DRAFT
                && audit.getStatus() != FiveSAuditStatus.IN_PROGRESS) {
            throw new FiveSStateException("Cannot score a " + audit.getStatus() + " audit");
        }

        FiveSAuditItem item = itemRepository
                .findByAuditIdAndPillar(auditId, request.pillar())
                .orElseGet(() -> {
                    FiveSAuditItem it = new FiveSAuditItem();
                    it.setAudit(audit);
                    it.setPillar(request.pillar());
                    return it;
                });
        item.setScore(request.score());
        item.setNote(request.note());
        item.setPhotoUrl(request.photoUrl());

        return toItemResponse(itemRepository.save(item));
    }

    public void deleteAudit(UUID id) {
        FiveSAudit audit = loadAudit(id);
        if (audit.getStatus() == FiveSAuditStatus.COMPLETED) {
            throw new FiveSStateException("Completed audit cannot be deleted");
        }
        auditRepository.delete(audit);
    }

    private FiveSAudit loadAudit(UUID id) {
        UUID tenantId = requireTenantId();
        return auditRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FiveSAuditNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private FiveSDto.AuditResponse toResponse(FiveSAudit a) {
        return new FiveSDto.AuditResponse(
                a.getId(), a.getTenantId(), a.getZone(), a.getDescription(),
                a.getStatus(), a.getAuditorId(), a.getScheduledAt(), a.getCompletedAt(),
                a.getOverallScore(), a.getCreatedAt(), a.getUpdatedAt(),
                a.getItems().stream().map(this::toItemResponse).toList());
    }

    private FiveSDto.ItemResponse toItemResponse(FiveSAuditItem i) {
        return new FiveSDto.ItemResponse(
                i.getId(), i.getAudit().getId(), i.getPillar(), i.getScore(),
                i.getNote(), i.getPhotoUrl(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
