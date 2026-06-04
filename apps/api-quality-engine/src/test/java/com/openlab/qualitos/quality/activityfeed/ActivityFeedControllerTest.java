package com.openlab.qualitos.quality.activityfeed;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test unitaire pur (Mockito/AssertJ), sans contexte Spring. */
class ActivityFeedControllerTest {

    private static final String TENANT = "00000000-0000-0000-0000-000000000099";

    private final AuditActivityRepository repository = mock(AuditActivityRepository.class);
    private final ActivityFeedController controller = new ActivityFeedController(repository);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void recent_renvoie_le_flux_mappe_pour_le_tenant_courant() {
        TenantContext.setTenantId(TENANT);

        UUID id = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-06-01T10:00:00Z");
        Instant recordedAt = Instant.parse("2026-06-01T10:00:01Z");
        AuditActivityEntry entry = new AuditActivityEntry(
                id, UUID.fromString(TENANT), 42L, occurredAt, recordedAt,
                "AUDIT_CREATED", "Audit", resourceId, actorUserId, "Audit créé");

        Pageable pageable = PageRequest.of(0, 50);
        when(repository.findByTenantIdOrderBySequenceNoDesc(eq(UUID.fromString(TENANT)), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<ActivityFeedDto.View> page = controller.recent(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        ActivityFeedDto.View view = page.getContent().get(0);
        assertThat(view.id()).isEqualTo(id);
        assertThat(view.sequenceNo()).isEqualTo(42L);
        assertThat(view.occurredAt()).isEqualTo(occurredAt);
        assertThat(view.recordedAt()).isEqualTo(recordedAt);
        assertThat(view.action()).isEqualTo("AUDIT_CREATED");
        assertThat(view.resourceType()).isEqualTo("Audit");
        assertThat(view.resourceId()).isEqualTo(resourceId);
        assertThat(view.actorUserId()).isEqualTo(actorUserId);
        assertThat(view.summary()).isEqualTo("Audit créé");

        // Le repo est appelé avec le tenantId du contexte et le Pageable transmis.
        ArgumentCaptor<UUID> tenantCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByTenantIdOrderBySequenceNoDesc(tenantCaptor.capture(), pageableCaptor.capture());
        assertThat(tenantCaptor.getValue()).isEqualTo(UUID.fromString(TENANT));
        assertThat(pageableCaptor.getValue()).isSameAs(pageable);
    }

    @Test
    void recent_sans_tenant_leve_MissingTenantContextException() {
        TenantContext.clear();

        assertThatThrownBy(() -> controller.recent(PageRequest.of(0, 50)))
                .isInstanceOf(MissingTenantContextException.class);
    }
}
