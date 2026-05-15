package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service orchestrant les fournisseurs et leurs sous-entités.
 *
 * Transitions de statut (toute autre transition = 409) :
 *   PROSPECT       → APPROVED | BLACKLISTED
 *   APPROVED       → CONDITIONAL | SUSPENDED | BLACKLISTED
 *   CONDITIONAL    → APPROVED | SUSPENDED | BLACKLISTED
 *   SUSPENDED      → APPROVED | CONDITIONAL | BLACKLISTED
 *   BLACKLISTED    → ∅ (terminal)
 *
 * Toute mutation de NC, audit, certificat déclenche {@link SupplierScoringService}
 * pour garder le score à jour sans intervention manuelle.
 */
@Service
public class SupplierService {

    private static final Map<SupplierStatus, Set<SupplierStatus>> ALLOWED_TRANSITIONS;
    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(SupplierStatus.class);
        ALLOWED_TRANSITIONS.put(SupplierStatus.PROSPECT, EnumSet.of(
                SupplierStatus.APPROVED, SupplierStatus.BLACKLISTED));
        ALLOWED_TRANSITIONS.put(SupplierStatus.APPROVED, EnumSet.of(
                SupplierStatus.CONDITIONAL, SupplierStatus.SUSPENDED, SupplierStatus.BLACKLISTED));
        ALLOWED_TRANSITIONS.put(SupplierStatus.CONDITIONAL, EnumSet.of(
                SupplierStatus.APPROVED, SupplierStatus.SUSPENDED, SupplierStatus.BLACKLISTED));
        ALLOWED_TRANSITIONS.put(SupplierStatus.SUSPENDED, EnumSet.of(
                SupplierStatus.APPROVED, SupplierStatus.CONDITIONAL, SupplierStatus.BLACKLISTED));
        ALLOWED_TRANSITIONS.put(SupplierStatus.BLACKLISTED, EnumSet.noneOf(SupplierStatus.class));
    }

    private final SupplierRepository supplierRepo;
    private final SupplierAuditRecordRepository auditRepo;
    private final SupplierNonConformityRepository ncRepo;
    private final SupplierCertificateRepository certRepo;
    private final SupplierScoringService scoringService;
    private final Clock clock;

    public SupplierService(SupplierRepository supplierRepo,
                           SupplierAuditRecordRepository auditRepo,
                           SupplierNonConformityRepository ncRepo,
                           SupplierCertificateRepository certRepo,
                           SupplierScoringService scoringService) {
        this(supplierRepo, auditRepo, ncRepo, certRepo, scoringService, Clock.systemUTC());
    }

    SupplierService(SupplierRepository supplierRepo,
                    SupplierAuditRecordRepository auditRepo,
                    SupplierNonConformityRepository ncRepo,
                    SupplierCertificateRepository certRepo,
                    SupplierScoringService scoringService,
                    Clock clock) {
        this.supplierRepo = supplierRepo;
        this.auditRepo = auditRepo;
        this.ncRepo = ncRepo;
        this.certRepo = certRepo;
        this.scoringService = scoringService;
        this.clock = clock;
    }

    // ---------- Suppliers ----------

    @Transactional
    public SupplierDto.SupplierResponse create(SupplierDto.CreateSupplierRequest req) {
        UUID tenantId = requireTenantId();
        supplierRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(s -> {
            throw new SupplierStateException("Supplier code already exists: " + req.code());
        });
        Supplier s = new Supplier();
        s.setTenantId(tenantId);
        s.setCode(req.code());
        s.setName(req.name());
        s.setCountryCode(req.countryCode());
        s.setContactEmail(req.contactEmail());
        s.setSupplierType(req.supplierType());
        s.setStatus(SupplierStatus.PROSPECT);
        s.setScore(100);
        s.setCreatedBy(req.createdBy());
        return toResponse(supplierRepo.save(s));
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.SupplierResponse> list(SupplierStatus status, SupplierType type, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Supplier> page;
        if (status != null) page = supplierRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (type != null) page = supplierRepo.findByTenantIdAndSupplierType(tenantId, type, pageable);
        else page = supplierRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierDto.SupplierResponse get(UUID id) { return toResponse(loadForTenant(id)); }

    @Transactional
    public SupplierDto.SupplierResponse update(UUID id, SupplierDto.UpdateSupplierRequest req) {
        Supplier s = loadForTenant(id);
        if (s.getStatus() == SupplierStatus.BLACKLISTED) {
            throw new SupplierStateException("Cannot edit a BLACKLISTED supplier");
        }
        if (req.name() != null) s.setName(req.name());
        if (req.countryCode() != null) s.setCountryCode(req.countryCode());
        if (req.contactEmail() != null) s.setContactEmail(req.contactEmail());
        if (req.supplierType() != null) s.setSupplierType(req.supplierType());
        return toResponse(supplierRepo.save(s));
    }

    @Transactional
    public void delete(UUID id) {
        Supplier s = loadForTenant(id);
        // Nettoyage explicite des enfants (ON DELETE CASCADE en DB mais on garde la
        // symétrie côté JPA — Hibernate ne suit pas la cascade FK SQL).
        auditRepo.deleteBySupplierId(id);
        ncRepo.deleteBySupplierId(id);
        certRepo.deleteBySupplierId(id);
        supplierRepo.delete(s);
    }

    @Transactional
    public SupplierDto.SupplierResponse changeStatus(UUID id, SupplierStatus target,
                                                     SupplierDto.StatusChangeRequest req) {
        Supplier s = loadForTenant(id);
        if (target == null) throw new SupplierStateException("Target status is required");
        Set<SupplierStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(s.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new SupplierStateException(
                    "Transition " + s.getStatus() + " → " + target + " is not allowed");
        }
        s.setStatus(target);
        if (target == SupplierStatus.APPROVED && s.getApprovedAt() == null) {
            s.setApprovedAt(Instant.now(clock));
            s.setApprovedBy(req.actorUserId());
        }
        return toResponse(supplierRepo.save(s));
    }

    // ---------- Audits ----------

    @Transactional
    public SupplierDto.AuditResponse addAudit(UUID supplierId, SupplierDto.CreateAuditRequest req) {
        Supplier s = loadForTenant(supplierId);
        SupplierAuditRecord a = new SupplierAuditRecord();
        a.setTenantId(s.getTenantId());
        a.setSupplierId(supplierId);
        a.setAuditedOn(req.auditedOn());
        a.setScore(req.score());
        a.setAuditorUserId(req.auditorUserId());
        a.setFindingsSummary(req.findingsSummary());
        a.setCriticalFindingsCount(opt(req.criticalFindingsCount()));
        a.setMajorFindingsCount(opt(req.majorFindingsCount()));
        a.setMinorFindingsCount(opt(req.minorFindingsCount()));
        SupplierAuditRecord saved = auditRepo.save(a);
        scoringService.recompute(s);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.AuditResponse> listAudits(UUID supplierId, Pageable pageable) {
        loadForTenant(supplierId);
        return auditRepo.findBySupplierIdOrderByAuditedOnDesc(supplierId, pageable)
                .map(this::toResponse);
    }

    // ---------- Non-conformities ----------

    @Transactional
    public SupplierDto.NonConformityResponse addNonConformity(
            UUID supplierId, SupplierDto.CreateNonConformityRequest req) {
        Supplier s = loadForTenant(supplierId);
        SupplierNonConformity nc = new SupplierNonConformity();
        nc.setTenantId(s.getTenantId());
        nc.setSupplierId(supplierId);
        nc.setLotReference(req.lotReference());
        nc.setDescription(req.description());
        nc.setSeverity(req.severity());
        nc.setDetectedOn(req.detectedOn());
        nc.setStatus(NonConformityStatus.OPEN);
        SupplierNonConformity saved = ncRepo.save(nc);
        scoringService.recompute(s);
        return toResponse(saved);
    }

    @Transactional
    public SupplierDto.NonConformityResponse updateNonConformity(
            UUID supplierId, UUID ncId, SupplierDto.UpdateNonConformityRequest req) {
        Supplier s = loadForTenant(supplierId);
        SupplierNonConformity nc = ncRepo.findById(ncId)
                .orElseThrow(() -> new SupplierChildNotFoundException("Non-conformity", ncId));
        if (!nc.getSupplierId().equals(supplierId) || !nc.getTenantId().equals(s.getTenantId())) {
            throw new SupplierChildNotFoundException("Non-conformity", ncId);
        }
        if (req.lotReference() != null) nc.setLotReference(req.lotReference());
        if (req.description() != null) nc.setDescription(req.description());
        if (req.severity() != null) nc.setSeverity(req.severity());
        if (req.status() != null) {
            nc.setStatus(req.status());
            if (req.status() == NonConformityStatus.RESOLVED && nc.getResolvedOn() == null) {
                nc.setResolvedOn(LocalDate.now(clock));
            }
        }
        if (req.resolvedOn() != null) nc.setResolvedOn(req.resolvedOn());
        if (req.resolution() != null) nc.setResolution(req.resolution());
        SupplierNonConformity saved = ncRepo.save(nc);
        scoringService.recompute(s);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.NonConformityResponse> listNonConformities(
            UUID supplierId, Pageable pageable) {
        loadForTenant(supplierId);
        return ncRepo.findBySupplierIdOrderByDetectedOnDesc(supplierId, pageable)
                .map(this::toResponse);
    }

    // ---------- Certificates ----------

    @Transactional
    public SupplierDto.CertificateResponse addCertificate(
            UUID supplierId, SupplierDto.CreateCertificateRequest req) {
        Supplier s = loadForTenant(supplierId);
        if (!req.expiresOn().isAfter(req.issuedOn())) {
            throw new SupplierStateException("expiresOn must be strictly after issuedOn");
        }
        SupplierCertificate c = new SupplierCertificate();
        c.setTenantId(s.getTenantId());
        c.setSupplierId(supplierId);
        c.setStandardCode(req.standardCode());
        c.setReference(req.reference());
        c.setIssuedOn(req.issuedOn());
        c.setExpiresOn(req.expiresOn());
        c.setDocumentUrl(req.documentUrl());
        SupplierCertificate saved = certRepo.save(c);
        scoringService.recompute(s);
        return toResponse(saved);
    }

    @Transactional
    public void deleteCertificate(UUID supplierId, UUID certId) {
        Supplier s = loadForTenant(supplierId);
        SupplierCertificate c = certRepo.findById(certId)
                .orElseThrow(() -> new SupplierChildNotFoundException("Certificate", certId));
        if (!c.getSupplierId().equals(supplierId) || !c.getTenantId().equals(s.getTenantId())) {
            throw new SupplierChildNotFoundException("Certificate", certId);
        }
        certRepo.delete(c);
        scoringService.recompute(s);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.CertificateResponse> listCertificates(UUID supplierId, Pageable pageable) {
        loadForTenant(supplierId);
        return certRepo.findBySupplierIdOrderByExpiresOnAsc(supplierId, pageable)
                .map(this::toResponse);
    }

    // ---------- Statistics ----------

    @Transactional(readOnly = true)
    public SupplierDto.SupplierStatistics statistics(UUID supplierId) {
        Supplier s = loadForTenant(supplierId);
        LocalDate today = LocalDate.now(clock);
        long openNc = ncRepo.countBySupplierIdAndStatus(supplierId, NonConformityStatus.OPEN)
                + ncRepo.countBySupplierIdAndStatus(supplierId, NonConformityStatus.IN_REVIEW);
        long resolvedRecent = ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                supplierId, NonConformityStatus.RESOLVED, today.minusMonths(SupplierScoringService.RECENT_NC_MONTHS));
        long expired = certRepo.countBySupplierIdAndExpiresOnBefore(supplierId, today);
        Optional<LocalDate> latestAudit = auditRepo.findLatestAuditDate(supplierId);
        return new SupplierDto.SupplierStatistics(
                supplierId, s.getScore(), s.getStatus(),
                openNc, resolvedRecent, expired, latestAudit.orElse(null));
    }

    // ---------- helpers ----------

    Supplier loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        Supplier s = supplierRepo.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        if (!s.getTenantId().equals(tenantId)) throw new SupplierNotFoundException(id);
        return s;
    }

    private static int opt(Integer v) { return v == null ? 0 : v; }

    private SupplierDto.SupplierResponse toResponse(Supplier s) {
        return new SupplierDto.SupplierResponse(
                s.getId(), s.getTenantId(), s.getCode(), s.getName(),
                s.getCountryCode(), s.getContactEmail(), s.getSupplierType(),
                s.getStatus(), s.getScore(), s.getLastAuditAt(),
                s.getApprovedAt(), s.getApprovedBy(),
                s.getCreatedBy(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private SupplierDto.AuditResponse toResponse(SupplierAuditRecord a) {
        return new SupplierDto.AuditResponse(
                a.getId(), a.getTenantId(), a.getSupplierId(),
                a.getAuditedOn(), a.getScore(), a.getAuditorUserId(),
                a.getFindingsSummary(), a.getCriticalFindingsCount(),
                a.getMajorFindingsCount(), a.getMinorFindingsCount(),
                a.getCreatedAt());
    }

    private SupplierDto.NonConformityResponse toResponse(SupplierNonConformity nc) {
        return new SupplierDto.NonConformityResponse(
                nc.getId(), nc.getTenantId(), nc.getSupplierId(),
                nc.getLotReference(), nc.getDescription(),
                nc.getSeverity(), nc.getStatus(),
                nc.getDetectedOn(), nc.getResolvedOn(), nc.getResolution(),
                nc.getCreatedAt(), nc.getUpdatedAt());
    }

    private SupplierDto.CertificateResponse toResponse(SupplierCertificate c) {
        return new SupplierDto.CertificateResponse(
                c.getId(), c.getTenantId(), c.getSupplierId(),
                c.getStandardCode(), c.getReference(),
                c.getIssuedOn(), c.getExpiresOn(), c.getDocumentUrl(),
                c.isExpiredAt(LocalDate.now(clock)),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
