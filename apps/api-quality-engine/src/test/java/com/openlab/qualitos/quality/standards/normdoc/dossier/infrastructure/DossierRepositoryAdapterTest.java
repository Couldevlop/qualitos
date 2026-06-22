package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierRepositoryAdapterTest {

    @Mock DossierJpaRepository jpa;
    @Mock NormDocTenantProvider tenantProvider;

    final ObjectMapper json = new ObjectMapper();
    DossierRepositoryAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-22T08:00:00Z");

    @BeforeEach
    void setup() {
        adapter = new DossierRepositoryAdapter(jpa, tenantProvider, json);
    }

    private DocumentationDossier dossier(UUID tenant) {
        DossierDocument m = DossierDocument.planned("m", NormDocKind.MANUAL, "Manuel",
                List.of(new SectionPlan("s", "Section", List.of("4.1"), "")));
        return DocumentationDossier.start(tenant, STD, "iso-9001", "ISO 9001",
                "ACME", "fr", List.of(m), AUTHOR, NOW);
    }

    @Test
    void save_newDossier_persists() {
        DocumentationDossier d = dossier(TENANT);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.save(any())).thenAnswer(inv -> {
            DossierJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        DocumentationDossier out = adapter.save(d);
        assertThat(out.getId()).isNotNull();
        verify(jpa, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void save_existing_reloadsTarget() {
        DocumentationDossier d = dossier(TENANT);
        d.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(d.getId(), TENANT))
                .thenReturn(Optional.of(DossierMapper.toEntity(d, null, json)));
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DocumentationDossier out = adapter.save(d);
        assertThat(out.getId()).isEqualTo(d.getId());
        verify(jpa).findByIdAndTenantId(d.getId(), TENANT);
    }

    @Test
    void save_crossTenant_rejected() {
        DocumentationDossier d = dossier(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        assertThatThrownBy(() -> adapter.save(d))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void findById_filtersByTenant() {
        UUID id = UUID.randomUUID();
        DocumentationDossier d = dossier(TENANT);
        d.assignId(id);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(DossierMapper.toEntity(d, null, json)));
        assertThat(adapter.findById(id)).isPresent();
    }

    @Test
    void findById_absent_empty() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void findByTenant_mapsResults() {
        DocumentationDossier d = dossier(TENANT);
        d.assignId(UUID.randomUUID());
        when(jpa.findByTenantId(eq(TENANT), any(Pageable.class)))
                .thenReturn(List.of(DossierMapper.toEntity(d, null, json)));
        assertThat(adapter.findByTenant(TENANT)).hasSize(1);
    }
}
