package com.openlab.qualitos.quality.dashboards.annotations.application;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardAnnotationServiceTest {

    @Mock DashboardAnnotationRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock ActorRoles actorRoles;

    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID OTHER_USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    DashboardAnnotationService service;

    @BeforeEach
    void setup() {
        service = new DashboardAnnotationService(repo, tenantProvider, actorRoles, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(tenantProvider.requireUserId()).thenReturn(USER);
        when(actorRoles.isTenantAdmin()).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            DashboardAnnotation a = inv.getArgument(0);
            if (a.getId() == null) a.assignId(ID);
            return a;
        });
    }

    @Test
    void create_usesJwtTenantAndAuthor_andReturnsDeletableView() {
        var view = service.create(new DashboardAnnotationDto.CreateRequest(
                "exec.trend", "Mai", "Dérive nette"));
        assertThat(view.id()).isEqualTo(ID);
        assertThat(view.tenantId()).isEqualTo(TENANT);
        assertThat(view.authorId()).isEqualTo(USER);
        assertThat(view.chartKey()).isEqualTo("exec.trend");
        assertThat(view.anchorLabel()).isEqualTo("Mai");
        assertThat(view.body()).isEqualTo("Dérive nette");
        assertThat(view.createdAt()).isEqualTo(NOW);
        assertThat(view.deletable()).isTrue();
    }

    @Test
    void create_invalidChartKey_throws() {
        assertThatThrownBy(() -> service.create(new DashboardAnnotationDto.CreateRequest(
                "BAD KEY", null, "b")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void listByChart_marksOwnDeletable_andOthersNot() {
        DashboardAnnotation mine = annotation(USER);
        DashboardAnnotation theirs = annotation(OTHER_USER);
        when(repo.findByTenantAndChartKey(TENANT, "exec.trend")).thenReturn(List.of(mine, theirs));

        List<DashboardAnnotationDto.View> views = service.listByChart("exec.trend");

        assertThat(views).hasSize(2);
        assertThat(views.get(0).deletable()).isTrue();   // authored by USER
        assertThat(views.get(1).deletable()).isFalse();  // authored by OTHER_USER
    }

    @Test
    void listByChart_adminCanDeleteAll() {
        when(actorRoles.isTenantAdmin()).thenReturn(true);
        when(repo.findByTenantAndChartKey(TENANT, "exec.trend"))
                .thenReturn(List.of(annotation(OTHER_USER)));
        assertThat(service.listByChart("exec.trend").get(0).deletable()).isTrue();
    }

    @Test
    void listByChart_blankKey_throws() {
        assertThatThrownBy(() -> service.listByChart("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listByChart(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_authorSucceeds() {
        when(repo.findByIdAndTenant(ID, TENANT)).thenReturn(Optional.of(annotation(USER, ID)));
        service.delete(ID);
        verify(repo).delete(ID);
    }

    @Test
    void delete_adminSucceeds_evenIfNotAuthor() {
        when(actorRoles.isTenantAdmin()).thenReturn(true);
        when(repo.findByIdAndTenant(ID, TENANT)).thenReturn(Optional.of(annotation(OTHER_USER, ID)));
        service.delete(ID);
        verify(repo).delete(ID);
    }

    @Test
    void delete_nonAuthorNonAdmin_forbidden() {
        when(repo.findByIdAndTenant(ID, TENANT)).thenReturn(Optional.of(annotation(OTHER_USER, ID)));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(DashboardAnnotationForbiddenException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void delete_crossTenant_returns404() {
        when(repo.findByIdAndTenant(ID, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(DashboardAnnotationNotFoundException.class);
        verify(repo, never()).delete(any());
    }

    private static DashboardAnnotation annotation(UUID author) {
        return annotation(author, null);
    }

    private static DashboardAnnotation annotation(UUID author, UUID id) {
        DashboardAnnotation a = DashboardAnnotation.create(
                TENANT, author, "exec.trend", null, "b", NOW);
        if (id != null) a.assignId(id);
        return a;
    }
}
