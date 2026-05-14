package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAcknowledgmentRepository ackRepository;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           DocumentAcknowledgmentRepository ackRepository) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.ackRepository = ackRepository;
    }

    // --- documents ---

    @Transactional(readOnly = true)
    public Page<DocumentDto.DocumentResponse> findAll(DocumentStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Document> page = status != null
                ? documentRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : documentRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentDto.DocumentResponse findById(UUID id) {
        return toResponse(loadDocument(id));
    }

    public DocumentDto.DocumentResponse createDocument(DocumentDto.CreateDocumentRequest req) {
        UUID tenantId = requireTenantId();
        if (documentRepository.existsByTenantIdAndCode(tenantId, req.code())) {
            throw new DocumentCodeConflictException(req.code());
        }

        Document d = new Document();
        d.setTenantId(tenantId);
        d.setCode(req.code());
        d.setTitle(req.title());
        d.setDescription(req.description());
        d.setType(req.type());
        d.setStatus(DocumentStatus.ACTIVE);
        d.setOwnerId(req.ownerId());
        d.setMandatoryRead(req.mandatoryRead());
        d = documentRepository.save(d);

        DocumentVersion v = new DocumentVersion();
        v.setDocument(d);
        v.setVersionNumber(1);
        v.setContent(req.initialContent());
        v.setContentUri(req.initialContentUri());
        v.setContentHash(computeHash(req.initialContent()));
        v.setChangeNote(req.initialChangeNote());
        v.setStatus(VersionStatus.DRAFT);
        v.setAuthorId(req.ownerId());
        versionRepository.save(v);
        d.getVersions().add(v);

        return toResponse(d);
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
        return toResponse(documentRepository.save(d));
    }

    public DocumentDto.DocumentResponse archiveDocument(UUID id) {
        Document d = loadDocument(id);
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new DocumentStateException("Document already archived");
        }
        d.setStatus(DocumentStatus.ARCHIVED);
        return toResponse(documentRepository.save(d));
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
        v.setAuthorId(req.authorId());
        return toVersionResponse(versionRepository.save(v));
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
            v.setContentHash(computeHash(req.content()));
        }
        if (req.contentUri() != null) v.setContentUri(req.contentUri());
        if (req.changeNote() != null) v.setChangeNote(req.changeNote());
        return toVersionResponse(versionRepository.save(v));
    }

    public DocumentDto.VersionResponse submitForReview(UUID documentId, UUID versionId) {
        loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.DRAFT) {
            throw new DocumentStateException("Only DRAFT versions can be submitted for review");
        }
        v.setStatus(VersionStatus.IN_REVIEW);
        return toVersionResponse(versionRepository.save(v));
    }

    public DocumentDto.VersionResponse approveVersion(UUID documentId, UUID versionId,
                                                     DocumentDto.ApprovalRequest req) {
        loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.IN_REVIEW) {
            throw new DocumentStateException("Only IN_REVIEW versions can be approved");
        }
        if (req.approverId().equals(v.getAuthorId())) {
            throw new DocumentStateException("Approver cannot be the author of the version");
        }
        v.setStatus(VersionStatus.APPROVED);
        v.setApprovedBy(req.approverId());
        v.setApprovedAt(Instant.now());
        return toVersionResponse(versionRepository.save(v));
    }

    public DocumentDto.VersionResponse publishVersion(UUID documentId, UUID versionId) {
        Document d = loadDocument(documentId);
        DocumentVersion v = loadVersion(documentId, versionId);
        if (v.getStatus() != VersionStatus.APPROVED) {
            throw new DocumentStateException("Only APPROVED versions can be published");
        }
        // Marquer l'ancienne PUBLISHED en OBSOLETE
        d.getVersions().stream()
                .filter(other -> other.getStatus() == VersionStatus.PUBLISHED)
                .forEach(other -> other.setStatus(VersionStatus.OBSOLETE));

        v.setStatus(VersionStatus.PUBLISHED);
        v.setPublishedAt(Instant.now());
        d.setCurrentVersionId(v.getId());
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
        DocumentAcknowledgment ack = ackRepository.findByVersionIdAndUserId(versionId, req.userId())
                .orElseGet(() -> {
                    DocumentAcknowledgment a = new DocumentAcknowledgment();
                    a.setVersion(v);
                    a.setTenantId(tenantId);
                    a.setUserId(req.userId());
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

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
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

    private DocumentDto.DocumentResponse toResponse(Document d) {
        return new DocumentDto.DocumentResponse(
                d.getId(), d.getTenantId(), d.getCode(), d.getTitle(), d.getDescription(),
                d.getType(), d.getStatus(), d.getOwnerId(), d.getCurrentVersionId(),
                d.isMandatoryRead(), d.getCreatedAt(), d.getUpdatedAt(),
                d.getVersions().stream().map(this::toVersionResponse).toList());
    }

    private DocumentDto.VersionResponse toVersionResponse(DocumentVersion v) {
        return new DocumentDto.VersionResponse(
                v.getId(), v.getDocument().getId(), v.getVersionNumber(),
                v.getContent(), v.getContentUri(), v.getContentHash(), v.getChangeNote(),
                v.getStatus(), v.getAuthorId(), v.getApprovedBy(), v.getApprovedAt(),
                v.getPublishedAt(), v.getBlockchainTxHash(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
