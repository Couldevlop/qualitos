package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
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
class NormDocRepositoryAdapterTest {

    @Mock NormDocJpaRepository jpa;
    @Mock NormDocTenantProvider tenantProvider;

    final ObjectMapper json = new ObjectMapper();
    NormDocRepositoryAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T08:00:00Z");

    @BeforeEach
    void setup() {
        adapter = new NormDocRepositoryAdapter(jpa, tenantProvider, json);
    }

    private NormativeDocument doc(UUID tenant) {
        return NormativeDocument.draftFromAi(tenant, STD, "iso-9001", NormDocKind.MANUAL,
                "Manuel", List.of(new NormDocSection("k", "t", List.of("4.1"), "b")),
                "ollama", AUTHOR, NOW);
    }

    @Test
    void save_newDocument_persists() {
        NormativeDocument d = doc(TENANT);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.save(any())).thenAnswer(inv -> {
            NormDocJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        NormativeDocument out = adapter.save(d);
        assertThat(out.getId()).isNotNull();
        verify(jpa, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void save_existingDocument_reloadsTarget() {
        NormativeDocument d = doc(TENANT);
        d.assignId(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        NormDocJpaEntity existing = NormDocMapper.toEntity(d, null, json);
        when(jpa.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(existing));
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NormativeDocument out = adapter.save(d);
        assertThat(out.getId()).isEqualTo(d.getId());
        verify(jpa).findByIdAndTenantId(d.getId(), TENANT);
    }

    @Test
    void save_crossTenant_rejected() {
        NormativeDocument d = doc(UUID.randomUUID());
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        assertThatThrownBy(() -> adapter.save(d))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void findById_filtersByTenant() {
        UUID id = UUID.randomUUID();
        NormativeDocument d = doc(TENANT);
        d.assignId(id);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(NormDocMapper.toEntity(d, null, json)));
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
        NormativeDocument d = doc(TENANT);
        d.assignId(UUID.randomUUID());
        when(jpa.findByTenantId(eq(TENANT), any(Pageable.class)))
                .thenReturn(List.of(NormDocMapper.toEntity(d, null, json)));
        assertThat(adapter.findByTenant(TENANT)).hasSize(1);
    }

    @Test
    void findByTenantAndStatus_mapsResults() {
        NormativeDocument d = doc(TENANT);
        d.assignId(UUID.randomUUID());
        when(jpa.findByTenantIdAndStatus(eq(TENANT), eq(NormDocStatus.BROUILLON_IA),
                any(Pageable.class)))
                .thenReturn(List.of(NormDocMapper.toEntity(d, null, json)));
        assertThat(adapter.findByTenantAndStatus(TENANT, NormDocStatus.BROUILLON_IA)).hasSize(1);
    }

    @Test
    void delete_existing_removes() {
        UUID id = UUID.randomUUID();
        NormDocJpaEntity e = new NormDocJpaEntity();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(e));
        adapter.delete(id);
        verify(jpa).delete(e);
    }

    @Test
    void delete_absent_noop() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        adapter.delete(id);
        verify(jpa, never()).delete(any());
    }
}
