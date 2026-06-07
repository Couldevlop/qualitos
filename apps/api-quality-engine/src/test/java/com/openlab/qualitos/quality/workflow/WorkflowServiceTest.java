package com.openlab.qualitos.quality.workflow;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock WorkflowDefinitionRepository repo;
    @InjectMocks WorkflowService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();

    static final String VALID_BPMN =
            "<?xml version=\"1.0\"?><bpmn:definitions xmlns:bpmn=\"http://x\"><bpmn:process/></bpmn:definitions>";

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void create_savesDraftV1_withTenantAndBpmn() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowDto.Response r = service.create(
                new WorkflowDto.CreateRequest("CAPA process", "desc", VALID_BPMN));

        ArgumentCaptor<WorkflowDefinition> cap = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(cap.getValue().getStatus()).isEqualTo(WorkflowStatus.DRAFT);
        assertThat(cap.getValue().getVersion()).isEqualTo(1);
        assertThat(cap.getValue().getBpmnXml()).isEqualTo(VALID_BPMN);
        assertThat(r.name()).isEqualTo("CAPA process");
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(
                new WorkflowDto.CreateRequest("n", null, VALID_BPMN)))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_blankBpmn_throwsValidation() {
        assertThatThrownBy(() -> service.create(
                new WorkflowDto.CreateRequest("n", null, "   ")))
                .isInstanceOf(WorkflowValidationException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_oversizedBpmn_throwsValidation() {
        String big = "x".repeat(WorkflowDto.MAX_BPMN_CHARS + 1);
        assertThatThrownBy(() -> service.create(new WorkflowDto.CreateRequest("n", null, big)))
                .isInstanceOf(WorkflowValidationException.class);
        verifyNoInteractions(repo);
    }

    // --- findAll ---
    @Test
    void findAll_noFilter_returnsSummaries() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(wf(TENANT, WorkflowStatus.DRAFT))));
        Page<WorkflowDto.Summary> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
        assertThat(r.getContent().get(0).status()).isEqualTo(WorkflowStatus.DRAFT);
    }

    @Test
    void findAll_statusFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(repo.findByTenantIdAndStatus(TENANT, WorkflowStatus.PUBLISHED, p))
                .thenReturn(new PageImpl<>(List.of(wf(TENANT, WorkflowStatus.PUBLISHED))));
        assertThat(service.findAll(WorkflowStatus.PUBLISHED, p).getContent()).hasSize(1);
    }

    // --- findById ---
    @Test
    void findById_found_returnsFullResponseWithXml() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        assertThat(service.findById(w.getId()).bpmnXml()).isEqualTo(VALID_BPMN);
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(WorkflowNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_notFound() {
        WorkflowDefinition w = wf(OTHER, WorkflowStatus.DRAFT);
        // tenant courant = TENANT ; le repo filtre par tenant donc renvoie vide.
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(w.getId())).isInstanceOf(WorkflowNotFoundException.class);
    }

    // --- update + versioning ---
    @Test
    void update_incrementsVersion_andAppliesFields() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        when(repo.save(w)).thenReturn(w);

        String newXml = VALID_BPMN.replace("process", "process2");
        WorkflowDto.Response r = service.update(w.getId(),
                new WorkflowDto.UpdateRequest("renamed", "newdesc", newXml));

        assertThat(r.version()).isEqualTo(2);
        assertThat(w.getName()).isEqualTo("renamed");
        assertThat(w.getDescription()).isEqualTo("newdesc");
        assertThat(w.getBpmnXml()).isEqualTo(newXml);
    }

    @Test
    void update_partial_onlyNonNullApplied() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        w.setName("keep");
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        when(repo.save(w)).thenReturn(w);

        service.update(w.getId(), new WorkflowDto.UpdateRequest(null, "onlyDesc", null));
        assertThat(w.getName()).isEqualTo("keep");
        assertThat(w.getDescription()).isEqualTo("onlyDesc");
        assertThat(w.getVersion()).isEqualTo(2);
    }

    @Test
    void update_blankName_throwsValidation() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        assertThatThrownBy(() -> service.update(w.getId(),
                new WorkflowDto.UpdateRequest("   ", null, null)))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void update_published_throwsState() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.PUBLISHED);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        assertThatThrownBy(() -> service.update(w.getId(),
                new WorkflowDto.UpdateRequest("x", null, null)))
                .isInstanceOf(WorkflowStateException.class);
    }

    @Test
    void update_oversizedBpmn_throwsValidation() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        String big = "x".repeat(WorkflowDto.MAX_BPMN_CHARS + 1);
        assertThatThrownBy(() -> service.update(w.getId(),
                new WorkflowDto.UpdateRequest(null, null, big)))
                .isInstanceOf(WorkflowValidationException.class);
    }

    // --- publish ---
    @Test
    void publish_fromDraft_success() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        when(repo.save(w)).thenReturn(w);
        service.publish(w.getId());
        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.PUBLISHED);
    }

    @Test
    void publish_notDraft_throwsState() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.PUBLISHED);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        assertThatThrownBy(() -> service.publish(w.getId())).isInstanceOf(WorkflowStateException.class);
    }

    // --- archive ---
    @Test
    void archive_fromPublished_success() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.PUBLISHED);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        when(repo.save(w)).thenReturn(w);
        service.archive(w.getId());
        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.ARCHIVED);
    }

    @Test
    void archive_fromDraft_success() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        when(repo.save(w)).thenReturn(w);
        service.archive(w.getId());
        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_throwsState() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.ARCHIVED);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        assertThatThrownBy(() -> service.archive(w.getId())).isInstanceOf(WorkflowStateException.class);
    }

    // --- delete ---
    @Test
    void delete_success() {
        WorkflowDefinition w = wf(TENANT, WorkflowStatus.DRAFT);
        when(repo.findByIdAndTenantId(w.getId(), TENANT)).thenReturn(Optional.of(w));
        service.delete(w.getId());
        verify(repo).delete(w);
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(WorkflowNotFoundException.class);
        verify(repo, never()).delete(any());
    }

    // --- helper ---
    private WorkflowDefinition wf(UUID tenant, WorkflowStatus status) {
        WorkflowDefinition w = new WorkflowDefinition();
        w.setId(UUID.randomUUID());
        w.setTenantId(tenant);
        w.setName("wf");
        w.setBpmnXml(VALID_BPMN);
        w.setStatus(status);
        w.setVersion(1);
        w.setCreatedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        return w;
    }
}
