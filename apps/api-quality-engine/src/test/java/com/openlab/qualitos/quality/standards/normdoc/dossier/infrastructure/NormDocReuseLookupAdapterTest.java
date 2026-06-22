package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierReuseLookup;
import com.openlab.qualitos.quality.standards.normdoc.infrastructure.NormDocJpaEntity;
import com.openlab.qualitos.quality.standards.normdoc.infrastructure.NormDocJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormDocReuseLookupAdapterTest {

    @Mock NormDocJpaRepository normDocs;
    @Mock NormDocTenantProvider tenantProvider;

    NormDocReuseLookupAdapter adapter;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_STD = UUID.randomUUID();
    static final UUID CURRENT_STD = UUID.randomUUID();

    @BeforeEach
    void setup() {
        adapter = new NormDocReuseLookupAdapter(normDocs, tenantProvider);
    }

    private NormDocJpaEntity entity() {
        NormDocJpaEntity e = new NormDocJpaEntity();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setStandardId(OTHER_STD);
        e.setStandardCode("iso-27001");
        e.setKind(NormDocKind.MANUAL);
        e.setTitle("Manuel ISO 27001");
        e.setSectionsJson("[]");
        e.setStatus(NormDocStatus.APPROUVE);
        e.setCreatedByUserId(UUID.randomUUID());
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }

    @Test
    void findApprovedByKind_filtersByTenantAndExcludesCurrentStandard() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(normDocs.findByTenantIdAndKindAndStatusAndStandardIdNot(
                eq(TENANT), eq(NormDocKind.MANUAL), eq(NormDocStatus.APPROUVE),
                eq(CURRENT_STD), any(Pageable.class)))
                .thenReturn(List.of(entity()));

        List<DossierReuseLookup.ReusableDoc> result =
                adapter.findApprovedByKind(NormDocKind.MANUAL, CURRENT_STD);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).standardCode()).isEqualTo("iso-27001");
        assertThat(result.get(0).kind()).isEqualTo(NormDocKind.MANUAL);
        assertThat(result.get(0).title()).isEqualTo("Manuel ISO 27001");
    }

    @Test
    void findApprovedByKind_noneFound_returnsEmpty() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(normDocs.findByTenantIdAndKindAndStatusAndStandardIdNot(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        assertThat(adapter.findApprovedByKind(NormDocKind.POLICY, CURRENT_STD)).isEmpty();
    }
}
