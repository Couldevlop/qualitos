package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Module Non-Conformités (§4.3) : saisie terrain, workflow de traitement et
 * escalade vers une CAPA. Multi-tenant strict (tenant issu du JWT, jamais du body).
 */
@Service
@Transactional
public class NcService {

    private final NonConformityRepository repository;
    private final CapaCaseRepository capaCaseRepository;

    public NcService(NonConformityRepository repository, CapaCaseRepository capaCaseRepository) {
        this.repository = repository;
        this.capaCaseRepository = capaCaseRepository;
    }

    @Transactional(readOnly = true)
    public Page<NcDto.Response> findAll(NcStatus status, NcSeverity severity, NcCategory category,
                                        Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<NonConformity> page;
        if (status != null && severity != null && category != null) {
            page = repository.findByTenantIdAndStatusAndSeverityAndCategory(tenantId, status, severity, category, pageable);
        } else if (status != null && severity != null) {
            page = repository.findByTenantIdAndStatusAndSeverity(tenantId, status, severity, pageable);
        } else if (status != null && category != null) {
            page = repository.findByTenantIdAndStatusAndCategory(tenantId, status, category, pageable);
        } else if (severity != null && category != null) {
            page = repository.findByTenantIdAndSeverityAndCategory(tenantId, severity, category, pageable);
        } else if (status != null) {
            page = repository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (severity != null) {
            page = repository.findByTenantIdAndSeverity(tenantId, severity, pageable);
        } else if (category != null) {
            page = repository.findByTenantIdAndCategory(tenantId, category, pageable);
        } else {
            page = repository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NcDto.Response findById(UUID id) {
        return toResponse(load(id));
    }

    /** Nombre de tentatives sur collision de référence concurrente (BUG #4). */
    private static final int REFERENCE_RETRY_ATTEMPTS = 3;

    public NcDto.Response create(NcDto.CreateRequest request) {
        UUID tenantId = requireTenantId();
        // H2 (OWASP A01) : le rapporteur est dérivé du JWT (sub), jamais d'un champ de
        // body falsifiable. On retombe sur le body uniquement hors contexte authentifié
        // (tests unitaires de service) ; en production le principal est toujours présent.
        UUID reporterId = CurrentUser.userId().orElse(request.reporterId());

        NonConformity nc = new NonConformity();
        nc.setTenantId(tenantId);
        nc.setTitle(request.title());
        nc.setDescription(request.description());
        nc.setCategory(request.category());
        nc.setSeverity(request.severity());
        nc.setStatus(NcStatus.OPEN);
        nc.setDetectedAt(request.detectedAt());
        nc.setZone(request.zone());
        nc.setGeoLat(request.geoLat());
        nc.setGeoLng(request.geoLng());
        nc.setPhotoUrls(request.photoUrls());
        nc.setReporterId(reporterId);

        // BUG #4 — Race sur la génération de référence : deux créations concurrentes
        // peuvent calculer la même référence et violer la contrainte d'unicité
        // (tenant_id, reference). On retente quelques fois en régénérant la référence ;
        // au-delà on laisse remonter → 409 via le GlobalExceptionHandler.
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < REFERENCE_RETRY_ATTEMPTS; attempt++) {
            nc.setReference(generateReference(tenantId));
            try {
                return toResponse(repository.saveAndFlush(nc));
            } catch (DataIntegrityViolationException ex) {
                last = ex;
                // Référence reprise entre temps : on régénère et on retente.
            }
        }
        throw last;
    }

    public NcDto.Response update(UUID id, NcDto.UpdateRequest request) {
        NonConformity nc = load(id);
        if (nc.getStatus() == NcStatus.CLOSED || nc.getStatus() == NcStatus.CANCELLED) {
            throw new NcStateException("Cannot modify a " + nc.getStatus() + " non-conformity");
        }
        if (request.title() != null) nc.setTitle(request.title());
        if (request.description() != null) nc.setDescription(request.description());
        if (request.category() != null) nc.setCategory(request.category());
        if (request.severity() != null) nc.setSeverity(request.severity());
        if (request.zone() != null) nc.setZone(request.zone());
        if (request.geoLat() != null) nc.setGeoLat(request.geoLat());
        if (request.geoLng() != null) nc.setGeoLng(request.geoLng());
        if (request.photoUrls() != null) nc.setPhotoUrls(request.photoUrls());
        return toResponse(repository.save(nc));
    }

    // --- workflow ---

    public NcDto.Response startAnalysis(UUID id, NcDto.StartAnalysisRequest request) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.OPEN) {
            throw new NcStateException("Only OPEN non-conformities can move to analysis");
        }
        nc.setStatus(NcStatus.UNDER_ANALYSIS);
        if (request != null && request.rootCause() != null) {
            nc.setRootCause(request.rootCause());
        }
        return toResponse(repository.save(nc));
    }

