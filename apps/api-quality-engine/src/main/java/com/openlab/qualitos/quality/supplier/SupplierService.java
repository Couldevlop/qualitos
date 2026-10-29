package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SupplierDocumentService documentService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SupplierService(SupplierRepository supplierRepo,
                           SupplierAuditRecordRepository auditRepo,
                           SupplierNonConformityRepository ncRepo,
                           SupplierCertificateRepository certRepo,
                           SupplierScoringService scoringService,
                           SupplierDocumentService documentService) {
        this(supplierRepo, auditRepo, ncRepo, certRepo, scoringService, documentService, Clock.systemUTC());
    }

    SupplierService(SupplierRepository supplierRepo,
                    SupplierAuditRecordRepository auditRepo,
                    SupplierNonConformityRepository ncRepo,
                    SupplierCertificateRepository certRepo,
                    SupplierScoringService scoringService,
                    SupplierDocumentService documentService,
                    Clock clock) {
        this.supplierRepo = supplierRepo;
        this.auditRepo = auditRepo;
        this.ncRepo = ncRepo;
        this.certRepo = certRepo;
        this.scoringService = scoringService;
        this.documentService = documentService;
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
        // Les documents d'abord : leurs binaires ne partiraient pas avec la cascade SQL.
        documentService.purgeSupplier(s.getTenantId(), id);
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
        nc.setPartNumber(req.partNumber().strip());
        nc.setPartQuantity(req.partQuantity());
        nc.setEightDRequested(Boolean.TRUE.equals(req.eightDRequested()));
        nc.setEightDReceived(Boolean.TRUE.equals(req.eightDReceived()));
        NonConformityStatus status = req.status() == null ? NonConformityStatus.OPEN : req.status();
        nc.setStatus(status);
        if (status == NonConformityStatus.RESOLVED) nc.setResolvedOn(LocalDate.now(clock));
        SupplierNonConformity saved = ncRepo.save(nc);
        scoringService.recompute(s);
        return toResponse(saved, s, 0);
    }

    @Transactional
    public SupplierDto.NonConformityResponse updateNonConformity(
            UUID supplierId, UUID ncId, SupplierDto.UpdateNonConformityRequest req) {
        Supplier s = loadForTenant(supplierId);
        SupplierNonConformity nc = loadNonConformity(s, ncId);
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
        if (req.partNumber() != null) nc.setPartNumber(req.partNumber().strip());
        if (req.partQuantity() != null) nc.setPartQuantity(req.partQuantity());
        if (req.eightDRequested() != null) nc.setEightDRequested(req.eightDRequested());
        if (req.eightDReceived() != null) nc.setEightDReceived(req.eightDReceived());
        SupplierNonConformity saved = ncRepo.save(nc);
        scoringService.recompute(s);
        return toResponse(saved, s, documentCounts(s.getTenantId(), List.of(saved)).getOrDefault(saved.getId(), 0L));
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.NonConformityResponse> listNonConformities(
            UUID supplierId, Pageable pageable) {
        Supplier s = loadForTenant(supplierId);
        Page<SupplierNonConformity> page = ncRepo.findBySupplierIdOrderByDetectedOnDesc(supplierId, pageable);
        Map<UUID, Long> counts = documentCounts(s.getTenantId(), page.getContent());
        return page.map(nc -> toResponse(nc, s, counts.getOrDefault(nc.getId(), 0L)));
    }

    /**
     * Les NC de tous les fournisseurs du tenant, les plus récentes d'abord, avec
     * des filtres facultatifs. Le fournisseur de chaque ligne est chargé en une
     * requête pour toute la page, comme le nombre de documents.
     */
    @Transactional(readOnly = true)
    public Page<SupplierDto.NonConformityResponse> listAllNonConformities(
            UUID supplierId, NonConformityStatus status, NonConformitySeverity severity, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Specification<SupplierNonConformity> filters = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (supplierId != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        }
        if (status != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (severity != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        Page<SupplierNonConformity> page = ncRepo.findAll(filters, withDefaultSort(pageable));

        Map<UUID, Supplier> owners = supplierRepo.findAllById(
                        page.getContent().stream().map(SupplierNonConformity::getSupplierId).distinct().toList())
                .stream()
                .filter(s -> s.getTenantId().equals(tenantId))
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        Map<UUID, Long> counts = documentCounts(tenantId, page.getContent());
        return page.map(nc -> toResponse(nc, owners.get(nc.getSupplierId()), counts.getOrDefault(nc.getId(), 0L)));
    }

    /** Une NC de ce fournisseur, ou 404 — y compris quand elle appartient à un autre. */
    public SupplierNonConformity loadNonConformity(Supplier s, UUID ncId) {
        SupplierNonConformity nc = ncRepo.findById(ncId)
                .orElseThrow(() -> new SupplierChildNotFoundException("Non-conformity", ncId));
        if (!nc.getSupplierId().equals(s.getId()) || !nc.getTenantId().equals(s.getTenantId())) {
            throw new SupplierChildNotFoundException("Non-conformity", ncId);
        }
        return nc;
    }

    /** Sans tri demandé, les NC les plus récentes d'abord : c'est la page qu'on vient chercher. */
    private static Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted() || pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("detectedOn"), Sort.Order.desc("createdAt")));
    }

    private Map<UUID, Long> documentCounts(UUID tenantId, List<SupplierNonConformity> page) {
        return documentService.countByOwners(tenantId, SupplierDocumentOwnerType.NON_CONFORMITY,
                page.stream().map(SupplierNonConformity::getId).toList());
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
        return toResponse(saved, 0);
    }

    @Transactional
    public void deleteCertificate(UUID supplierId, UUID certId) {
        Supplier s = loadForTenant(supplierId);
        SupplierCertificate c = certRepo.findById(certId)
                .orElseThrow(() -> new SupplierChildNotFoundException("Certificate", certId));
        if (!c.getSupplierId().equals(supplierId) || !c.getTenantId().equals(s.getTenantId())) {
            throw new SupplierChildNotFoundException("Certificate", certId);
        }
        // Ses documents partent avec lui : rien ne les rattacherait plus à rien.
        documentService.purgeOwner(s.getTenantId(), SupplierDocumentOwnerType.CERTIFICATE, certId);
        certRepo.delete(c);
        scoringService.recompute(s);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto.CertificateResponse> listCertificates(UUID supplierId, Pageable pageable) {
        Supplier s = loadForTenant(supplierId);
        Page<SupplierCertificate> page = certRepo.findBySupplierIdOrderByExpiresOnAsc(supplierId, pageable);
        Map<UUID, Long> counts = documentService.countByOwners(s.getTenantId(), SupplierDocumentOwnerType.CERTIFICATE,
                page.getContent().stream().map(SupplierCertificate::getId).toList());
        return page.map(c -> toResponse(c, counts.getOrDefault(c.getId(), 0L)));
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

    public Supplier loadForTenant(UUID id) {
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

    private SupplierDto.NonConformityResponse toResponse(SupplierNonConformity nc, Supplier owner,
                                                         long documentCount) {
        return new SupplierDto.NonConformityResponse(
                nc.getId(), nc.getTenantId(), nc.getSupplierId(),
                owner == null ? null : owner.getCode(),
                owner == null ? null : owner.getName(),
                nc.getLotReference(), nc.getDescription(),
                nc.getSeverity(), nc.getStatus(),
                nc.getDetectedOn(), nc.getResolvedOn(), nc.getResolution(),
                nc.getPartNumber(), nc.getPartQuantity(),
                nc.isEightDRequested(), nc.isEightDReceived(),
                documentCount,
                nc.getCreatedAt(), nc.getUpdatedAt());
    }

    private SupplierDto.CertificateResponse toResponse(SupplierCertificate c, long documentCount) {
        return new SupplierDto.CertificateResponse(
                c.getId(), c.getTenantId(), c.getSupplierId(),
                c.getStandardCode(), c.getReference(),
                c.getIssuedOn(), c.getExpiresOn(), c.getDocumentUrl(),
                c.isExpiredAt(LocalDate.now(clock)),
                documentCount,
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
