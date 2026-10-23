package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.circuit.application.ApprovalCircuits;
import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
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
    /** Sans réglage, Mockito rend Optional.empty() : l'approbation simple d'avant. */
    @Mock ApprovalCircuits circuits;
    @Mock DocumentCatalogService catalog;
    @Mock AuthorizationService authorization;
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);
    DocumentService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    /** L'utilisateur du jeton : c'est lui qui rédige, approuve, acquitte (ADR 0080). */
    static void connecte(UUID user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.toString(), "n/a", List.of()));
    }

    @BeforeEach void ctx() {
        service = new DocumentService(docRepo, versionRepo, ackRepo, circuits, catalog, authorization, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
        connecte(OWNER);
    }
    @AfterEach  void clr() { TenantContext.clear(); SecurityContextHolder.clearContext(); }

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
        when(docRepo.findAll(ArgumentMatchers.<Specification<Document>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc(TENANT, DocumentStatus.ACTIVE))));
        Page<DocumentDto.DocumentResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(docRepo.findAll(ArgumentMatchers.<Specification<Document>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc(TENANT, DocumentStatus.ARCHIVED))));
        Page<DocumentDto.DocumentResponse> r = service.findAll(DocumentStatus.ARCHIVED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(DocumentStatus.ARCHIVED);
        assertThat(r.getContent().get(0).displayStatus()).isEqualTo(DisplayStatus.ARCHIVED);
    }

    @Test
    void findAll_unsorted_sortsByReference() {
        when(docRepo.findAll(ArgumentMatchers.<Specification<Document>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.findAll(null, PageRequest.of(0, 10));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(docRepo).findAll(ArgumentMatchers.<Specification<Document>>any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("code")).isNotNull();
    }

    @Test
    void search_withFolder_expandsToDescendants() {
        UUID folder = UUID.randomUUID();
        when(catalog.folderAndDescendants(TENANT, folder)).thenReturn(Set.of(folder, UUID.randomUUID()));
        when(docRepo.findAll(ArgumentMatchers.<Specification<Document>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.search(new DocumentDto.ListFilter(DocumentView.ALL, folder, "pro", DisplayStatus.IN_FORCE,
                DocumentType.PROCEDURE, UUID.randomUUID(), OWNER), PageRequest.of(0, 10, Sort.by("title")));
        verify(catalog).folderAndDescendants(TENANT, folder);
    }

    @Test
    void search_toValidate_asksForDocumentsAwaitingMyDecision() {
        when(authorization.currentRoleCodes()).thenReturn(Set.of("QUALITY_MANAGER"));
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        when(docRepo.findIdsAwaitingDecisionOf(eq(TENANT), eq(OWNER), any(), eq(true))).thenReturn(List.of());
        when(docRepo.findAll(ArgumentMatchers.<Specification<Document>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.search(new DocumentDto.ListFilter(DocumentView.TO_VALIDATE, null, null, null, null, null, null),
                PageRequest.of(0, 10));
        ArgumentCaptor<java.util.Collection<String>> roles = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(docRepo).findIdsAwaitingDecisionOf(eq(TENANT), eq(OWNER), roles.capture(), eq(true));
        // Jamais vide : une liste vide rendrait la requête SQL invalide.
        assertThat(roles.getValue()).contains("QUALITY_MANAGER").hasSizeGreaterThan(1);
    }

    @Test
    void viewCounts_countsEachView() {
        when(catalog.settingsOf(TENANT)).thenReturn(DocumentSettingsRow.defaults(TENANT));
        when(authorization.currentRoleCodes()).thenReturn(Set.of());
        when(docRepo.findIdsAwaitingDecisionOf(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        when(docRepo.count(ArgumentMatchers.<Specification<Document>>any())).thenReturn(3L, 1L, 0L, 2L, 4L);
        DocumentDto.ViewCounts c = service.viewCounts();
        assertThat(c).isEqualTo(new DocumentDto.ViewCounts(3, 1, 0, 2, 4));
    }

    // --- statut affiché ---
    @Test
    void displayStatus_reviewOverdueWinsOverLifecycle() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.DRAFT);
        d.setLifecycle(DocumentLifecycle.IN_FORCE);
        d.setNextReviewDate(today);
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.IN_FORCE);
        d.setLifecycle(DocumentLifecycle.IN_REVISION);
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.IN_REVISION);
        d.setNextReviewDate(today.minusDays(1));
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.REVIEW_OVERDUE);
        d.setStatus(DocumentStatus.ARCHIVED);
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.ARCHIVED);
        d.setStatus(DocumentStatus.ACTIVE);
        d.setLifecycle(null);
        assertThat(DocumentService.displayStatus(d, today)).isEqualTo(DisplayStatus.DRAFT);
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
        connecte(AUTHOR);
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
        connecte(AUTHOR);
        assertThatThrownBy(() -> service.createVersion(d.getId(),
                new DocumentDto.CreateVersionRequest("x", null, null, AUTHOR)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void createVersion_existingDraft_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.getVersions().add(ver(d, 1, VersionStatus.DRAFT));
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        connecte(AUTHOR);
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

    // --- submitForReview : le circuit de signature (ADR 0099) ---
    static final UUID SIGNER_1 = UUID.randomUUID();
    static final UUID SIGNER_2 = UUID.randomUUID();
    static final UUID SIGNER_3 = UUID.randomUUID();

    static DocumentDto.SubmitRequest signataires(UUID... ids) {
        List<DocumentDto.SignatoryRequest> l = new java.util.ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            l.add(new DocumentDto.SignatoryRequest(ids[i], "  Signataire " + (i + 1) + "  "));
        }
        return new DocumentDto.SubmitRequest(l);
    }

    /** Une version qui porte son fichier : la soumission l'exige. */
    static DocumentVersion avecFichier(DocumentVersion v) {
        DocumentBlob b = new DocumentBlob();
        b.setContentType("application/pdf");
        v.setBlob(b);
        v.setFileName("proc.pdf");
        return v;
    }

    /** Une version en revue, avec ses signataires en attente. */
    DocumentVersion enSignature(Document d, UUID... ids) {
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        for (int i = 0; i < ids.length; i++) {
            DocumentVersionSignatory g = new DocumentVersionSignatory();
            g.setTenantId(TENANT);
            g.setVersion(v);
            g.setPosition(i + 1);
            g.setSignerId(ids[i]);
            g.setSignerName("Signataire " + (i + 1));
            v.getSignatories().add(g);
        }
        return v;
    }

    void charge(Document d, DocumentVersion v) {
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
    }

    @Test
    void submitForReview_designeLesSignatairesDansLOrdre_sansDemarrerDeCircuitParRoles() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = avecFichier(ver(d, 1, VersionStatus.DRAFT));
        v.setAuthorId(AUTHOR);
        charge(d, v);
        when(versionRepo.save(v)).thenReturn(v);

        DocumentDto.VersionResponse r = service.submitForReview(d.getId(), v.getId(),
                signataires(SIGNER_1, SIGNER_2, SIGNER_3));

        assertThat(v.getStatus()).isEqualTo(VersionStatus.IN_REVIEW);
        assertThat(r.signatories()).extracting(DocumentDto.SignatoryView::position).containsExactly(1, 2, 3);
        assertThat(r.signatories()).extracting(DocumentDto.SignatoryView::userId)
                .containsExactly(SIGNER_1, SIGNER_2, SIGNER_3);
        assertThat(r.signatories()).extracting(DocumentDto.SignatoryView::name)
                .containsExactly("Signataire 1", "Signataire 2", "Signataire 3");
        assertThat(r.signatories()).allMatch(g -> g.decision() == SignatoryDecision.PENDING);
        assertThat(v.getSignatories()).allMatch(g -> TENANT.equals(g.getTenantId()));
        verifyNoInteractions(circuits);
        verify(versionRepo, never()).saveAndFlush(any());
    }

    @Test
    void submitForReview_uneNouvelleSoumissionRemplaceLesSignataires() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = avecFichier(enSignature(d, SIGNER_1, SIGNER_2));
        v.setStatus(VersionStatus.DRAFT);
        charge(d, v);
        when(versionRepo.save(v)).thenReturn(v);

        service.submitForReview(d.getId(), v.getId(), signataires(SIGNER_3));

        verify(versionRepo).saveAndFlush(v);
        assertThat(v.getSignatories()).extracting(DocumentVersionSignatory::getSignerId).containsExactly(SIGNER_3);
    }

    @Test
    void submitForReview_sansFichier_refuse() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        charge(d, v);
        assertThatThrownBy(() -> service.submitForReview(d.getId(), v.getId(), signataires(SIGNER_1)))
                .isInstanceOf(DocumentStateException.class)
                .hasMessageContaining("Attach the document file");
        assertThat(v.getStatus()).isEqualTo(VersionStatus.DRAFT);
    }

    @Test
    void submitForReview_listeDeSignatairesInvalide_refusee() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = avecFichier(ver(d, 1, VersionStatus.DRAFT));
        v.setAuthorId(AUTHOR);
        charge(d, v);
        UUID id = d.getId();
        UUID vid = v.getId();

        assertThatThrownBy(() -> service.submitForReview(id, vid, null))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThatThrownBy(() -> service.submitForReview(id, vid, new DocumentDto.SubmitRequest(null)))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThatThrownBy(() -> service.submitForReview(id, vid, signataires()))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThatThrownBy(() -> service.submitForReview(id, vid,
                signataires(SIGNER_1, SIGNER_2, SIGNER_3, UUID.randomUUID())))
                .isInstanceOf(DocumentSignatoryException.class).hasMessageContaining("between 1 and 3");
        assertThatThrownBy(() -> service.submitForReview(id, vid, signataires(SIGNER_1, SIGNER_1)))
                .isInstanceOf(DocumentSignatoryException.class).hasMessageContaining("twice");
        assertThatThrownBy(() -> service.submitForReview(id, vid, signataires(AUTHOR)))
                .isInstanceOf(DocumentSignatoryException.class).hasMessageContaining("author");
        assertThatThrownBy(() -> service.submitForReview(id, vid, new DocumentDto.SubmitRequest(
                java.util.Arrays.asList(new DocumentDto.SignatoryRequest(SIGNER_1, "  ")))))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThatThrownBy(() -> service.submitForReview(id, vid, new DocumentDto.SubmitRequest(
                java.util.Arrays.asList((DocumentDto.SignatoryRequest) null))))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThatThrownBy(() -> service.submitForReview(id, vid, new DocumentDto.SubmitRequest(
                List.of(new DocumentDto.SignatoryRequest(null, "X")))))
                .isInstanceOf(DocumentSignatoryException.class);
        assertThat(v.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(v.getSignatories()).isEmpty();
    }

    @Test
    void submitForReview_notDraft_throws() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.PUBLISHED);
        charge(d, v);
        assertThatThrownBy(() -> service.submitForReview(d.getId(), v.getId(), signataires(SIGNER_1)))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- la signature, dans l'ordre (ADR 0099) ---

    @Test
    void signer_chacunASonTour_laDerniereSignatureApprouve() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = enSignature(d, SIGNER_1, SIGNER_2);
        charge(d, v);
        when(versionRepo.save(v)).thenReturn(v);

        connecte(SIGNER_1);
        DocumentDto.VersionResponse r = service.approveVersion(d.getId(), v.getId(),
                new DocumentDto.ApprovalRequest(null, "  Conforme  "));
        assertThat(r.status()).isEqualTo(VersionStatus.IN_REVIEW);
        assertThat(r.signatories().get(0).decision()).isEqualTo(SignatoryDecision.APPROVED);
        assertThat(r.signatories().get(0).comment()).isEqualTo("Conforme");
        assertThat(r.signatories().get(0).decidedAt()).isEqualTo(CLOCK.instant());
        assertThat(r.signatories().get(1).decision()).isEqualTo(SignatoryDecision.PENDING);
        assertThat(v.getApprovedBy()).isNull();

        connecte(SIGNER_2);
        r = service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null, " "));
        assertThat(r.status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(r.signatories().get(1).comment()).isNull();
        assertThat(v.getApprovedBy()).isEqualTo(SIGNER_2);
        assertThat(v.getApprovedAt()).isEqualTo(CLOCK.instant());
        verifyNoInteractions(circuits, authorization);
    }

    @Test
    void signer_sansCommentaire_etSansCorps() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = enSignature(d, SIGNER_1);
        charge(d, v);
        when(versionRepo.save(v)).thenReturn(v);
        connecte(SIGNER_1);

        DocumentDto.VersionResponse r = service.approveVersion(d.getId(), v.getId(), null);

        assertThat(r.status()).isEqualTo(VersionStatus.APPROVED);
        assertThat(r.signatories().get(0).comment()).isNull();
    }

    @Test
    void signer_horsDeSonTour_ouSansEtreDesigne_estRefuse() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = enSignature(d, SIGNER_1, SIGNER_2);
        charge(d, v);

        connecte(SIGNER_2);
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.rejectVersion(d.getId(), v.getId(), new DocumentDto.RejectionRequest("non")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        connecte(USER);
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(v.getSignatories()).allMatch(g -> g.getDecision() == SignatoryDecision.PENDING);
        assertThat(v.getStatus()).isEqualTo(VersionStatus.IN_REVIEW);
        verify(versionRepo, never()).save(any());
    }

    @Test
    void signer_quandToutLeMondeADecide_estUnConflit() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = enSignature(d, SIGNER_1);
        v.getSignatories().get(0).setDecision(SignatoryDecision.APPROVED);
        charge(d, v);
        connecte(SIGNER_1);
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null)))
                .isInstanceOf(DocumentStateException.class);
    }

    @Test
    void refuser_parLeSignataireDontCEstLeTour_renvoieEnBrouillon() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = enSignature(d, SIGNER_1, SIGNER_2);
        v.getSignatories().get(0).setDecision(SignatoryDecision.APPROVED);
        charge(d, v);
        when(versionRepo.save(v)).thenReturn(v);
        connecte(SIGNER_2);

        DocumentDto.VersionResponse r = service.rejectVersion(d.getId(), v.getId(),
                new DocumentDto.RejectionRequest("  Annexe manquante "));

        assertThat(r.status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(r.rejectedBy()).isEqualTo(SIGNER_2);
        assertThat(r.rejectionReason()).isEqualTo("Annexe manquante");
        assertThat(r.signatories().get(1).decision()).isEqualTo(SignatoryDecision.REJECTED);
        assertThat(r.signatories().get(1).comment()).isEqualTo("Annexe manquante");
        assertThat(r.signatories().get(1).decidedAt()).isEqualTo(CLOCK.instant());
        verifyNoInteractions(circuits, authorization);
    }

    @Test
    void versionSoumiseAvantLeCircuitDeSignature_exigeLeDroitDApprouver() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        charge(d, v);
        connecte(APPROVER);
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(false);

        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.rejectVersion(d.getId(), v.getId(), new DocumentDto.RejectionRequest("non")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(circuits);
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
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        connecte(APPROVER);
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
        connecte(AUTHOR);
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
        connecte(APPROVER);
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(),
                new DocumentDto.ApprovalRequest(APPROVER)))
                .isInstanceOf(DocumentStateException.class);
    }

    // --- circuits de validation (ADR 0080) ---

    static CircuitDto.RunView passage(UUID versionId, String status) {
        return new CircuitDto.RunView(UUID.randomUUID(), "document-version", versionId, status, 0,
                List.of(), List.of(), Instant.now(), null);
    }

    @Test
    void approveVersion_circuitStillRunning_staysInReview() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(circuits.decide(CircuitSubject.DOCUMENT_VERSION, v.getId(), true, "vu"))
                .thenReturn(Optional.of(passage(v.getId(), "IN_PROGRESS")));
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        connecte(APPROVER);

        DocumentDto.VersionResponse r = service.approveVersion(d.getId(), v.getId(),
                new DocumentDto.ApprovalRequest(null, "vu"));

        assertThat(r.status()).isEqualTo(VersionStatus.IN_REVIEW);
        assertThat(v.getApprovedBy()).isNull();
        verify(versionRepo, never()).save(any());
    }

    @Test
    void approveVersion_lastStepOfTheCircuit_approves() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        when(circuits.decide(CircuitSubject.DOCUMENT_VERSION, v.getId(), true, null))
                .thenReturn(Optional.of(passage(v.getId(), "APPROVED")));
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        connecte(APPROVER);

        service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(null));

        assertThat(v.getStatus()).isEqualTo(VersionStatus.APPROVED);
        assertThat(v.getApprovedBy()).isEqualTo(APPROVER);
    }

    @Test
    void rejectVersion_backToDraft_withTheReason_andTellsTheCircuit() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        connecte(APPROVER);

        DocumentDto.VersionResponse r = service.rejectVersion(d.getId(), v.getId(),
                new DocumentDto.RejectionRequest("  Section 4 incomplète  "));

        assertThat(r.status()).isEqualTo(VersionStatus.DRAFT);
        assertThat(r.rejectedBy()).isEqualTo(APPROVER);
        assertThat(r.rejectedAt()).isNotNull();
        assertThat(r.rejectionReason()).isEqualTo("Section 4 incomplète");
        verify(circuits).decide(CircuitSubject.DOCUMENT_VERSION, v.getId(), false, "Section 4 incomplète");
    }

    @Test
    void rejectVersion_byTheAuthor_refused() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        connecte(AUTHOR);

        assertThatThrownBy(() -> service.rejectVersion(d.getId(), v.getId(),
                new DocumentDto.RejectionRequest("non")))
                .isInstanceOf(DocumentStateException.class);
        verifyNoInteractions(circuits);
    }

    @Test
    void rejectVersion_notInReview_refused() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        connecte(APPROVER);

        assertThatThrownBy(() -> service.rejectVersion(d.getId(), v.getId(),
                new DocumentDto.RejectionRequest("non")))
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
        DocumentSettingsRow settings = DocumentSettingsRow.defaults(TENANT);
        settings.setReviewPeriodMonths(12);
        when(catalog.settingsOf(TENANT)).thenReturn(settings);

        service.publishVersion(d.getId(), next.getId());

        assertThat(old.getStatus()).isEqualTo(VersionStatus.OBSOLETE);
        assertThat(old.getObsoletedAt()).isEqualTo(Instant.parse("2026-10-19T10:00:00Z"));
        assertThat(next.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(next.getPublishedAt()).isNotNull();
        assertThat(d.getCurrentVersionId()).isEqualTo(next.getId());
        assertThat(d.getLifecycle()).isEqualTo(DocumentLifecycle.IN_FORCE);
        assertThat(d.getNextReviewDate()).isEqualTo(LocalDate.of(2027, 10, 19));
    }

    // --- GED : dossier, processus, revue (ADR 0094) ---
    @Test
    void createDocument_withFolderAndProcess_checksTheyBelongToTheTenant() {
        UUID folder = UUID.randomUUID();
        UUID process = UUID.randomUUID();
        when(docRepo.existsByTenantIdAndCode(TENANT, "PRO-9")).thenReturn(false);
        when(docRepo.save(any())).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DocumentDto.DocumentResponse r = service.createDocument(new DocumentDto.CreateDocumentRequest(
                "PRO-9", "T", null, DocumentType.EXTERNAL, OWNER, false, null, null, null, folder, process));
        verify(catalog).requireFolderOf(TENANT, folder);
        verify(catalog).requireProcessOf(TENANT, process);
        assertThat(r.folderId()).isEqualTo(folder);
        assertThat(r.processId()).isEqualTo(process);
        assertThat(r.lifecycle()).isEqualTo(DocumentLifecycle.DRAFT);
        assertThat(r.displayStatus()).isEqualTo(DisplayStatus.DRAFT);
    }

    @Test
    void updateDocument_movesAndClears() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setFolderId(UUID.randomUUID());
        d.setProcessId(UUID.randomUUID());
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(docRepo.save(d)).thenReturn(d);
        UUID folder = UUID.randomUUID();
        service.updateDocument(d.getId(), new DocumentDto.UpdateDocumentRequest(
                null, null, null, null, null, folder, null, null, null, true));
        assertThat(d.getFolderId()).isEqualTo(folder);
        assertThat(d.getProcessId()).isNull();
        verify(catalog).requireFolderOf(TENANT, folder);

        UUID process = UUID.randomUUID();
        service.updateDocument(d.getId(), new DocumentDto.UpdateDocumentRequest(
                null, null, null, null, null, null, process, null, true, null));
        assertThat(d.getFolderId()).isNull();
        assertThat(d.getProcessId()).isEqualTo(process);
    }

    @Test
    void updateDocument_reviewDate_onlyOncePublished() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        LocalDate date = LocalDate.of(2027, 1, 1);
        DocumentDto.UpdateDocumentRequest req = new DocumentDto.UpdateDocumentRequest(
                null, null, null, null, null, null, null, date, null, null);
        assertThatThrownBy(() -> service.updateDocument(d.getId(), req))
                .isInstanceOf(DocumentStateException.class);

        d.setLifecycle(DocumentLifecycle.IN_FORCE);
        when(docRepo.save(d)).thenReturn(d);
        service.updateDocument(d.getId(), req);
        assertThat(d.getNextReviewDate()).isEqualTo(date);
    }

    @Test
    void createVersion_afterPublication_inheritsFileAndGoesInRevision() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion published = ver(d, 1, VersionStatus.PUBLISHED);
        DocumentBlob blob = new DocumentBlob();
        blob.setId(UUID.randomUUID());
        blob.setSha256("a".repeat(64));
        blob.setContentType("application/pdf");
        blob.setOriginalSize(10);
        published.setBlob(blob);
        published.setFileName("proc.pdf");
        d.getVersions().add(published);
        d.setCurrentVersionId(published.getId());
        d.setLifecycle(DocumentLifecycle.IN_FORCE);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentDto.VersionResponse r = service.createVersion(d.getId(),
                new DocumentDto.CreateVersionRequest(null, null, "v2", null));

        assertThat(d.getLifecycle()).isEqualTo(DocumentLifecycle.IN_REVISION);
        assertThat(r.contentHash()).isEqualTo("a".repeat(64));
        assertThat(r.file()).isNotNull();
        assertThat(r.file().fileName()).isEqualTo("proc.pdf");
        assertThat(r.file().previewable()).isTrue();
    }

    @Test
    void updateVersion_textDoesNotReplaceFileHash() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.DRAFT);
        DocumentBlob blob = new DocumentBlob();
        blob.setSha256("b".repeat(64));
        blob.setContentType("text/csv");
        v.setBlob(blob);
        v.setContentHash("b".repeat(64));
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);
        service.updateVersion(d.getId(), v.getId(), new DocumentDto.UpdateVersionRequest("texte", null, null));
        assertThat(v.getContentHash()).isEqualTo("b".repeat(64));
    }

    @Test
    void versionResponse_purgedFile_isFlagged() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.OBSOLETE);
        v.setFileName("old.pdf");
        v.setFilePurgedAt(Instant.now());
        DocumentDto.VersionResponse r = DocumentService.toVersionResponse(v);
        assertThat(r.file().purged()).isTrue();
        assertThat(r.file().previewable()).isFalse();
        assertThat(DocumentService.toVersionResponse(ver(d, 2, VersionStatus.DRAFT)).file()).isNull();
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
        connecte(USER);
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

        connecte(USER);
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
        connecte(USER);
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
        connecte(USER);
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

    // ---------- ADR 0080 : on n'agit pas au nom d'un autre ----------

    @Test
    void approuverOuAcquitterAuNomDUnAutreEstRefuse() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        d.setMandatoryRead(true);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));

        connecte(USER);
        assertThatThrownBy(() -> service.approveVersion(d.getId(), v.getId(), new DocumentDto.ApprovalRequest(APPROVER)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(v.getStatus()).isEqualTo(VersionStatus.IN_REVIEW);

        v.setStatus(VersionStatus.PUBLISHED);
        assertThatThrownBy(() -> service.acknowledge(d.getId(), v.getId(), new DocumentDto.AcknowledgeRequest(OWNER)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void sansIdentifiantDansLeCorpsLApprobateurEstCeluiDuJeton() {
        Document d = doc(TENANT, DocumentStatus.ACTIVE);
        DocumentVersion v = ver(d, 1, VersionStatus.IN_REVIEW);
        v.setAuthorId(AUTHOR);
        when(docRepo.findByIdAndTenantId(d.getId(), TENANT)).thenReturn(Optional.of(d));
        when(versionRepo.findByIdAndDocumentId(v.getId(), d.getId())).thenReturn(Optional.of(v));
        when(versionRepo.save(v)).thenReturn(v);

        when(authorization.has(Permission.DOCUMENT_APPROVE)).thenReturn(true);
        connecte(APPROVER);
        service.approveVersion(d.getId(), v.getId(), null);

        assertThat(v.getApprovedBy()).isEqualTo(APPROVER);
    }
}