    public NcDto.Response defineAction(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.UNDER_ANALYSIS) {
            throw new NcStateException("Action can only be defined from UNDER_ANALYSIS");
        }
        nc.setStatus(NcStatus.ACTION_DEFINED);
        return toResponse(repository.save(nc));
    }

    public NcDto.Response resolve(UUID id, NcDto.ResolveRequest request) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.ACTION_DEFINED) {
            throw new NcStateException("Only ACTION_DEFINED non-conformities can be resolved");
        }
        nc.setStatus(NcStatus.RESOLVED);
        nc.setResolutionNote(request.resolutionNote());
        nc.setResolvedAt(Instant.now());
        return toResponse(repository.save(nc));
    }

    public NcDto.Response close(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.RESOLVED) {
            throw new NcStateException("Only RESOLVED non-conformities can be closed");
        }
        nc.setStatus(NcStatus.CLOSED);
        nc.setClosedAt(Instant.now());
        return toResponse(repository.save(nc));
    }

    public NcDto.Response cancel(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.OPEN && nc.getStatus() != NcStatus.UNDER_ANALYSIS) {
            throw new NcStateException("Only OPEN or UNDER_ANALYSIS non-conformities can be cancelled");
        }
        nc.setStatus(NcStatus.CANCELLED);
        return toResponse(repository.save(nc));
    }

    /**
     * Escalade la NC en CAPA (§4.3) : crée un CapaCase lié (sourceType = NON_CONFORMITY,
     * sourceRef = référence NC), pose capa_case_id. Ne change pas le statut de la NC.
     * Idempotent : une 2e escalade est refusée.
     */
    public NcDto.Response escalateToCapa(UUID id, NcDto.EscalateRequest request) {
        UUID tenantId = requireTenantId();
        NonConformity nc = load(id);
        if (nc.getCapaCaseId() != null) {
            throw new NcStateException("Non-conformity already escalated to CAPA " + nc.getCapaCaseId());
        }
        CapaCase capa = new CapaCase();
        capa.setTenantId(tenantId);
        capa.setTitle(nc.getTitle());
        capa.setDescription(nc.getDescription());
        capa.setType(CapaType.CORRECTIVE);
        capa.setCriticity(mapCriticity(nc.getSeverity()));
        capa.setStatus(CapaStatus.OPEN);
        capa.setSourceType(CapaSourceType.NON_CONFORMITY);
        capa.setSourceRef(nc.getReference());
        capa.setOwnerId(request.ownerId());
        CapaCase saved = capaCaseRepository.save(capa);
        nc.setCapaCaseId(saved.getId());
        return toResponse(repository.save(nc));
    }

    // --- helpers ---

    private static CapaCriticity mapCriticity(NcSeverity severity) {
        return switch (severity) {
            case MINOR -> CapaCriticity.LOW;
            case MAJOR -> CapaCriticity.HIGH;
            case CRITICAL -> CapaCriticity.CRITICAL;
        };
    }

    /** NC-{année}-{séquence par tenant}, avec garde anti-collision. */
    private String generateReference(UUID tenantId) {
        String prefix = "NC-" + Instant.now().atZone(ZoneOffset.UTC).getYear() + "-";
        long next = repository.countByTenantIdAndReferenceStartingWith(tenantId, prefix) + 1;
        String reference;
        do {
            reference = prefix + String.format("%04d", next);
            next++;
        } while (repository.existsByTenantIdAndReference(tenantId, reference));
        return reference;
    }

    private NonConformity load(UUID id) {
        UUID tenantId = requireTenantId();
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NcNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private NcDto.Response toResponse(NonConformity nc) {
        return new NcDto.Response(
                nc.getId(), nc.getTenantId(), nc.getReference(), nc.getTitle(), nc.getDescription(),
                nc.getCategory(), nc.getSeverity(), nc.getStatus(), nc.getDetectedAt(),
                nc.getZone(), nc.getGeoLat(), nc.getGeoLng(), nc.getPhotoUrls(),
                nc.getReporterId(), nc.getCapaCaseId(), nc.getRootCause(), nc.getResolutionNote(),
                nc.getResolvedAt(), nc.getClosedAt(), nc.getCreatedAt(), nc.getUpdatedAt());
    }
}
