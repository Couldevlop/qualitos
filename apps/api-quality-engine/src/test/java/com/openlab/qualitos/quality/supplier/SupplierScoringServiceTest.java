package com.openlab.qualitos.quality.supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// La formule de scoring fait plusieurs appels distincts par appel à recompute()
// (OPEN + IN_REVIEW pour chaque sévérité, etc.). On stubbe sélectivement les
// retours non nuls dans chaque test ; les autres retournent 0L par défaut. Le
// strict stubbing trouverait ça suspect — LENIENT est la bonne politique ici.
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierScoringServiceTest {

    @Mock SupplierRepository supplierRepo;
    @Mock SupplierNonConformityRepository ncRepo;
    @Mock SupplierCertificateRepository certRepo;
    @Mock SupplierAuditRecordRepository auditRepo;

    SupplierScoringService scoring;
    static final UUID ID = UUID.randomUUID();

    /** Fixe l'horloge pour stabiliser today.minusMonths(N). */
    static final LocalDate TODAY = LocalDate.parse("2026-05-15");
    static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        scoring = new SupplierScoringService(supplierRepo, ncRepo, certRepo, auditRepo, CLOCK);
    }

    @Test
    void recompute_perfectSupplier_keeps100() {
        Supplier s = supplier();
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(2)));
        // Tous les compteurs NC + certs expirés retournent 0 (mocks renvoient 0L par défaut).
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        int out = scoring.recompute(s);
        assertThat(out).isEqualTo(100);
    }

    @Test
    void recompute_oneOpenCritical_minus15() {
        Supplier s = supplier();
        when(ncRepo.countBySupplierIdAndSeverityAndStatus(
                ID, NonConformitySeverity.CRITICAL, NonConformityStatus.OPEN)).thenReturn(1L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(2)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(85);
    }

    @Test
    void recompute_oneOpenMajor_inReview_minus7() {
        Supplier s = supplier();
        when(ncRepo.countBySupplierIdAndSeverityAndStatus(
                ID, NonConformitySeverity.MAJOR, NonConformityStatus.IN_REVIEW)).thenReturn(1L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(2)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(93);
    }

    @Test
    void recompute_oneMinorOpen_minus2() {
        Supplier s = supplier();
        when(ncRepo.countBySupplierIdAndSeverityAndStatus(
                ID, NonConformitySeverity.MINOR, NonConformityStatus.OPEN)).thenReturn(1L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(2)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(98);
    }

    @Test
    void recompute_recentResolvedNc_minus1() {
        Supplier s = supplier();
        when(ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                eq(ID), eq(NonConformityStatus.RESOLVED), any())).thenReturn(3L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(2)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(97);
    }

    @Test
    void recompute_expiredCerts_minus8Each() {
        Supplier s = supplier();
        when(certRepo.countBySupplierIdAndExpiresOnBefore(eq(ID), eq(TODAY))).thenReturn(2L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(1)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(84);
    }

    @Test
    void recompute_noAudit_minus5() {
        Supplier s = supplier();
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.empty());
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(95);
    }

    @Test
    void recompute_obsoleteAudit_minus5() {
        Supplier s = supplier();
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(TODAY.minusMonths(24)));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isEqualTo(95);
    }

    @Test
    void recompute_clampsAtZero() {
        Supplier s = supplier();
        when(ncRepo.countBySupplierIdAndSeverityAndStatus(
                ID, NonConformitySeverity.CRITICAL, NonConformityStatus.OPEN)).thenReturn(20L);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.empty());
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(scoring.recompute(s)).isZero();
    }

    @Test
    void recompute_updatesSupplierLastAuditAt() {
        Supplier s = supplier();
        LocalDate audit = TODAY.minusMonths(3);
        when(auditRepo.findLatestAuditDate(ID)).thenReturn(Optional.of(audit));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        scoring.recompute(s);
        assertThat(s.getLastAuditAt()).isEqualTo(audit);
    }

    private Supplier supplier() {
        Supplier s = new Supplier();
        s.setId(ID);
        s.setTenantId(UUID.randomUUID());
        s.setScore(100);
        return s;
    }
}
