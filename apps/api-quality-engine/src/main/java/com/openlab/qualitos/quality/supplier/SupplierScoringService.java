package com.openlab.qualitos.quality.supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Calcule le score qualité fournisseur (0..100). Centralisé pour pouvoir évoluer
 * vers un modèle IA (XGBoost cf. CLAUDE.md §6.5) sans toucher aux callers.
 *
 * Formule V1 (déterministe, expliquable) :
 *   base = 100
 *   - 15 par NC CRITICAL ouverte (status ≠ RESOLVED)
 *   - 7  par NC MAJOR ouverte
 *   - 2  par NC MINOR ouverte
 *   - 1  par NC RESOLVED dans les 12 derniers mois (cicatrice récente)
 *   - 8  par certificat expiré
 *   - 5  si aucun audit ou dernier audit > 18 mois (équivalence audit obsolète)
 *   clamp [0, 100]
 *
 * Le résultat est un entier — facile à logger, comparer, indexer.
 */
@Service
public class SupplierScoringService {

    static final int W_NC_OPEN_CRITICAL = 15;
    static final int W_NC_OPEN_MAJOR = 7;
    static final int W_NC_OPEN_MINOR = 2;
    static final int W_NC_RECENT_RESOLVED = 1;
    static final int W_CERT_EXPIRED = 8;
    static final int W_AUDIT_OBSOLETE = 5;
    static final int AUDIT_OBSOLETE_MONTHS = 18;
    static final int RECENT_NC_MONTHS = 12;

    private final SupplierRepository supplierRepo;
    private final SupplierNonConformityRepository ncRepo;
    private final SupplierCertificateRepository certRepo;
    private final SupplierAuditRecordRepository auditRepo;
    private final Clock clock;

    public SupplierScoringService(SupplierRepository supplierRepo,
                                  SupplierNonConformityRepository ncRepo,
                                  SupplierCertificateRepository certRepo,
                                  SupplierAuditRecordRepository auditRepo) {
        this(supplierRepo, ncRepo, certRepo, auditRepo, Clock.systemUTC());
    }

    SupplierScoringService(SupplierRepository supplierRepo,
                           SupplierNonConformityRepository ncRepo,
                           SupplierCertificateRepository certRepo,
                           SupplierAuditRecordRepository auditRepo,
                           Clock clock) {
        this.supplierRepo = supplierRepo;
        this.ncRepo = ncRepo;
        this.certRepo = certRepo;
        this.auditRepo = auditRepo;
        this.clock = clock;
    }

    @Transactional
    public int recompute(Supplier supplier) {
        UUID id = supplier.getId();
        LocalDate today = LocalDate.now(clock);

        long openCritical = ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.CRITICAL, NonConformityStatus.OPEN)
                + ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.CRITICAL, NonConformityStatus.IN_REVIEW);
        long openMajor = ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.MAJOR, NonConformityStatus.OPEN)
                + ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.MAJOR, NonConformityStatus.IN_REVIEW);
        long openMinor = ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.MINOR, NonConformityStatus.OPEN)
                + ncRepo.countBySupplierIdAndSeverityAndStatus(
                id, NonConformitySeverity.MINOR, NonConformityStatus.IN_REVIEW);

        long recentResolved = ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                id, NonConformityStatus.RESOLVED, today.minusMonths(RECENT_NC_MONTHS));

        long expiredCerts = certRepo.countBySupplierIdAndExpiresOnBefore(id, today);

        LocalDate latestAudit = auditRepo.findLatestAuditDate(id).orElse(null);
        boolean auditObsolete = latestAudit == null
                || latestAudit.isBefore(today.minusMonths(AUDIT_OBSOLETE_MONTHS));

        int score = 100
                - (int) (openCritical * W_NC_OPEN_CRITICAL)
                - (int) (openMajor * W_NC_OPEN_MAJOR)
                - (int) (openMinor * W_NC_OPEN_MINOR)
                - (int) (recentResolved * W_NC_RECENT_RESOLVED)
                - (int) (expiredCerts * W_CERT_EXPIRED)
                - (auditObsolete ? W_AUDIT_OBSOLETE : 0);

        score = Math.max(0, Math.min(100, score));

        supplier.setScore(score);
        supplier.setLastAuditAt(latestAudit);
        supplierRepo.save(supplier);
        return score;
    }
}
