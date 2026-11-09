package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.circle.CircleDto;
import com.openlab.qualitos.quality.circle.CircleService;
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
    /** Le module des cercles : c'est lui qui tient les idees, pas le 5S. */
    private final CircleService circleService;

    public FiveSService(FiveSAuditRepository auditRepository, FiveSAuditItemRepository itemRepository,
                        CircleService circleService) {
        this.auditRepository = auditRepository;
        this.itemRepository = itemRepository;
        this.circleService = circleService;
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

    /**
     * Porter un pilier de cet audit a un cercle de qualite — ce que le
     * referentiel transverse promet : « un audit 5S peut alimenter un cercle de
     * qualite » (§3.6).
     *
     * <p>Un audit dit ce qui ne va pas ; il ne dit pas comment y remedier. Le
     * cercle, lui, est fait pour ca : dix personnes du terrain qui cherchent
     * ensemble. L'idee part donc de la note du pilier, et la cite.
     *
     * <p>L'audit doit etre termine : une note encore en cours de saisie se
     * corrige, et une idee defendue en seance sur un chiffre qui a bouge depuis
     * ne tient pas. Le pilier doit avoir ete note, pour la meme raison.
     *
     * <p>L'origine est construite ici, pas demandee : un « vient de » ecrit par
     * l'appelant rattacherait une idee a n'importe quel audit (§18.2).
     */
    public CircleDto.ProposalResponse proposeToCircle(UUID auditId,
                                                      FiveSDto.ProposeToCircleRequest request) {
        FiveSAudit audit = loadAudit(auditId);
        if (audit.getStatus() != FiveSAuditStatus.COMPLETED) {
            throw new FiveSStateException(
                    "Only a COMPLETED audit can feed a quality circle (status is " + audit.getStatus() + ")");
        }
        FiveSAuditItem item = itemRepository.findByAuditIdAndPillar(auditId, request.pillar())
                .filter(it -> it.getScore() != null)
                .orElseThrow(() -> new FiveSStateException(
                        "Pillar " + request.pillar() + " has no score on this audit"));

        return circleService.addProposal(request.circleId(),
                new CircleDto.ProposalRequest(request.title(), request.description(), null),
                origine(audit, item));
    }

    /**
     * L'origine citee : l'audit, sa zone, le pilier et sa note.
     *
     * <p>Tenue sous 255 caracteres, la longueur de la colonne : une zone au nom
     * fleuve ne doit pas faire echouer l'ecriture de l'idee.
     */
    private static String origine(FiveSAudit audit, FiveSAuditItem item) {
        String zone = audit.getZone() == null ? "" : audit.getZone().trim();
        if (zone.length() > 120) {
            zone = zone.substring(0, 119).trim() + "\u2026";
        }
        StringBuilder sb = new StringBuilder("Audit 5S zone ").append(zone)
                .append(" \u2014 ").append(item.getPillar())
                .append(" not\u00e9 ").append(item.getScore()).append("/10");
        if (audit.getCompletedAt() != null) {
            sb.append(" le ").append(audit.getCompletedAt().toString(), 0, 10);
        }
        String ref = sb.toString();
        return ref.length() > 255 ? ref.substring(0, 254) + "\u2026" : ref;
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
