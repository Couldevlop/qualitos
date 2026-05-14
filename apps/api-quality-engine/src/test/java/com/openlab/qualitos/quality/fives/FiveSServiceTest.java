package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class FiveSServiceTest {

    @Mock FiveSAuditRepository auditRepository;
    @Mock FiveSAuditItemRepository itemRepository;
    @InjectMocks FiveSService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUDITOR = UUID.randomUUID();

    @BeforeEach void setCtx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr()    { TenantContext.clear(); }

    // create
    @Test
    void createAudit_success() {
        FiveSDto.CreateAuditRequest req = new FiveSDto.CreateAuditRequest(
                "Atelier A", "desc", AUDITOR, Instant.now());
        FiveSAudit saved = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.save(any())).thenReturn(saved);

        FiveSDto.AuditResponse r = service.createAudit(req);

        assertThat(r.status()).isEqualTo(FiveSAuditStatus.DRAFT);
        assertThat(r.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void createAudit_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.createAudit(new FiveSDto.CreateAuditRequest(
                "z", null, AUDITOR, null)))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(auditRepository);
    }

    // findAll
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(auditRepository.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(audit(TENANT, FiveSAuditStatus.DRAFT))));
        Page<FiveSDto.AuditResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(auditRepository, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(auditRepository.findByTenantIdAndStatus(TENANT, FiveSAuditStatus.IN_PROGRESS, p))
                .thenReturn(new PageImpl<>(List.of(audit(TENANT, FiveSAuditStatus.IN_PROGRESS))));
        Page<FiveSDto.AuditResponse> r = service.findAll(FiveSAuditStatus.IN_PROGRESS, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(FiveSAuditStatus.IN_PROGRESS);
    }

    // findById
    @Test
    void findById_found() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThat(service.findById(a.getId()).id()).isEqualTo(a.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(auditRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(FiveSAuditNotFoundException.class);
    }

    // updateAudit
    @Test
    void updateAudit_success() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(auditRepository.save(a)).thenReturn(a);
        Instant when = Instant.now().plusSeconds(60);
        service.updateAudit(a.getId(), new FiveSDto.UpdateAuditRequest("Z2", "d", when));
        assertThat(a.getZone()).isEqualTo("Z2");
        assertThat(a.getDescription()).isEqualTo("d");
        assertThat(a.getScheduledAt()).isEqualTo(when);
    }

    @Test
    void updateAudit_completed_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.COMPLETED);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.updateAudit(a.getId(),
                new FiveSDto.UpdateAuditRequest("z", null, null)))
                .isInstanceOf(FiveSStateException.class);
    }

    // start
    @Test
    void startAudit_success() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(auditRepository.save(a)).thenReturn(a);
        service.startAudit(a.getId());
        assertThat(a.getStatus()).isEqualTo(FiveSAuditStatus.IN_PROGRESS);
    }

    @Test
    void startAudit_notDraft_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.IN_PROGRESS);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.startAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class);
    }

    // complete
    @Test
    void completeAudit_success_computesOverallScore() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.IN_PROGRESS);
        for (FiveSPillar p : FiveSPillar.values()) {
            FiveSAuditItem i = item(a, p);
            i.setScore(8);
            a.getItems().add(i);
        }
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(auditRepository.save(a)).thenReturn(a);

        service.completeAudit(a.getId());

        assertThat(a.getStatus()).isEqualTo(FiveSAuditStatus.COMPLETED);
        assertThat(a.getOverallScore()).isEqualTo(80d);
        assertThat(a.getCompletedAt()).isNotNull();
    }

    @Test
    void completeAudit_missingPillars_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.IN_PROGRESS);
        a.getItems().add(item(a, FiveSPillar.SEIRI));
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.completeAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class)
                .hasMessageContaining("5 pillars");
    }

    @Test
    void completeAudit_notInProgress_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.completeAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class);
    }

    // cancel
    @Test
    void cancelAudit_success() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.IN_PROGRESS);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(auditRepository.save(a)).thenReturn(a);
        service.cancelAudit(a.getId());
        assertThat(a.getStatus()).isEqualTo(FiveSAuditStatus.CANCELLED);
    }

    @Test
    void cancelAudit_completed_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.COMPLETED);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.cancelAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class);
    }

    @Test
    void cancelAudit_alreadyCancelled_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.CANCELLED);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.cancelAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class);
    }

    // score
    @Test
    void scorePillar_newItem_created() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.IN_PROGRESS);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(itemRepository.findByAuditIdAndPillar(a.getId(), FiveSPillar.SEIRI))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FiveSDto.ScoreRequest req = new FiveSDto.ScoreRequest(FiveSPillar.SEIRI, 7, "n", "u");
        FiveSDto.ItemResponse r = service.scorePillar(a.getId(), req);
        assertThat(r.score()).isEqualTo(7);
        assertThat(r.pillar()).isEqualTo(FiveSPillar.SEIRI);
    }

    @Test
    void scorePillar_existingItem_updated() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        FiveSAuditItem existing = item(a, FiveSPillar.SEITON);
        existing.setScore(3);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(itemRepository.findByAuditIdAndPillar(a.getId(), FiveSPillar.SEITON))
                .thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);

        service.scorePillar(a.getId(), new FiveSDto.ScoreRequest(FiveSPillar.SEITON, 9, null, null));
        assertThat(existing.getScore()).isEqualTo(9);
    }

    @Test
    void scorePillar_completedAudit_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.COMPLETED);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.scorePillar(a.getId(),
                new FiveSDto.ScoreRequest(FiveSPillar.SEIRI, 5, null, null)))
                .isInstanceOf(FiveSStateException.class);
    }

    // delete
    @Test
    void deleteAudit_success() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.DRAFT);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        service.deleteAudit(a.getId());
        verify(auditRepository).delete(a);
    }

    @Test
    void deleteAudit_completed_throws() {
        FiveSAudit a = audit(TENANT, FiveSAuditStatus.COMPLETED);
        when(auditRepository.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.deleteAudit(a.getId()))
                .isInstanceOf(FiveSStateException.class);
    }

    // helpers
    private FiveSAudit audit(UUID tenant, FiveSAuditStatus status) {
        FiveSAudit a = new FiveSAudit();
        a.setId(UUID.randomUUID());
        a.setTenantId(tenant);
        a.setZone("Z");
        a.setStatus(status);
        a.setAuditorId(AUDITOR);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }

    private FiveSAuditItem item(FiveSAudit a, FiveSPillar p) {
        FiveSAuditItem i = new FiveSAuditItem();
        i.setId(UUID.randomUUID());
        i.setAudit(a);
        i.setPillar(p);
        i.setScore(5);
        i.setCreatedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return i;
    }
}
