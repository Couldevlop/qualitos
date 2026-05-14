package com.openlab.qualitos.quality.docs;

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
class DocumentServiceTest {

    @Mock DocumentRepository docRepo;
    @Mock DocumentVersionRepository versionRepo;
    @Mock DocumentAcknowledgmentRepository ackRepo;
    @InjectMocks DocumentService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void createDocument_success_createsInitialDraftVersion() {
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "PRO-001", "Procédure", "d", DocumentType.PROCEDURE, OWNER, true,
                "hello", null, "init");
        when(docRepo.existsByTenantIdAndCode(TENANT, "PRO-001")).thenReturn(false);
        when(docRepo.save(any())).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepo.save(any())).thenAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        DocumentDto.DocumentResponse r = service.createDocument(req);

        assertThat(r.tenantId()).isEqualTo(TENANT);
        assertThat(r.versions()).hasSize(1);
        assertThat(r.versions().get(0).versionNumber()).isEqualTo(1);
        assertThat(r.versions().get(0).status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(r.versions().get(0).contentHash()).isNotBlank();
    }

    @Test
    void createDocument_codeConflict_throws() {
        when(docRepo.existsByTenantIdAndCode(TENANT, "PRO-001")).thenReturn(true);
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "PRO-001", "T", null, DocumentType.POLICY, OWNER, false, null, null, null);
        assertThatThrownBy(() -> service.createDocument(req))
                .isInstanceOf(DocumentCodeConflictException.class);
        verify(docRepo, never()).save(any());
    }

    @Test
    void createDocument_missingTenant_throws() {
        TenantContext.clear();
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "X", "T", null, DocumentType.POLICY, OWNER, false, null, null, null);
        assertThatThrownBy(() -> service.createDocument(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- findAll ---
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(docRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(doc(TENANT, DocumentStatus.ACTIVE))));
        Page<DocumentDto.DocumentResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(docRepo.findByTenantIdAndStatus(TENANT, DocumentStatus.ARCHIVED, p))
                .thenReturn(new PageImpl<>(List.of(doc(TENANT, DocumentStatus.ARCHIVED))));
        Page<DocumentDto.DocumentResponse> r = service.findAll(DocumentStatus.ARCHIVED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(DocumentStatus.ARCHIVED);
    }

    // --- findById ---
    @Test
    void findById_found() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThat(service.findById(d.getId()).id()).isEqualTo(d.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(docRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    // --- updateDocument ---
    @Test
    void updateDocument_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(docRepo.save(d)).thenReturn(d);
        UUID newOwner = UUID.randomUUID();
        service.updateDocument(d.getId(),
                new DocumentDto.UpdateDocumentRequest("T2", "D2", DocumentType.MANUAL, newOwner, true));
        assertThat(d.getTitle()).isEqualTo("T2");
        assertThat(d.getType()).isEqualTo(DocumentType.MANUAL);
        assertThat(d.getOwnerId()).isEqualTo(newOwner);
        assertThat(d.isMandatoryRead()).isTrue();
    }

    @Test
    void updateDocument_archived_throws() {
        Document d = doc(TENANT, DocumentStatus.ARCHIVED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.updateDocument(d.getId(),
                new DocumentDto.UpdateDocumentRequest("X", null, null, null, null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void archiveDocument_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(docRepo.save(d)).thenReturn(d);
        service.archiveDocument(d.getId());
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
    }

    @Test
    void archiveDocument_alreadyArchived_throws() {
        Document d = doc(TENANT, DocumentStatus.ARCHIVED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.archiveDocument(d.getId()))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- createVersion ---
    @Test
    void createVersion_success_incrementsNumber() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v1 = ver(d, 1, VersionStatus.PUBLISHED);
        d.getVersions().add(v1);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.save(any())).thenAnswer(inv -> {
            DocumentVersion x = inv.getArgument(0);
            x.setId(UUID.randomUUID());
            return x;
        });
        DocumentDto.VersionResponse r = service.createVersion(d.getId(),
                new DocumentDto.CreateVersionRequest("nouveau", null, "change", AUTHOR));
        assertThat(r.versionNumber()).isEqualTo(2);
        assertThat(r.status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(r.contentHash()).isNotBlank();
    }

    @Test
    void createVersion_archivedDoc_throws() {
        Document d = doc(TENANT, DocumentStatus.ARCHIVED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.createVersion(d.getId(),
                new DocumentDto.CreateVersionRequest("x", null, null, AUTHOR)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void createVersion_existingDraft_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.getVersions().add(ver(d, 1, VersionStatus.DRAFT));
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.createVersion(d.getId(),
                new DocumentDto.CreateVersionRequest("x", null, null, AUTHOR)))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- updateVersion ---
    @Test
    void updateVersion_draft_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        service.updateVersion(d.getId(), v.getId(),
                new DocumentDto.UpdateVersionRequest("new content", "uri", "n"));
        assertThat(v.getContent()).isEqualTo("new content");
        assertThat(v.getContentHash()).isNotBlank();
        assertThat(v.getContentUri()).isEqualTo("uri");
    }

    @Test
    void updateVersion_notDraft_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.APPROVED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.updateVersion(d.getId(), v.getId(),
                new DocumentDto.UpdateVersionRequest("x", null, null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void updateVersion_notFound_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        UUID vid = UUID.randomUUID();
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(vid, d.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateVersion(d.getId(), vid,
                new DocumentDto.UpdateVersionRequest("x", null, null)))
                .isInstanceOf(DocumentVersionNotFoundException.class);
    }

    // --- submitForReview ---
    @Test
    void submitForReview_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        service.submitForReview(d.getId(), v.getId());
        assertThat(v.getStatus()).isEqualTo(VersionStatus.IN_REVIEW);
    }

    @Test
    void submitForReview_notDraft_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.submitForReview(d.getId(), v.getId()))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- approveVersion ---
    @Test
    void approveVersion_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(APPROVER));
        assertThat(v.getStatus()).isEqualTo(VersionStatus.APPROVED);
        assertThat(v.getApprovedBy()).isEqualTo(APPROVER);
        assertThat(v.getApprovedAt()).isNotNull();
    }

    @Test
    void approveVersion_selfApproval_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(),
                new DocumentDto.ApprovalRequest(AUTHOR)))
                .isInstanceOf(DocumentStateException.class)
                .hasMessageContaining("Approver cannot be the author");
    }

    @Test
    void approveVersion_notInReview_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(),
                new DocumentDto.ApprovalRequest(APPROVER)))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- publishVersion ---
    @Test
    void publishVersion_success_obsoletesPrevious() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion old = ver(d, 1, VersionStatus.PUBLISHED);
        DocumentVersion next = ver(d, 2, VersionStatus.APPROVED);
        d.getVersions().addAll(List.of(old, next));
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(next.getId(), d.getId())).thenReturn(Optional.of(next));
        when(versionRepo.save(next)).thenReturn(next);

        service.publishVersion(d.getId(), next.getId());

        assertThat(old.getStatus()).isEqualTo(VersionStatus.OBSOLETE);
        assertThat(next.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(next.getPublishedAt()).isNotNull();
        assertThat(d.getCurrentVersionId()).isEqualTo(next.getId());
    }

    @Test
    void publishVersion_notApproved_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.publishVersion(d.getId(), v.getId()))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- blockchain ---
    @Test
    void setBlockchainTx_success() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        service.setBlockchainTx(d.getId(), v.getId(), "0xabc");
        assertThat(v.getBlockchainTxHash()).isEqualTo("0xabc");
    }

    @Test
    void setBlockchainTx_notPublished_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.setBlockchainTx(d.getId(), v.getId(), "x"))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- acknowledge ---
    @Test
    void acknowledge_success_newAck() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setMandatoryRead(true);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(ackRepo.findByVersionIdAndUserId(v.getId(), USER)).thenReturn(Optional.empty());
        when(ackRepo.save(any())).thenAnswer(inv -> {
            DocumentAcknowledgment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setAcknowledgedAt(Instant.now());
            return a;
        });
        DocumentDto.AcknowledgmentResponse r = service.acknowledge(d.getId(), v.getId(),
                new DocumentDto.AcknowledgeRequest(USER));
        assertThat(r.userId()).isEqualTo(USER);
        assertThat(r.versionId()).isEqualTo(v.getId());
    }

    @Test
    void acknowledge_idempotent_reusesExisting() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setMandatoryRead(true);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        DocumentAcknowledgment existing = new DocumentAcknowledgment();
        existing.setId(UUID.randomUUID());
        existing.setVersion(v);
        existing.setTenantId(TENANT);
        existing.setUserId(USER);
        existing.setAcknowledgedAt(Instant.now().minusSeconds(10));

        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(ackRepo.findByVersionIdAndUserId(v.getId(), USER)).thenReturn(Optional.of(existing));
        when(ackRepo.save(existing)).thenReturn(existing);

        DocumentDto.AcknowledgmentResponse r = service.acknowledge(d.getId(), v.getId(),
                new DocumentDto.AcknowledgeRequest(USER));
        assertThat(r.id()).isEqualTo(existing.getId());
    }

    @Test
    void acknowledge_notPublished_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setMandatoryRead(true);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.acknowledge(d.getId(), v.getId(),
                new DocumentDto.AcknowledgeRequest(USER)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void acknowledge_notMandatory_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setMandatoryRead(false);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.acknowledge(d.getId(), v.getId(),
                new DocumentDto.AcknowledgeRequest(USER)))
                .isInstanceOf(DocumentStateException.class)
                .hasMessageContaining("mandatory-read");
    }

    @Test
    void countAcknowledgments_returnsCount() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(ackRepo.countByVersionId(v.getId())).thenReturn(42L);
        assertThat(service.countAcknowledgments(d.getId(), v.getId())).isEqualTo(42L);
    }

    // --- computeHash ---
    @Test
    void computeHash_sameInput_sameOutput() {
        assertThat(DocumentService.computeHash("hello")).isEqualTo(DocumentService.computeHash("hello"));
        assertThat(DocumentService.computeHash("hello")).hasSize(64);
    }

    @Test
    void computeHash_nullInput_returnsNull() {
        assertThat(DocumentService.computeHash(null)).isNull();
    }

    @Test
    void computeHash_differentInputs_differentOutputs() {
        assertThat(DocumentService.computeHash("a")).isNotEqualTo(DocumentService.computeHash("b"));
    }

    // --- helpers ---
    private Document doc(UUID tenant, DocumentStatus status) {
        Document d = new Document();
        d.setId(UUID.randomUUID());
        d.setTenantId(tenant);
        d.setCode("C");
        d.setTitle("T");
        d.setType(DocumentType.PROCEDURE);
        d.setStatus(status);
        d.setOwnerId(OWNER);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }

    private DocumentVersion ver(Document d, int n, VersionStatus s) {
        DocumentVersion v = new DocumentVersion();
        v.setId(UUID.randomUUID());
        v.setDocument(d);
        v.setVersionNumber(n);
        v.setStatus(s);
        v.setAuthorId(AUTHOR);
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());
        return v;
    }
}
