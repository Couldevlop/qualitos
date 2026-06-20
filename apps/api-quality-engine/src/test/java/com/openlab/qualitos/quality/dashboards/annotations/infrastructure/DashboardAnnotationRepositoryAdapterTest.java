package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAnnotationRepositoryAdapterTest {

    @Mock DashboardAnnotationJpaRepository jpa;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");

    DashboardAnnotationRepositoryAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new DashboardAnnotationRepositoryAdapter(jpa);
    }

    @Test
    void save_generatesIdWhenAbsent_andAssignsBack() {
        DashboardAnnotation a = DashboardAnnotation.create(TENANT, AUTHOR, "exec.trend", "Mai", "b", NOW);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DashboardAnnotation out = adapter.save(a);

        ArgumentCaptor<DashboardAnnotationJpaEntity> captor =
                ArgumentCaptor.forClass(DashboardAnnotationJpaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(AUTHOR);
        assertThat(captor.getValue().getChartKey()).isEqualTo("exec.trend");
        assertThat(captor.getValue().getAnchorLabel()).isEqualTo("Mai");
        assertThat(out.getId()).isEqualTo(captor.getValue().getId());
        assertThat(a.getId()).isEqualTo(captor.getValue().getId()); // assigned back
    }

    @Test
    void save_keepsExistingId() {
        DashboardAnnotation a = DashboardAnnotation.create(TENANT, AUTHOR, "exec.trend", null, "b", NOW);
        UUID id = UUID.randomUUID();
        a.assignId(id);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.save(a);

        ArgumentCaptor<DashboardAnnotationJpaEntity> captor =
                ArgumentCaptor.forClass(DashboardAnnotationJpaEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    void findByIdAndTenant_mapsResult() {
        UUID id = UUID.randomUUID();
        DashboardAnnotationJpaEntity e = entity(id, TENANT, AUTHOR);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(e));

        Optional<DashboardAnnotation> out = adapter.findByIdAndTenant(id, TENANT);
        assertThat(out).isPresent();
        assertThat(out.get().getId()).isEqualTo(id);
        assertThat(out.get().getBody()).isEqualTo("b");
    }

    @Test
    void findByIdAndTenant_empty() {
        UUID id = UUID.randomUUID();
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThat(adapter.findByIdAndTenant(id, TENANT)).isEmpty();
    }

    @Test
    void findByTenantAndChartKey_mapsList() {
        when(jpa.findByTenantIdAndChartKeyOrderByCreatedAtDesc(TENANT, "exec.trend"))
                .thenReturn(List.of(entity(UUID.randomUUID(), TENANT, AUTHOR)));
        assertThat(adapter.findByTenantAndChartKey(TENANT, "exec.trend")).hasSize(1);
    }

    @Test
    void delete_delegates() {
        UUID id = UUID.randomUUID();
        adapter.delete(id);
        verify(jpa).deleteById(id);
    }

    private static DashboardAnnotationJpaEntity entity(UUID id, UUID tenant, UUID author) {
        DashboardAnnotationJpaEntity e = new DashboardAnnotationJpaEntity();
        e.setId(id);
        e.setTenantId(tenant);
        e.setAuthorId(author);
        e.setChartKey("exec.trend");
        e.setAnchorLabel("Mai");
        e.setBody("b");
        e.setCreatedAt(NOW);
        return e;
    }
}
