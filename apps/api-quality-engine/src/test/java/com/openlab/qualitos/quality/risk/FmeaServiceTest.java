package com.openlab.qualitos.quality.risk;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FmeaServiceTest {

    @Mock FmeaProjectRepository projectRepo;
    @Mock FmeaItemRepository itemRepo;
    @InjectMocks FmeaService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID PROJ = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ----- Projects -----

    @Test
    void createProject_default100Threshold_andRevision1() {
        when(projectRepo.findByTenantIdAndCode(TENANT, "p1")).thenReturn(Optional.empty());
        when(projectRepo.save(any())).thenAnswer(inv -> {
            FmeaProject p = inv.getArgument(0);
            p.setId(PROJ); p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
            return p;
        });
        FmeaDto.ProjectResponse out = service.createProject(new FmeaDto.CreateProjectRequest(
                "p1", "Process A", null, FmeaType.PROCESS_FMEA, null, null, USER));
        assertThat(out.criticalRpnThreshold()).isEqualTo(100);
        assertThat(out.revision()).isOne();
        assertThat(out.status()).isEqualTo(FmeaStatus.DRAFT);
    }

    @Test
    void createProject_customThreshold() {
        when(projectRepo.findByTenantIdAndCode(TENANT, "p1")).thenReturn(Optional.empty());
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        FmeaDto.ProjectResponse out = service.createProject(new FmeaDto.CreateProjectRequest(
                "p1", "n", null, FmeaType.DESIGN_FMEA, 75, null, USER));
        assertThat(out.criticalRpnThreshold()).isEqualTo(75);
    }

    @Test
    void createProject_duplicateCode_throws() {
        when(projectRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(project()));
        assertThatThrownBy(() -> service.createProject(new FmeaDto.CreateProjectRequest(
                "dup", "n", null, FmeaType.PROCESS_FMEA, null, null, USER)))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void createProject_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.createProject(new FmeaDto.CreateProjectRequest(
                "x", "n", null, FmeaType.PROCESS_FMEA, null, null, USER)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void getProject_otherTenant_appearsNotFound() {
        FmeaProject p = project();
        p.setTenantId(UUID.randomUUID());
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.getProject(PROJ))
                .isInstanceOf(FmeaProjectNotFoundException.class);
    }

    @Test
    void list_filterByStatus_orType_orAll() {
        when(projectRepo.findByTenantIdAndStatus(eq(TENANT), eq(FmeaStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(project())));
        assertThat(service.listProjects(FmeaStatus.ACTIVE, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(projectRepo.findByTenantIdAndType(eq(TENANT), eq(FmeaType.BOW_TIE), any()))
                .thenReturn(new PageImpl<>(List.of(project())));
        assertThat(service.listProjects(null, FmeaType.BOW_TIE, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(projectRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(project())));
        assertThat(service.listProjects(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void updateProject_archived_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.updateProject(PROJ, new FmeaDto.UpdateProjectRequest(
                "x", null, null, null)))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void updateProject_appliesPatches() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID owner = UUID.randomUUID();
        service.updateProject(PROJ, new FmeaDto.UpdateProjectRequest("renamed", "scope", 50, owner));
        assertThat(p.getName()).isEqualTo("renamed");
        assertThat(p.getScope()).isEqualTo("scope");
        assertThat(p.getCriticalRpnThreshold()).isEqualTo(50);
        assertThat(p.getOwnerUserId()).isEqualTo(owner);
    }

    @Test
    void deleteProject_active_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ACTIVE);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteProject(PROJ))
                .isInstanceOf(FmeaStateException.class);
        verify(itemRepo, never()).deleteByProjectId(any());
    }

    @Test
    void deleteProject_draft_cascadesItems() {
        FmeaProject p = project(); // DRAFT
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        service.deleteProject(PROJ);
        verify(itemRepo).deleteByProjectId(PROJ);
        verify(projectRepo).delete(p);
    }

    @Test
    void activate_idempotentWhenAlreadyActive() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ACTIVE);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        service.activate(PROJ);
        verify(projectRepo, never()).save(any());
    }

    @Test
    void activate_fromDraft_setsLastReviewed() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        FmeaDto.ProjectResponse out = service.activate(PROJ);
        assertThat(out.status()).isEqualTo(FmeaStatus.ACTIVE);
        assertThat(out.lastReviewedAt()).isNotNull();
    }

    @Test
    void activate_archived_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.activate(PROJ))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void reopen_fromActive_incrementsRevision() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ACTIVE);
        p.setRevision(2);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        FmeaDto.ProjectResponse out = service.reopen(PROJ);
        assertThat(out.status()).isEqualTo(FmeaStatus.DRAFT);
        assertThat(out.revision()).isEqualTo(3);
    }

    @Test
    void reopen_fromDraft_rejected() {
        FmeaProject p = project(); // DRAFT
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.reopen(PROJ))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void archive_fromAnyNonArchived() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.archive(PROJ).status()).isEqualTo(FmeaStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.archive(PROJ))
                .isInstanceOf(FmeaStateException.class);
    }

    // ----- Items -----

    @Test
    void addItem_archivedProject_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.addItem(PROJ, itemReq(5, 5, 5)))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void addItem_computesRpnAndAssignsSequence() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(itemRepo.findMaxSequenceNo(PROJ)).thenReturn(4);
        ArgumentCaptor<FmeaItem> cap = ArgumentCaptor.forClass(FmeaItem.class);
        when(itemRepo.save(cap.capture())).thenAnswer(inv -> {
            FmeaItem i = inv.getArgument(0);
            i.setId(ITEM);
            i.setCreatedAt(Instant.now()); i.setUpdatedAt(Instant.now());
            return i;
        });

        FmeaDto.ItemResponse out = service.addItem(PROJ, itemReq(7, 3, 4));

        assertThat(cap.getValue().getSequenceNo()).isEqualTo(5);
        assertThat(out.rpn()).isEqualTo(84);
        assertThat(out.critical()).isFalse(); // 84 < threshold 100
    }

    @Test
    void addItem_markedCriticalWhenAboveThreshold() {
        FmeaProject p = project();
        p.setCriticalRpnThreshold(50);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(itemRepo.findMaxSequenceNo(PROJ)).thenReturn(0);
        when(itemRepo.save(any())).thenAnswer(inv -> {
            FmeaItem i = inv.getArgument(0); i.setId(ITEM);
            i.setCreatedAt(Instant.now()); i.setUpdatedAt(Instant.now()); return i;
        });
        FmeaDto.ItemResponse out = service.addItem(PROJ, itemReq(8, 4, 2)); // RPN=64 > 50
        assertThat(out.critical()).isTrue();
    }

    @Test
    void updateItem_recomputesRpn_appliesPatches() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        FmeaItem i = item(5, 5, 5);
        when(itemRepo.findById(ITEM)).thenReturn(Optional.of(i));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FmeaDto.ItemResponse out = service.updateItem(PROJ, ITEM, new FmeaDto.UpdateItemRequest(
                "fn", "fm", "fe", null, null, 9, 9, 9, null, null, null, 2, 2, 2));

        assertThat(out.rpn()).isEqualTo(729);
        assertThat(out.rpnAfter()).isEqualTo(8);
        assertThat(out.function()).isEqualTo("fn");
    }

    @Test
    void updateItem_crossProject_appearsNotFound() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        FmeaItem i = item(5, 5, 5);
        i.setProjectId(UUID.randomUUID()); // un autre projet
        when(itemRepo.findById(ITEM)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.updateItem(PROJ, ITEM,
                new FmeaDto.UpdateItemRequest(null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null)))
                .isInstanceOf(FmeaItemNotFoundException.class);
    }

    @Test
    void updateItem_archived_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.updateItem(PROJ, ITEM,
                new FmeaDto.UpdateItemRequest(null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null)))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void deleteItem_archived_rejected() {
        FmeaProject p = project();
        p.setStatus(FmeaStatus.ARCHIVED);
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteItem(PROJ, ITEM))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void deleteItem_happyPath() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        FmeaItem i = item(5, 5, 5);
        when(itemRepo.findById(ITEM)).thenReturn(Optional.of(i));
        service.deleteItem(PROJ, ITEM);
        verify(itemRepo).delete(i);
    }

    @Test
    void listItems_paginated() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(itemRepo.findByProjectIdOrderBySequenceNoAsc(eq(PROJ), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(5, 5, 5))));
        assertThat(service.listItems(PROJ, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void statistics_returnsAggregates() {
        FmeaProject p = project();
        when(projectRepo.findById(PROJ)).thenReturn(Optional.of(p));
        when(itemRepo.countByProjectId(PROJ)).thenReturn(7L);
        when(itemRepo.countByProjectIdAndRpnGreaterThanEqual(PROJ, 100)).thenReturn(2L);
        when(itemRepo.maxRpn(PROJ)).thenReturn(420);
        when(itemRepo.averageRpn(PROJ)).thenReturn(95.4);
        FmeaDto.ProjectStatistics s = service.statistics(PROJ);
        assertThat(s.totalItems()).isEqualTo(7L);
        assertThat(s.criticalItems()).isEqualTo(2L);
        assertThat(s.maxRpn()).isEqualTo(420);
        assertThat(s.averageRpn()).isEqualTo(95.4);
        assertThat(s.criticalRpnThreshold()).isEqualTo(100);
    }

    // ----- helpers -----

    private FmeaProject project() {
        FmeaProject p = new FmeaProject();
        p.setId(PROJ);
        p.setTenantId(TENANT);
        p.setCode("p-1");
        p.setName("Project 1");
        p.setType(FmeaType.PROCESS_FMEA);
        p.setStatus(FmeaStatus.DRAFT);
        p.setCriticalRpnThreshold(100);
        p.setRevision(1);
        p.setCreatedBy(USER);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private FmeaItem item(int s, int o, int d) {
        FmeaItem i = new FmeaItem();
        i.setId(ITEM);
        i.setTenantId(TENANT);
        i.setProjectId(PROJ);
        i.setSequenceNo(1);
        i.setSeverity(s); i.setOccurrence(o); i.setDetection(d);
        i.recomputeRpn();
        i.setCreatedAt(Instant.now()); i.setUpdatedAt(Instant.now());
        return i;
    }

    private FmeaDto.CreateItemRequest itemReq(int s, int o, int d) {
        return new FmeaDto.CreateItemRequest(
                "fn", "fm", "fe", null, null, s, o, d,
                null, null, null, null, null, null);
    }
}
