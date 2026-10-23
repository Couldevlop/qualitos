package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.circuit.application.ApprovalCircuits;
import com.openlab.qualitos.quality.circuit.application.CircuitDto;
import com.openlab.qualitos.quality.circuit.domain.CircuitRun;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import org.springframework.security.access.AccessDeniedException;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    /**
     * Rôle qu'aucun client ne porte : la requête « à valider par moi » exige une
     * liste de rôles non vide, et un utilisateur sans rôle ne doit rien y trouver.
     */
    private static final String NO_ROLE = "-";

    /** Signataire 1, 2 et 3 : pas davantage (ADR 0099). */
    static final int MAX_SIGNATORIES = 3;

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAcknowledgmentRepository ackRepository;
    private final ApprovalCircuits circuits;
    private final DocumentCatalogService catalog;
    private final AuthorizationService authorization;
    private final Clock clock;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           DocumentAcknowledgmentRepository ackRepository,
                           ApprovalCircuits circuits,
                           DocumentCatalogService catalog,
                           AuthorizationService authorization,
                           Clock clock) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.ackRepository = ackRepository;
        this.circuits = circuits;
        this.catalog = catalog;
        this.authorization = authorization;
        this.clock = clock;
    }

    // --- documents ---

    @Transactional(readOnly = true)
    public Page<DocumentDto.DocumentResponse> findAll(DocumentStatus status, Pageable pageable) {
        return search(DocumentDto.ListFilter.of(status), pageable);
    }

    /** La liste de la GED, filtrée comme l'écran le demande (ADR 0094). */
    @Transactional(readOnly = true)
    public Page<DocumentDto.DocumentResponse> search(DocumentDto.ListFilter f, Pageable pageable) {
        UUID tenantId = requireTenantId();
        LocalDate today = today();
        List<Specification<Document>> specs = new ArrayList<>();
        specs.add(DocumentSpecifications.tenant(tenantId));
        if (f.view() != null) {
            specs.add(viewSpec(tenantId, f.view(), today));
        }
        if (f.folderId() != null) {
            specs.add(DocumentSpecifications.inFolders(catalog.folderAndDescendants(tenantId, f.folderId())));
        }
        specs.add(DocumentSpecifications.matching(f.q()));
        specs.add(DocumentSpecifications.withDisplayStatus(f.displayStatus(), today));
        specs.add(DocumentSpecifications.ofType(f.type()));
        specs.add(DocumentSpecifications.ofProcess(f.processId()));
        specs.add(DocumentSpecifications.ownedBy(f.ownerId()));
        Pageable sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("code"));
        return documentRepository.findAll(Specification.allOf(specs.stream().filter(s -> s != null).toList()), sorted)
                .map(d -> toResponse(d, today));
    }

    /** Les compteurs des vues rapides du panneau de gauche. */
    @Transactional(readOnly = true)
    public DocumentDto.ViewCounts viewCounts() {
        UUID tenantId = requireTenantId();
        LocalDate today = today();
        return new DocumentDto.ViewCounts(
                count(tenantId, DocumentView.ALL, today),
                count(tenantId, DocumentView.MINE, today),
                count(tenantId, DocumentView.TO_VALIDATE, today),
                count(tenantId, DocumentView.REVIEW_DUE, today),
                count(tenantId, DocumentView.ARCHIVED, today));
    }

    private long count(UUID tenantId, DocumentView view, LocalDate today) {
        return documentRepository.count(Specification.allOf(DocumentSpecifications.tenant(tenantId),
                viewSpec(tenantId, view, today)));
    }

    private Specification<Document> viewSpec(UUID tenantId, DocumentView view, LocalDate today) {
        return switch (view) {
            case ALL -> DocumentSpecifications.active();
            case ARCHIVED -> DocumentSpecifications.archived();
            case MINE -> Specification.allOf(DocumentSpecifications.active(),
                    DocumentSpecifications.ownedBy(CurrentUser.requireUserId()));
            case REVIEW_DUE -> Specification.allOf(DocumentSpecifications.active(),
                    DocumentSpecifications.reviewDueBy(
                            today.plusDays(catalog.settingsOf(tenantId).getReviewWarningDays())));
            case TO_VALIDATE -> DocumentSpecifications.idIn(awaitingMyDecision(tenantId));
        };
    }

    /** Les documents dont une version en revue attend la décision de l'utilisateur. */
    private List<UUID> awaitingMyDecision(UUID tenantId) {
        Set<String> roles = new HashSet<>(authorization.currentRoleCodes());
        roles.add(NO_ROLE);
        return documentRepository.findIdsAwaitingDecisionOf(tenantId, CurrentUser.requireUserId(), roles,
                authorization.has(Permission.DOCUMENT_APPROVE)).stream().map(UUID::fromString).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto.DocumentResponse findById(UUID id) {
        return toResponse(loadDocument(id), today());
    }

    public DocumentDto.DocumentResponse createDocument(DocumentDto.CreateDocumentRequest req) {
        UUID tenantId = requireTenantId();
        if (documentRepository.existsByTenantIdAndCode(tenantId, req.code())) {
            throw new DocumentCodeConflictException(req.code());
        }
        if (req.folderId() != null) catalog.requireFolderOf(tenantId, req.folderId());
        if (req.processId() != null) catalog.requireProcessOf(tenantId, req.processId());

        Document d = new Document();
        d.setTenantId(tenantId);
        d.setCode(req.code());
        d.setTitle(req.title());
        d.setDescription(req.description());
        d.setType(req.type());
        d.setStatus(DocumentStatus.ACTIVE);
        d.setOwnerId(req.ownerId());
        d.setMandatoryRead(req.mandatoryRead());
        d.setFolderId(req.folderId());
        d.setProcessId(req.processId());
        d.setLifecycle(DocumentLifecycle.DRAFT);
        d = documentRepository.save(d);

        DocumentVersion v = new DocumentVersion();
        v.setDocument(d);
        v.setVersionNumber(1);
        v.setContent(req.initialContent());
        v.setContentUri(req.initialContentUri());
        v.setContentHash(computeHash(req.initialContent()));
        v.setChangeNote(req.initialChangeNote());
        v.setStatus(VersionStatus.DRAFT);
        // L'auteur de la première version est celui qui la rédige, pas le
        // propriétaire désigné : sans quoi la règle « l'approbateur n'est pas
        // l'auteur » se contournait en désignant un autre propriétaire.
        v.setAuthorId(CurrentUser.requireUserId());
        versionRepository.save(v);
        d.getVersions().add(v);

        return toResponse(d, today());
    }

    public DocumentDto.DocumentResponse updateDocument(UUID id, DocumentDto.UpdateDocumentRequest req) {
        Document d = loadDocument(id);
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new DocumentStateException("Archived document cannot be modified");
        }
        if (req.title() != null) d.setTitle(req.title());
        if (req.description() != null) d.setDescription(req.description());
        if (req.type() != null) d.setType(req.type());
        if (req.ownerId() != null) d.setOwnerId(req.ownerId());
        if (req.mandatoryRead() != null) d.setMandatoryRead(req.mandatoryRead());
        if (Boolean.TRUE.equals(req.clearFolder())) {
            d.setFolderId(null);
        } else if (req.folderId() != null) {
            catalog.requireFolderOf(d.getTenantId(), req.folderId());
            d.setFolderId(req.folderId());
        }
        if (Boolean.TRUE.equals(req.clearProcess())) {
            d.setProcessId(null);
        } else if (req.processId() != null) {
            catalog.requireProcessOf(d.getTenantId(), req.processId());
            d.setProcessId(req.processId());
        }
        if (req.nextReviewDate() != null) {
            if (d.getLifecycle() == DocumentLifecycle.DRAFT) {
                throw new DocumentStateException("A document gets a review date once a version is published");
            }
            d.setNextReviewDate(req.nextReviewDate());
        }
        return toResponse(documentRepository.save(d), today());
    }

    public DocumentDto.DocumentResponse archiveDocument(UUID id) {
        Document d = loadDocument(id);
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new DocumentStateException("Document already archived");
        }
        d.setStatus(DocumentStatus.ARCHIVED);
        return toResponse(documentRepository.save(d), today());
    }

    // --- versions ---

    public DocumentDto.VersionResponse createVersion(UUID documentId, DocumentDto.CreateVersionRequest req) {
        Document d = loadDocument(documentId);
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new DocumentStateException("Cannot add a version to an archived document");
        }
        boolean hasDraft = d.getVersions().stream()
                .anyMatch(v -> v.getStatus() == VersionStatus.DRAFT
                        || v.getStatus() == VersionStatus.IN_REVIEW);
        if (hasDraft) {
            throw new DocumentStateException(
                    "Another version is already DRAFT or IN_REVIEW — finish it first");
        }
        int next = d.getVersions().stream()
                .mapToInt(DocumentVersion::getVersionNumber).max().orElse(0) + 1;

        DocumentVersion v = new DocumentVersion();
        v.setDocument(d);
        v.setVersionNumber(next);
        v.setContent(req.content());
        v.setContentUri(req.contentUri());
        v.setContentHash(computeHash(req.content()));
        v.setChangeNote(req.changeNote());
        v.setStatus(VersionStatus.DRAFT);
        v.setAuthorId(actingAs(req.authorId()));
        // La nouvelle version part du fichier de celle qui fait foi : tant qu'on ne
        // le remplace pas, les deux partagent le même objet stocké (ADR 0094).
        latestWithFile(d).ifPresent(prev -> {
            v.setBlob(prev.getBlob());
            v.setFileName(prev.getFileName());
            if (req.content() == null) {
                v.setContentHash(prev.getBlob().getSha256());
            }
        });
        if (d.getCurrentVersionId() != null) {
            d.setLifecycle(DocumentLifecycle.IN_REVISION);
            documentRepository.save(d);
        }
        return toVersionResponse(versionRepository.save(v));
    }

    private static Optional<DocumentVersion> latestWithFile(Document d) {
        return d.getVersions().stream()
                .filter(x -> x.getBlob() != null)
                .max(Comparator.comparing(DocumentVersion::getVersionNumber));
    }

    public DocumentDto.VersionResponse updateVersion(UUID documentId, UUID versionId,
                                                    DocumentDto.UpdateVersionRequest req) {
        loadDocument(documentId);
        DocumentVersion v = versionRepository.findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new DocumentVersionNotFoundException(versionId));
        if (v.getStatus() != VersionStatus.DRAFT) {
            throw new DocumentStateException("Only DRAFT versions can be edited");
        }
        if (req.content() != null) {
            v.setContent(req.content());
            // Un fichier joint fait foi : son empreinte n'est pas remplacée par celle du texte.
            if (v.getBlob() == null) {
                v.setContentHash(computeHash(req.content()));
            }
        }
        if (req.contentUri() != null) v.setContentUri(req.contentUri());
        if (req.changeNote() != null) v.setChangeNote(req.changeNote());
        return toVersionResponse(versionRepository.save(v));
    }

    /**
     * Soumet un brouillon à validation, avec le circuit de signature que l'auteur
     * désigne : un à trois signataires, qui signeront dans l'ordre (ADR 0099). La
     * version doit porter son fichier : un document se valide sur pièce.
     * Une nouvelle soumission remplace les signataires de la précédente.
     */
    public DocumentDto.VersionResponse submitForReview(UUID documentId, UUID versionId,
                                                      DocumentDto.SubmitRequest req) {
        Document d = loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.DRAFT) {
            throw new DocumentStateException("Only DRAFT versions can be submitted for review");
        }
        if (v.getBlob() == null) {
            throw new DocumentStateException("Attach the document file before submitting the version");
        }
        List<DocumentDto.SignatoryRequest> designes = checkedSignatories(req, v.getAuthorId());
        if (!v.getSignatories().isEmpty()) {
            // Les anciennes lignes partent avant les nouvelles : mêmes rangs, même version.
            v.getSignatories().clear();
            versionRepository.saveAndFlush(v);
        }
        for (int i = 0; i < designes.size(); i++) {
            DocumentDto.SignatoryRequest s = designes.get(i);
            DocumentVersionSignatory g = new DocumentVersionSignatory();
            g.setTenantId(d.getTenantId());
            g.setVersion(v);
            g.setPosition(i + 1);
            g.setSignerId(s.userId());
            g.setSignerName(s.name().strip());
            v.getSignatories().add(g);
        }
        v.setStatus(VersionStatus.IN_REVIEW);
        return toVersionResponse(versionRepository.save(v));
    }

    /** Un à trois signataires, tous distincts, et jamais l'auteur : on ne valide pas son propre texte. */
    private static List<DocumentDto.SignatoryRequest> checkedSignatories(DocumentDto.SubmitRequest req, UUID auteur) {
        List<DocumentDto.SignatoryRequest> liste = req == null || req.signatories() == null
                ? List.of() : req.signatories();
        if (liste.isEmpty() || liste.size() > MAX_SIGNATORIES) {
            throw new DocumentSignatoryException("Designate between 1 and " + MAX_SIGNATORIES + " signatories");
        }
        Set<UUID> vus = new HashSet<>();
        for (DocumentDto.SignatoryRequest s : liste) {
            if (s == null || s.userId() == null || s.name() == null || s.name().isBlank()) {
                throw new DocumentSignatoryException("Each signatory needs a directory account and a name");
            }
            if (s.userId().equals(auteur)) {
                throw new DocumentSignatoryException("The author of the version cannot sign it");
            }
            if (!vus.add(s.userId())) {
                throw new DocumentSignatoryException("The same person cannot sign twice");
            }
        }
        return liste;
    }

    /** Le signataire dont c'est le tour : le premier rang encore en attente. */
    private static Optional<DocumentVersionSignatory> currentSignatory(DocumentVersion v) {
        return v.getSignatories().stream()
                .filter(s -> s.getDecision() == SignatoryDecision.PENDING)
                .min(Comparator.comparingInt(DocumentVersionSignatory::getPosition));
    }

    /** Seul le signataire dont c'est le tour signe ou refuse ; les autres reçoivent 403. */
    private static DocumentVersionSignatory requireMyTurn(DocumentVersion v, UUID acteur) {
        DocumentVersionSignatory tour = currentSignatory(v)
                .orElseThrow(() -> new DocumentStateException("Every signatory has already decided"));
        if (!tour.getSignerId().equals(acteur)) {
            throw new AccessDeniedException("It is not your turn to sign this version");
        }
        return tour;
    }

    /** Une version sans signataires (soumise avant le circuit de signature) suit l'approbation par droit. */
    private void requireApproveRight() {
        if (!authorization.has(Permission.DOCUMENT_APPROVE)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public DocumentDto.VersionResponse approveVersion(UUID documentId, UUID versionId,
                                                     DocumentDto.ApprovalRequest req) {
        loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.IN_REVIEW) {
            throw new DocumentStateException("Only IN_REVIEW versions can be approved");
        }
        UUID approbateur = actingAs(req == null ? null : req.approverId());
        if (approbateur.equals(v.getAuthorId())) {
            throw new DocumentStateException("Approver cannot be the author of the version");
        }
        if (!v.getSignatories().isEmpty()) {
            // Le circuit de signature : le signataire dont c'est le tour signe ; la
            // version n'est approuvée qu'à la dernière signature (ADR 0099).
            DocumentVersionSignatory tour = requireMyTurn(v, approbateur);
            Instant now = clock.instant();
            tour.setDecision(SignatoryDecision.APPROVED);
            tour.setDecidedAt(now);
            tour.setComment(req == null || req.comment() == null || req.comment().isBlank()
                    ? null : req.comment().strip());
            if (currentSignatory(v).isEmpty()) {
                v.setStatus(VersionStatus.APPROVED);
                v.setApprovedBy(approbateur);
                v.setApprovedAt(now);
            }
            return toVersionResponse(versionRepository.save(v));
        }
        requireApproveRight();
        // Avec un circuit, l'approbation franchit une étape ; la version n'est
        // approuvée qu'à la dernière. Sans circuit, une approbation suffit.
        Optional<CircuitDto.RunView> passage = circuits.decide(CircuitSubject.DOCUMENT_VERSION, v.getId(), true,
                req == null ? null : req.comment());
        if (passage.isPresent() && !CircuitRun.Status.APPROVED.name().equals(passage.get().status())) {
            return toVersionResponse(v);
        }
        v.setStatus(VersionStatus.APPROVED);
        v.setApprovedBy(approbateur);
        v.setApprovedAt(Instant.now());
        return toVersionResponse(versionRepository.save(v));
    }

    /**
     * Refuse une version en revue : elle revient en brouillon avec la raison,
     * pour que l'auteur la corrige et la soumette à nouveau. Avec des
     * signataires, seul celui dont c'est le tour refuse ; avec un circuit par
     * rôles d'avant (ADR 0080), le porteur du rôle de l'étape en cours.
     */
    public DocumentDto.VersionResponse rejectVersion(UUID documentId, UUID versionId,
                                                    DocumentDto.RejectionRequest req) {
        loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.IN_REVIEW) {
            throw new DocumentStateException("Only IN_REVIEW versions can be rejected");
        }
        UUID acteur = CurrentUser.requireUserId();
        if (acteur.equals(v.getAuthorId())) {
            throw new DocumentStateException("The author cannot reject their own version");
        }
        String raison = req.reason().strip();
        if (!v.getSignatories().isEmpty()) {
            // Un refus du signataire dont c'est le tour arrête le circuit (ADR 0099).
            DocumentVersionSignatory tour = requireMyTurn(v, acteur);
            tour.setDecision(SignatoryDecision.REJECTED);
            tour.setDecidedAt(clock.instant());
            tour.setComment(raison);
        } else {
            requireApproveRight();
            circuits.decide(CircuitSubject.DOCUMENT_VERSION, v.getId(), false, raison);
        }
        v.setStatus(VersionStatus.DRAFT);
        v.setRejectedBy(acteur);
        v.setRejectedAt(Instant.now());
        v.setRejectionReason(raison);
        return toVersionResponse(versionRepository.save(v));
    }

    public DocumentDto.VersionResponse publishVersion(UUID documentId, UUID versionId) {
        Document d = loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.APPROVED) {
            throw new DocumentStateException("Only APPROVED versions can be published");
        }
        Instant now = clock.instant();
        // Marquer l'ancienne PUBLISHED en OBSOLETE : sa conservation court dès maintenant.
        d.getVersions().stream()
                .filter(other -> other.getStatus() == VersionStatus.PUBLISHED)
                .forEach(other -> {
                    other.setStatus(VersionStatus.OBSOLETE);
                    other.setObsoletedAt(now);
                });

        v.setStatus(VersionStatus.PUBLISHED);
        v.setPublishedAt(now);
        d.setCurrentVersionId(v.getId());
        d.setLifecycle(DocumentLifecycle.IN_FORCE);
        // La revue périodique repart de la publication, au délai réglé par le client.
        d.setNextReviewDate(today().plusMonths(catalog.settingsOf(d.getTenantId()).getReviewPeriodMonths()));
        documentRepository.save(d);
        return toVersionResponse(versionRepository.save(v));
    }

    public DocumentDto.VersionResponse setBlockchainTx(UUID documentId, UUID versionId, String txHash) {
        loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.PUBLISHED) {
            throw new DocumentStateException("Only PUBLISHED versions can be anchored on blockchain");
        }
        v.setBlockchainTxHash(txHash);
        return toVersionResponse(versionRepository.save(v));
    }

    // --- acknowledgments ---

    public DocumentDto.AcknowledgmentResponse acknowledge(UUID documentId, UUID versionId,
                                                         DocumentDto.AcknowledgeRequest req) {
        UUID tenantId = requireTenantId();
        Document d = loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.PUBLISHED) {
            throw new DocumentStateException("Only PUBLISHED versions can be acknowledged");
        }
        if (!d.isMandatoryRead()) {
            throw new DocumentStateException("Document is not marked as mandatory-read");
        }
        UUID lecteur = actingAs(req == null ? null : req.userId());
        DocumentAcknowledgment ack = ackRepository.findByVersionIdAndUserId(versionId, lecteur)
                .orElseGet(() -> {
                    DocumentAcknowledgment a = new DocumentAcknowledgment();
                    a.setVersion(v);
                    a.setTenantId(tenantId);
                    a.setUserId(lecteur);
                    return a;
                });
        ack = ackRepository.save(ack);
        return new DocumentDto.AcknowledgmentResponse(
                ack.getId(), v.getId(), ack.getUserId(), ack.getAcknowledgedAt());
    }

    @Transactional(readOnly = true)
    public long countAcknowledgments(UUID documentId, UUID versionId) {
        loadDocument(documentId);
        loadVersion(documentId, versionId);
        return ackRepository.countByVersionId(versionId);
    }

    // --- helpers ---

    private Document loadDocument(UUID id) {
        UUID tenantId = requireTenantId();
        return documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private DocumentVersion loadVersion(UUID documentId, UUID versionId) {
        return versionRepository.findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new DocumentVersionNotFoundException(versionId));
    }

    /**
     * L'utilisateur du jeton, qui agit (ADR 0080). Un identifiant fourni dans le
     * corps doit être le sien : agir au nom d'un autre est refusé (403), pas
     * corrigé en silence — la tentative doit se voir.
     */
    static UUID actingAs(UUID declared) {
        UUID acteur = CurrentUser.requireUserId();
        if (declared != null && !declared.equals(acteur)) {
            throw new AccessDeniedException("Acting on behalf of another user is not allowed");
        }
        return acteur;
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    static String computeHash(String content) {
        if (content == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Le statut affiché : une revue dépassée l'emporte sur le cycle de vie (ADR 0094). */
    static DisplayStatus displayStatus(Document d, LocalDate today) {
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            return DisplayStatus.ARCHIVED;
        }
        DocumentLifecycle lc = d.getLifecycle() == null ? DocumentLifecycle.DRAFT : d.getLifecycle();
        if (lc != DocumentLifecycle.DRAFT && d.getNextReviewDate() != null && d.getNextReviewDate().isBefore(today)) {
            return DisplayStatus.REVIEW_OVERDUE;
        }
        return switch (lc) {
            case DRAFT -> DisplayStatus.DRAFT;
            case IN_FORCE -> DisplayStatus.IN_FORCE;
            case IN_REVISION -> DisplayStatus.IN_REVISION;
        };
    }

    private DocumentDto.DocumentResponse toResponse(Document d, LocalDate today) {
        return new DocumentDto.DocumentResponse(
                d.getId(), d.getTenantId(), d.getCode(), d.getTitle(), d.getDescription(),
                d.getType(), d.getStatus(), d.getOwnerId(), d.getCurrentVersionId(),
                d.isMandatoryRead(), d.getCreatedAt(), d.getUpdatedAt(),
                d.getVersions().stream().map(DocumentService::toVersionResponse).toList(),
                d.getFolderId(), d.getProcessId(), d.getLifecycle(), displayStatus(d, today),
                d.getNextReviewDate());
    }

    static DocumentDto.VersionResponse toVersionResponse(DocumentVersion v) {
        return new DocumentDto.VersionResponse(
                v.getId(), v.getDocument().getId(), v.getVersionNumber(),
                v.getContent(), v.getContentUri(), v.getContentHash(), v.getChangeNote(),
                v.getStatus(), v.getAuthorId(), v.getApprovedBy(), v.getApprovedAt(),
                v.getPublishedAt(), v.getBlockchainTxHash(),
                v.getCreatedAt(), v.getUpdatedAt(),
                v.getRejectedBy(), v.getRejectedAt(), v.getRejectionReason(),
                fileInfo(v),
                v.getSignatories().stream()
                        .map(s -> new DocumentDto.SignatoryView(s.getPosition(), s.getSignerId(), s.getSignerName(),
                                s.getDecision(), s.getDecidedAt(), s.getComment()))
                        .toList());
    }

    private static DocumentDto.FileInfo fileInfo(DocumentVersion v) {
        DocumentBlob b = v.getBlob();
        if (b != null) {
            String type = b.getContentType();
            boolean previewable = "application/pdf".equals(type) || type.startsWith("image/")
                    || DocumentFiles.isTextType(type);
            return new DocumentDto.FileInfo(v.getFileName(), type, b.getOriginalSize(), previewable, false);
        }
        if (v.getFilePurgedAt() != null) {
            return new DocumentDto.FileInfo(v.getFileName(), null, 0, false, true);
        }
        return null;
    }
}
