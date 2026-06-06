package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NcServiceTest {

    @Mock NonConformityRepository repo;
    @Mock CapaCaseRepository capaRepo;
    @InjectMocks NcService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID REPORTER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create + reference generation ---
    @Test
    void create_generatesReference_andSavesOpen() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(2L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(req());

        ArgumentCaptor<NonConformity> cap = ArgumentCaptor.forClass(NonConformity.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(NcStatus.OPEN);
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(r.reference()).matches("NC-\\d{4}-0003");
        assertThat(r.reference()).endsWith("0003"); // count(2) + 1
    }

    @Test
    void create_referenceCollision_skipsToNext() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        // première référence prise, deuxième libre
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(true, false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response r = service.create(req());
        assertThat(r.reference()).endsWith("0002");
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(req()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(repo);
    }

    // --- findAll filters ---
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantId(TENANT, p)).thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        Page<NcDto.Response> r = service.findAll(null, null, null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void findAll_statusOnly() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndStatus(TENANT, NcStatus.OPEN, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(NcStatus.OPEN, null, null, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_severityOnly() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndSeverity(TENANT, NcSeverity.MAJOR, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(null, NcSeverity.MAJOR, null, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_categoryOnly() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndCategory(TENANT, NcCategory.PRODUCT, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(null, null, NcCategory.PRODUCT, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_statusAndSeverity() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndStatusAndSeverity(TENANT, NcStatus.OPEN, NcSeverity.MAJOR, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(NcStatus.OPEN, NcSeverity.MAJOR, null, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_statusAndCategory() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndStatusAndCategory(TENANT, NcStatus.OPEN, NcCategory.PRODUCT, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(NcStatus.OPEN, null, NcCategory.PRODUCT, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_severityAndCategory() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndSeverityAndCategory(TENANT, NcSeverity.MAJOR, NcCategory.PRODUCT, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(null, NcSeverity.MAJOR, NcCategory.PRODUCT, p).getContent()).hasSize(1);
    }

    @Test
    void findAll_allThreeFilters() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndStatusAndSeverityAndCategory(
                TENANT, NcStatus.OPEN, NcSeverity.MAJOR, NcCategory.PRODUCT, p))
                .thenReturn(new PageImpl<>(java.util.List.of(nc(TENANT, NcStatus.OPEN))));
        assertThat(service.findAll(NcStatus.OPEN, NcSeverity.MAJOR, NcCategory.PRODUCT, p)
                .getContent()).hasSize(1);
    }

    // --- findById ---
    @Test
    void findById_found() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThat(service.findById(n.getId()).id()).isEqualTo(n.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(NcNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_notFound() {
        NonConformity n = nc(OTHER, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(n.getId())).isInstanceOf(NcNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_success() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.update(n.getId(), new NcDto.UpdateRequest(
                "t2", "d2", NcCategory.SUPPLIER, NcSeverity.CRITICAL, "zoneB", 1.0, 2.0, "u1\nu2"));
        assertThat(n.getTitle()).isEqualTo("t2");
        assertThat(n.getCategory()).isEqualTo(NcCategory.SUPPLIER);
        assertThat(n.getSeverity()).isEqualTo(NcSeverity.CRITICAL);
        assertThat(n.getZone()).isEqualTo("zoneB");
        assertThat(n.getGeoLat()).isEqualTo(1.0);
        assertThat(n.getPhotoUrls()).isEqualTo("u1\nu2");
    }

    @Test
    void update_closed_throws() {
        NonConformity n = nc(TENANT, NcStatus.CLOSED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.update(n.getId(),
                new NcDto.UpdateRequest("x", null, null, null, null, null, null, null)))
                .isInstanceOf(NcStateException.class);
    }

    @Test
    void update_cancelled_throws() {
        NonConformity n = nc(TENANT, NcStatus.CANCELLED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.update(n.getId(),
                new NcDto.UpdateRequest("x", null, null, null, null, null, null, null)))
                .isInstanceOf(NcStateException.class);
    }

    // --- startAnalysis ---
    @Test
    void startAnalysis_success_withRootCause() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.startAnalysis(n.getId(), new NcDto.StartAnalysisRequest("cause racine X"));
        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS);
        assertThat(n.getRootCause()).isEqualTo("cause racine X");
    }

    @Test
    void startAnalysis_nullBody_ok() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.startAnalysis(n.getId(), null);
        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS);
        assertThat(n.getRootCause()).isNull();
    }

    @Test
    void startAnalysis_notOpen_throws() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.startAnalysis(n.getId(),
                new NcDto.StartAnalysisRequest(null))).isInstanceOf(NcStateException.class);
    }

    // --- defineAction ---
    @Test
    void defineAction_success() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.defineAction(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.ACTION_DEFINED);
    }

    @Test
    void defineAction_wrongState_throws() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.defineAction(n.getId())).isInstanceOf(NcStateException.class);
    }

    // --- resolve ---
    @Test
    void resolve_success() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.resolve(n.getId(), new NcDto.ResolveRequest("corrigé et vérifié"));
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
        assertThat(n.getResolutionNote()).isEqualTo("corrigé et vérifié");
        assertThat(n.getResolvedAt()).isNotNull();
    }

    @Test
    void resolve_wrongState_throws() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.resolve(n.getId(),
                new NcDto.ResolveRequest("x"))).isInstanceOf(NcStateException.class);
    }

    // --- close ---
    @Test
    void close_success() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.close(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
        assertThat(n.getClosedAt()).isNotNull();
    }

    @Test
    void close_wrongState_throws() {
        NonConformity n = nc(TENANT, NcStatus.ACTION_DEFINED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.close(n.getId())).isInstanceOf(NcStateException.class);
    }

    // --- cancel ---
    @Test
    void cancel_fromOpen_success() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.cancel(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.CANCELLED);
    }

    @Test
    void cancel_fromUnderAnalysis_success() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);
        service.cancel(n.getId());
        assertThat(n.getStatus()).isEqualTo(NcStatus.CANCELLED);
    }

    @Test
    void cancel_fromResolved_throws() {
        NonConformity n = nc(TENANT, NcStatus.RESOLVED);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.cancel(n.getId())).isInstanceOf(NcStateException.class);
    }

    // --- escalateToCapa ---
    @Test
    void escalate_createsCapa_andLinks_keepsNcStatus() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setSeverity(NcSeverity.CRITICAL);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        UUID capaId = UUID.randomUUID();
        when(capaRepo.save(any())).thenAnswer(inv -> {
            CapaCase c = inv.getArgument(0);
            c.setId(capaId);
            return c;
        });
        when(repo.save(n)).thenReturn(n);

        NcDto.Response r = service.escalateToCapa(n.getId(), new NcDto.EscalateRequest(OWNER));

        ArgumentCaptor<CapaCase> cap = ArgumentCaptor.forClass(CapaCase.class);
        verify(capaRepo).save(cap.capture());
        assertThat(cap.getValue().getSourceType()).isEqualTo(CapaSourceType.NON_CONFORMITY);
        assertThat(cap.getValue().getSourceRef()).isEqualTo(n.getReference());
        assertThat(cap.getValue().getOwnerId()).isEqualTo(OWNER);
        assertThat(cap.getValue().getCriticity()).isEqualTo(CapaCriticity.CRITICAL);
        assertThat(r.capaCaseId()).isEqualTo(capaId);
        assertThat(n.getStatus()).isEqualTo(NcStatus.UNDER_ANALYSIS); // inchangé
    }

    @Test
    void escalate_mapsMinorToLow() {
        NonConformity n = nc(TENANT, NcStatus.OPEN);
        n.setSeverity(NcSeverity.MINOR);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(capaRepo.save(any())).thenAnswer(inv -> { ((CapaCase) inv.getArgument(0)).setId(UUID.randomUUID()); return inv.getArgument(0); });
        when(repo.save(n)).thenReturn(n);

        service.escalateToCapa(n.getId(), new NcDto.EscalateRequest(OWNER));

        ArgumentCaptor<CapaCase> cap = ArgumentCaptor.forClass(CapaCase.class);
        verify(capaRepo).save(cap.capture());
        assertThat(cap.getValue().getCriticity()).isEqualTo(CapaCriticity.LOW);
    }

    @Test
    void escalate_secondTime_idempotent_refused() {
        NonConformity n = nc(TENANT, NcStatus.UNDER_ANALYSIS);
        n.setCapaCaseId(UUID.randomUUID());
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.escalateToCapa(n.getId(), new NcDto.EscalateRequest(OWNER)))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("already escalated");
        verify(capaRepo, never()).save(any());
    }

    // --- helpers ---
    private NcDto.CreateRequest req() {
        return new NcDto.CreateRequest(
                "Joint torique défectueux", "détail", NcCategory.PRODUCT, NcSeverity.MAJOR,
                Instant.now(), "Atelier 3", 48.85, 2.35, "https://s/p1.jpg", REPORTER);
    }

    private NonConformity nc(UUID tenant, NcStatus status) {
        NonConformity n = new NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(tenant);
        n.setReference("NC-2026-0001");
        n.setTitle("t");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(status);
        n.setDetectedAt(Instant.now());
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return n;
    }
}
