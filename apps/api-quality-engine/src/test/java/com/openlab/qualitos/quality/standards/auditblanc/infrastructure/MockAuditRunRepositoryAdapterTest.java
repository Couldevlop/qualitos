package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Adapter JPA — filtrage tenant strict (§8.4 onglet 7, OWASP A01). */
@ExtendWith(MockitoExtension.class)
class MockAuditRunRepositoryAdapterTest {

    @Mock MockAuditRunJpaRepository jpa;

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MockAuditTenantProvider tenantProvider = () -> TENANT;

    private MockAuditRunRepositoryAdapter adapter() {
        return new MockAuditRunRepositoryAdapter(jpa, tenantProvider, json);
    }

    private static MockAuditRun run(UUID tenant) {
        return MockAuditRun.of(tenant, UUID.randomUUID(), UUID.randomUUID(),
                "iso-9001", "ISO 9001:2015", 50d, List.of(), List.of(), List.of(),
                "ollama", UUID.randomUUID(), Instant.parse("2026-06-20T09:00:00Z"));
    }

    @Test
    void save_currentTenant_persistsAndReassignsId() {
        MockAuditRun run = run(TENANT);
        when(jpa.save(any())).thenAnswer(inv -> {
            MockAuditRunJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        MockAuditRun saved = adapter().save(run);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCode()).isEqualTo("iso-9001");
    }

    @Test
    void save_crossTenant_rejected() {
        assertThatThrownBy(() -> adapter().save(run(OTHER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void findById_filtersByTenant() {
        UUID id = UUID.randomUUID();
        MockAuditRun stored = run(TENANT);
        stored.assignId(id);
        when(jpa.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(MockAuditRunMapper.toEntity(stored, json)));
        assertThat(adapter().findById(id)).isPresent();

        UUID missing = UUID.randomUUID();
        lenient().when(jpa.findByIdAndTenantId(eq(missing), eq(TENANT)))
                .thenReturn(Optional.empty());
        assertThat(adapter().findById(missing)).isEmpty();
    }

    @Test
    void findByAdoption_filtersByTenant() {
        UUID adoption = UUID.randomUUID();
        MockAuditRun stored = run(TENANT);
        stored.assignId(UUID.randomUUID());
        when(jpa.findByTenantIdAndAdoptionId(eq(TENANT), eq(adoption), any()))
                .thenReturn(List.of(MockAuditRunMapper.toEntity(stored, json)));
        assertThat(adapter().findByAdoption(adoption)).hasSize(1);
    }
}
