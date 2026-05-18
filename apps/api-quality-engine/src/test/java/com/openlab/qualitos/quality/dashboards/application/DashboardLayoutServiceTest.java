package com.openlab.qualitos.quality.dashboards.application;

import com.openlab.qualitos.quality.dashboards.domain.DashboardLayout;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutNotFoundException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutRepository;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutStateException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardLayoutServiceTest {

    @Mock DashboardLayoutRepository repo;
    @Mock TenantProvider tenantProvider;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID OTHER_USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    DashboardLayoutService service;

    @BeforeEach
    void setup() {
        service = new DashboardLayoutService(repo, tenantProvider, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(tenantProvider.requireUserId()).thenReturn(USER);
        when(repo.existsByTenantUserName(any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            DashboardLayout l = inv.getArgument(0);
            if (l.getId() == null) l.assignId(ID);
            return l;
        });
    }

    @Test
    void create_succeeds() {
        var view = service.create(new DashboardLayoutDto.SaveRequest(
                "Exec", "d", "{\"grid\":[]}", false));
        assertThat(view.id()).isEqualTo(ID);
        assertThat(view.tenantId()).isEqualTo(TENANT);
        assertThat(view.userId()).isEqualTo(USER);
        assertThat(view.version()).isEqualTo(1);
    }

    @Test
    void create_duplicate_throws() {
        when(repo.existsByTenantUserName(TENANT, USER, "Exec")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new DashboardLayoutDto.SaveRequest(
                "Exec", null, "{}", false)))
            .isInstanceOf(DashboardLayoutStateException.class);
    }

    @Test
    void get_ownerCanRead() {
        DashboardLayout owned = DashboardLayout.create(TENANT, USER, "name1", null,
                "{}", false, NOW);
        owned.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(owned));
        assertThat(service.get(ID).id()).isEqualTo(ID);
    }

    @Test
    void get_otherUserSharedCanRead() {
        DashboardLayout shared = DashboardLayout.create(TENANT, OTHER_USER, "name2", null,
                "{}", true, NOW);
        shared.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(shared));
        assertThat(service.get(ID).id()).isEqualTo(ID);
    }

    @Test
    void get_otherUserPrivateThrows() {
        DashboardLayout privateOther = DashboardLayout.create(TENANT, OTHER_USER, "name3",
                null, "{}", false, NOW);
        privateOther.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(privateOther));
        assertThatThrownBy(() -> service.get(ID))
            .isInstanceOf(DashboardLayoutNotFoundException.class);
    }

    @Test
    void crossTenant_returns404() {
        DashboardLayout other = DashboardLayout.create(OTHER_TENANT, USER, "name5", null,
                "{}", true, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
            .isInstanceOf(DashboardLayoutNotFoundException.class);
    }

    @Test
    void delete_onlyOwner() {
        DashboardLayout otherOwner = DashboardLayout.create(TENANT, OTHER_USER, "name4",
                null, "{}", true, NOW);
        otherOwner.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(otherOwner));
        assertThatThrownBy(() -> service.delete(ID))
            .isInstanceOf(DashboardLayoutNotFoundException.class);
    }

    @Test
    void list_returnsViews() {
        DashboardLayout l = DashboardLayout.create(TENANT, USER, "name1", null,
                "{}", false, NOW);
        l.assignId(ID);
        when(repo.findVisibleForUser(TENANT, USER)).thenReturn(List.of(l));
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void update_incrementsVersion() {
        DashboardLayout owned = DashboardLayout.create(TENANT, USER, "name1", null,
                "{}", false, NOW);
        owned.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(owned));
        var view = service.update(ID, new DashboardLayoutDto.SaveRequest(
                "renamed", "d", "{\"k\":1}", true));
        assertThat(view.version()).isEqualTo(2);
        assertThat(view.shared()).isTrue();
    }
}
