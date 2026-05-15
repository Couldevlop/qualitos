package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepo;
    @Mock SupplierAuditRecordRepository auditRepo;
    @Mock SupplierNonConformityRepository ncRepo;
    @Mock SupplierCertificateRepository certRepo;
    @Mock SupplierScoringService scoring;

    SupplierService service;
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID SUP = UUID.randomUUID();
    static final UUID CHILD = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(
            LocalDate.parse("2026-05-15").atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new SupplierService(supplierRepo, auditRepo, ncRepo, certRepo, scoring, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // --- Suppliers ---

    @Test
    void create_setsProspectAndScore100() {
        when(supplierRepo.findByTenantIdAndCode(TENANT, "acme")).thenReturn(Optional.empty());
        when(supplierRepo.save(any())).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(SUP); s.setCreatedAt(Instant.now()); s.setUpdatedAt(Instant.now());
            return s;
        });
        SupplierDto.SupplierResponse out = service.create(new SupplierDto.CreateSupplierRequest(
                "acme", "Acme Corp", "FR", "ops@acme.test",
                SupplierType.COMPONENT, USER));
        assertThat(out.status()).isEqualTo(SupplierStatus.PROSPECT);
        assertThat(out.score()).isEqualTo(100);
        assertThat(out.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void create_duplicateCode_throws() {
        when(supplierRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(supplier()));
        assertThatThrownBy(() -> service.create(new SupplierDto.CreateSupplierRequest(
                "dup", "n", "FR", null, SupplierType.SERVICE, USER)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new SupplierDto.CreateSupplierRequest(
                "x", "n", "FR", null, SupplierType.SERVICE, USER)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        Supplier s = supplier();
        s.setTenantId(UUID.randomUUID());
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.get(SUP))
                .isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    void update_blacklisted_rejected() {
        Supplier s = supplier();
        s.setStatus(SupplierStatus.BLACKLISTED);
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.update(SUP, new SupplierDto.UpdateSupplierRequest(
                "x", null, null, null)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void update_appliesPatches() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(SUP, new SupplierDto.UpdateSupplierRequest(
                "renamed", "DE", "x@y.z", SupplierType.SERVICE));
        assertThat(s.getName()).isEqualTo("renamed");
        assertThat(s.getCountryCode()).isEqualTo("DE");
        assertThat(s.getContactEmail()).isEqualTo("x@y.z");
        assertThat(s.getSupplierType()).isEqualTo(SupplierType.SERVICE);
    }

    @Test
    void delete_cascadesAllChildren() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        service.delete(SUP);
        verify(auditRepo).deleteBySupplierId(SUP);
        verify(ncRepo).deleteBySupplierId(SUP);
        verify(certRepo).deleteBySupplierId(SUP);
        verify(supplierRepo).delete(s);
    }

    @Test
    void list_filterByStatus_orType_orAll() {
        when(supplierRepo.findByTenantIdAndStatus(eq(TENANT), eq(SupplierStatus.APPROVED), any()))
                .thenReturn(new PageImpl<>(List.of(supplier())));
        assertThat(service.list(SupplierStatus.APPROVED, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(supplierRepo.findByTenantIdAndSupplierType(eq(TENANT), eq(SupplierType.COMPONENT), any()))
                .thenReturn(new PageImpl<>(List.of(supplier())));
        assertThat(service.list(null, SupplierType.COMPONENT, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(supplierRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(supplier())));
        assertThat(service.list(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // --- Status transitions ---

    @Test
    void changeStatus_prospectToApproved_setsApprovedFields() {
        Supplier s = supplier(); // PROSPECT
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SupplierDto.SupplierResponse out = service.changeStatus(SUP, SupplierStatus.APPROVED,
                new SupplierDto.StatusChangeRequest(USER, "Audit OK"));
        assertThat(out.status()).isEqualTo(SupplierStatus.APPROVED);
        assertThat(out.approvedAt()).isNotNull();
        assertThat(out.approvedBy()).isEqualTo(USER);
    }

    @Test
    void changeStatus_approvedToConditional_ok() {
        Supplier s = supplier(); s.setStatus(SupplierStatus.APPROVED);
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.changeStatus(SUP, SupplierStatus.CONDITIONAL,
                new SupplierDto.StatusChangeRequest(USER, null)).status())
                .isEqualTo(SupplierStatus.CONDITIONAL);
    }

    @Test
    void changeStatus_prospectToConditional_rejected() {
        Supplier s = supplier(); // PROSPECT
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.changeStatus(SUP, SupplierStatus.CONDITIONAL,
                new SupplierDto.StatusChangeRequest(USER, null)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void changeStatus_fromBlacklisted_alwaysRejected() {
        Supplier s = supplier(); s.setStatus(SupplierStatus.BLACKLISTED);
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.changeStatus(SUP, SupplierStatus.APPROVED,
                new SupplierDto.StatusChangeRequest(USER, null)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void changeStatus_nullTarget_rejected() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.changeStatus(SUP, null,
                new SupplierDto.StatusChangeRequest(USER, null)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void changeStatus_approvedToApprovedAgain_keepsOriginalApprovedAt() {
        Supplier s = supplier(); s.setStatus(SupplierStatus.SUSPENDED);
        Instant original = Instant.parse("2026-01-01T00:00:00Z");
        s.setApprovedAt(original); s.setApprovedBy(USER);
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SupplierDto.SupplierResponse out = service.changeStatus(SUP, SupplierStatus.APPROVED,
                new SupplierDto.StatusChangeRequest(UUID.randomUUID(), null));
        assertThat(out.approvedAt()).isEqualTo(original); // pas écrasé
    }

    // --- Audits ---

    @Test
    void addAudit_persistsAndRecomputesScore() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(auditRepo.save(any())).thenAnswer(inv -> {
            SupplierAuditRecord a = inv.getArgument(0);
            a.setId(CHILD); a.setCreatedAt(Instant.now()); return a;
        });
        SupplierDto.AuditResponse out = service.addAudit(SUP, new SupplierDto.CreateAuditRequest(
                LocalDate.parse("2026-04-01"), 88, USER, "summary", 0, 1, 2));
        assertThat(out.score()).isEqualTo(88);
        verify(scoring).recompute(s);
    }

    @Test
    void addAudit_findingCountsDefaultToZero() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(auditRepo.save(any())).thenAnswer(inv -> {
            SupplierAuditRecord a = inv.getArgument(0);
            a.setId(CHILD); a.setCreatedAt(Instant.now()); return a;
        });
        SupplierDto.AuditResponse out = service.addAudit(SUP, new SupplierDto.CreateAuditRequest(
                LocalDate.parse("2026-04-01"), 70, null, null, null, null, null));
        assertThat(out.criticalFindingsCount()).isZero();
        assertThat(out.majorFindingsCount()).isZero();
        assertThat(out.minorFindingsCount()).isZero();
    }

    // --- Non-conformities ---

    @Test
    void addNc_setsOpenAndRecomputesScore() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(ncRepo.save(any())).thenAnswer(inv -> {
            SupplierNonConformity nc = inv.getArgument(0);
            nc.setId(CHILD); nc.setCreatedAt(Instant.now()); nc.setUpdatedAt(Instant.now()); return nc;
        });
        SupplierDto.NonConformityResponse out = service.addNonConformity(SUP,
                new SupplierDto.CreateNonConformityRequest(
                        "LOT-42", "Dim hors tolérance",
                        NonConformitySeverity.MAJOR, LocalDate.parse("2026-05-01")));
        assertThat(out.status()).isEqualTo(NonConformityStatus.OPEN);
        verify(scoring).recompute(s);
    }

    @Test
    void updateNc_resolvedStatus_setsResolvedOnIfMissing() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        SupplierNonConformity nc = nc();
        when(ncRepo.findById(CHILD)).thenReturn(Optional.of(nc));
        when(ncRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.updateNonConformity(SUP, CHILD, new SupplierDto.UpdateNonConformityRequest(
                null, null, null, NonConformityStatus.RESOLVED, null, "Lot rebuté"));
        assertThat(nc.getResolvedOn()).isEqualTo(LocalDate.parse("2026-05-15"));
        assertThat(nc.getResolution()).isEqualTo("Lot rebuté");
        verify(scoring).recompute(s);
    }

    @Test
    void updateNc_crossSupplier_appearsNotFound() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        SupplierNonConformity nc = nc();
        nc.setSupplierId(UUID.randomUUID()); // appartient à un autre supplier
        when(ncRepo.findById(CHILD)).thenReturn(Optional.of(nc));
        assertThatThrownBy(() -> service.updateNonConformity(SUP, CHILD,
                new SupplierDto.UpdateNonConformityRequest(null, null, null, null, null, null)))
                .isInstanceOf(SupplierChildNotFoundException.class);
    }

    // --- Certificates ---

    @Test
    void addCert_validatesIssuedBeforeExpires() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.addCertificate(SUP, new SupplierDto.CreateCertificateRequest(
                "iso-9001", "REF", LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"),
                null)))
                .isInstanceOf(SupplierStateException.class);
    }

    @Test
    void addCert_happyPath_recomputesScore() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(certRepo.save(any())).thenAnswer(inv -> {
            SupplierCertificate c = inv.getArgument(0);
            c.setId(CHILD); c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now()); return c;
        });
        SupplierDto.CertificateResponse out = service.addCertificate(SUP,
                new SupplierDto.CreateCertificateRequest(
                        "iso-9001", "BV-12345",
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2027-01-01"),
                        "https://bv.example/cert"));
        assertThat(out.standardCode()).isEqualTo("iso-9001");
        assertThat(out.expired()).isFalse();
        verify(scoring).recompute(s);
    }

    @Test
    void deleteCert_crossSupplier_appearsNotFound() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        SupplierCertificate c = new SupplierCertificate();
        c.setId(CHILD); c.setSupplierId(UUID.randomUUID()); c.setTenantId(TENANT);
        when(certRepo.findById(CHILD)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteCertificate(SUP, CHILD))
                .isInstanceOf(SupplierChildNotFoundException.class);
    }

    @Test
    void deleteCert_happyPath_recomputesScore() {
        Supplier s = supplier();
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        SupplierCertificate c = new SupplierCertificate();
        c.setId(CHILD); c.setSupplierId(SUP); c.setTenantId(TENANT);
        c.setIssuedOn(LocalDate.parse("2024-01-01"));
        c.setExpiresOn(LocalDate.parse("2027-01-01"));
        when(certRepo.findById(CHILD)).thenReturn(Optional.of(c));
        service.deleteCertificate(SUP, CHILD);
        verify(certRepo).delete(c);
        verify(scoring).recompute(s);
    }

    // --- Statistics ---

    @Test
    void statistics_aggregatesCounters() {
        Supplier s = supplier(); s.setScore(72);
        when(supplierRepo.findById(SUP)).thenReturn(Optional.of(s));
        when(ncRepo.countBySupplierIdAndStatus(SUP, NonConformityStatus.OPEN)).thenReturn(2L);
        when(ncRepo.countBySupplierIdAndStatus(SUP, NonConformityStatus.IN_REVIEW)).thenReturn(1L);
        when(ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                eq(SUP), eq(NonConformityStatus.RESOLVED), any())).thenReturn(5L);
        when(certRepo.countBySupplierIdAndExpiresOnBefore(eq(SUP), any())).thenReturn(1L);
        when(auditRepo.findLatestAuditDate(SUP))
                .thenReturn(Optional.of(LocalDate.parse("2026-02-01")));
        SupplierDto.SupplierStatistics out = service.statistics(SUP);
        assertThat(out.openNonConformities()).isEqualTo(3L);
        assertThat(out.resolvedNonConformitiesRecent()).isEqualTo(5L);
        assertThat(out.expiredCertificates()).isEqualTo(1L);
        assertThat(out.score()).isEqualTo(72);
        assertThat(out.lastAuditAt()).isEqualTo(LocalDate.parse("2026-02-01"));
    }

    // --- helpers ---

    private Supplier supplier() {
        Supplier s = new Supplier();
        s.setId(SUP);
        s.setTenantId(TENANT);
        s.setCode("s-1");
        s.setName("Supplier 1");
        s.setSupplierType(SupplierType.COMPONENT);
        s.setStatus(SupplierStatus.PROSPECT);
        s.setScore(100);
        s.setCreatedBy(USER);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private SupplierNonConformity nc() {
        SupplierNonConformity nc = new SupplierNonConformity();
        nc.setId(CHILD); nc.setTenantId(TENANT); nc.setSupplierId(SUP);
        nc.setSeverity(NonConformitySeverity.MAJOR);
        nc.setStatus(NonConformityStatus.OPEN);
        nc.setDetectedOn(LocalDate.parse("2026-04-01"));
        nc.setCreatedAt(Instant.now()); nc.setUpdatedAt(Instant.now());
        return nc;
    }
}
