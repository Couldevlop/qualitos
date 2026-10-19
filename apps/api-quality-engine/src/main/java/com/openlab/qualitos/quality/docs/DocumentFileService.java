package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Les fichiers des versions de documents (ADR 0094).
 *
 * <p><b>Un contenu, un objet.</b> Le binaire est rangé sous une clé dérivée de son
 * empreinte SHA-256, propre au client : redéposer le même fichier, ou ouvrir une
 * version dont le fichier n'a pas changé, ne consomme aucun octet de plus. Une
 * ligne de {@code document_blobs} par contenu, partagée par les versions qui le
 * portent ; l'objet n'est retiré que lorsque plus aucune version ne le désigne.
 *
 * <p><b>Compression au cas par cas</b> : voir {@link DocumentFiles}. Le stockage
 * resservant l'objet avec son {@code Content-Encoding}, le navigateur reçoit le
 * fichier d'origine sans que le serveur n'ait à le relayer.
 *
 * <p><b>Conservation.</b> Au terme de la durée réglée par le client, le fichier
 * d'une version obsolète est retiré ; son empreinte et son ancrage restent, et la
 * version continue de figurer dans l'historique.
 */
@Service
@Transactional
public class DocumentFileService {

    private static final Logger log = LoggerFactory.getLogger(DocumentFileService.class);

    static final Duration LINK_TTL = Duration.ofMinutes(10);
    static final int PURGE_BATCH = 100;
    private static final long MB = 1024L * 1024;

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentBlobRepository blobs;
    private final DocumentCatalogService catalog;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;
    private final Clock clock;

    public DocumentFileService(DocumentRepository documents,
                               DocumentVersionRepository versions,
                               DocumentBlobRepository blobs,
                               DocumentCatalogService catalog,
                               ObjectProvider<ObjectStorage> storageProvider,
                               AuditEventService auditEvents,
                               Clock clock) {
        this.documents = documents;
        this.versions = versions;
        this.blobs = blobs;
        this.catalog = catalog;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
        this.clock = clock;
    }

    // ---------- dépôt ----------

    /** Joint un fichier à une version en brouillon ; un fichier déjà joint est remplacé. */
    public DocumentDto.VersionResponse upload(UUID documentId, UUID versionId, String declaredType,
                                              String originalFilename, byte[] content) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        Document d = documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new DocumentStateException("Archived document cannot be modified");
        }
        DocumentVersion v = versions.findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new DocumentVersionNotFoundException(versionId));
        if (v.getStatus() != VersionStatus.DRAFT) {
            throw new DocumentStateException("A file can only be attached to a DRAFT version");
        }

        if (content == null || content.length == 0) {
            throw new DocumentFileRejectedException("Empty file");
        }
        DocumentSettingsRow settings = catalog.settingsOf(tenantId);
        if (content.length > settings.getMaxFileMb() * MB) {
            throw new DocumentFileTooLargeException("A document file cannot exceed " + settings.getMaxFileMb() + " MB");
        }
        String filename = UploadedBinaryGuard.sanitizeFilename(originalFilename);
        String type = DocumentFiles.resolveType(declaredType, filename);
        if (type == null) {
            throw new DocumentFileRejectedException("Unsupported file type: " + declaredType
                    + " (allowed: " + DocumentFiles.ALLOWED_TYPES.values().stream().distinct().toList() + ")");
        }
        // Le type déclaré est falsifiable : un exécutable renommé en .pdf passerait.
        if (!DocumentFiles.contentMatches(type, content)) {
            throw new DocumentFileRejectedException("File content does not match its type '" + type + "'");
        }

        String sha = DocumentFiles.sha256(content);
        DocumentBlob blob = blobs.findByTenantIdAndSha256(tenantId, sha)
                .orElseGet(() -> store(tenantId, storage, settings, sha, type, content));

        DocumentBlob previous = v.getBlob();
        v.setBlob(blob);
        v.setFileName(filename != null ? filename : "document." + DocumentFiles.ALLOWED_TYPES.get(type));
        v.setContentHash(sha);
        v.setFilePurgedAt(null);
        DocumentVersion saved = versions.saveAndFlush(v);
        if (previous != null && !previous.getId().equals(blob.getId())) {
            releaseIfUnreferenced(previous);
        }
        trace(tenantId, "document.file.uploaded", d.getId(),
                "Fichier joint à " + d.getCode() + " v" + v.getVersionNumber(),
                "{\"versionId\":\"" + v.getId() + "\",\"sha256\":\"" + sha
                        + "\",\"sizeBytes\":" + content.length + ",\"storedBytes\":" + blob.getStoredSize() + "}");
        return DocumentService.toVersionResponse(saved);
    }

    /**
     * Un contenu nouveau chez ce client : la ligne d'abord, l'objet ensuite. Si la
     * transaction échoue après le dépôt, l'objet reste sans ligne : le balayage des
     * orphelins le retirera. L'ordre inverse laisserait une ligne sans objet.
     */
    private DocumentBlob store(UUID tenantId, ObjectStorage storage, DocumentSettingsRow settings,
                               String sha, String type, byte[] content) {
        DocumentFiles.Encoded encoded = DocumentFiles.encodeForStorage(type, content);
        if (settings.getQuotaMb() != null
                && blobs.sumStoredBytes(tenantId) + encoded.bytes().length > settings.getQuotaMb() * MB) {
            throw new DocumentFileTooLargeException("The document storage quota of "
                    + settings.getQuotaMb() + " MB would be exceeded");
        }
        DocumentBlob blob = new DocumentBlob();
        blob.setTenantId(tenantId);
        blob.setSha256(sha);
        // Clé faite d'identifiants tenus par la plateforme : aucun nom de client n'y
        // entre. Le suffixe aléatoire sépare deux vies d'un même contenu : un objet
        // libéré dont la suppression attend la fin de sa transaction ne doit pas
        // emporter celui qu'un nouveau dépôt du même fichier vient d'écrire.
        blob.setObjectKey("tenants/" + tenantId + "/documents/blobs/" + sha + "-"
                + UUID.randomUUID().toString().substring(0, 8));
        blob.setContentType(type);
        blob.setOriginalSize(content.length);
        blob.setStoredSize(encoded.bytes().length);
        blob.setContentEncoding(encoded.contentEncoding());
        DocumentBlob saved = blobs.saveAndFlush(blob);
        storage.put(saved.getObjectKey(), type, encoded.contentEncoding(), encoded.bytes());
        return saved;
    }

    // ---------- lecture ----------

    /**
     * Un lien présigné vers le fichier d'une version : en pièce jointe pour le
     * téléchargement, en ligne pour l'aperçu. Le serveur ne relaie pas les octets.
     */
    @Transactional(readOnly = true)
    public DocumentDto.FileLink link(UUID documentId, UUID versionId, boolean download) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        DocumentVersion v = versions.findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new DocumentVersionNotFoundException(versionId));
        DocumentBlob blob = v.getBlob();
        if (blob == null) {
            throw new DocumentStateException(v.getFilePurgedAt() != null
                    ? "The file of this version was removed at the end of its retention period"
                    : "This version has no file");
        }
        String url = storage.presignGet(blob.getObjectKey(), LINK_TTL, v.getFileName(), download).toString();
        return new DocumentDto.FileLink(url, v.getFileName(), blob.getContentType(), blob.getOriginalSize(),
                LINK_TTL.toSeconds());
    }

    @Transactional(readOnly = true)
    public DocumentDto.StorageUsage usage() {
        UUID tenantId = requireTenantId();
        DocumentSettingsRow s = catalog.settingsOf(tenantId);
        return new DocumentDto.StorageUsage(blobs.countFiles(tenantId), versions.sumLogicalBytes(tenantId),
                blobs.sumOriginalBytes(tenantId), blobs.sumStoredBytes(tenantId),
                s.getQuotaMb() == null ? null : s.getQuotaMb() * MB);
    }

    // ---------- conservation ----------

    /**
     * Retire les fichiers des versions obsolètes d'un client au-delà de leur durée
     * de conservation, par lot. Le tenant doit être posé par l'appelant.
     *
     * @return le nombre de versions dont le fichier a été retiré
     */
    public int purgeExpired(UUID tenantId, int retentionYears) {
        Instant now = clock.instant();
        Instant before = now.atZone(ZoneOffset.UTC).minusYears(retentionYears).toInstant();
        List<DocumentVersion> due = versions.findPurgeable(tenantId, VersionStatus.OBSOLETE, before,
                PageRequest.of(0, PURGE_BATCH));
        for (DocumentVersion v : due) {
            DocumentBlob blob = v.getBlob();
            v.setBlob(null);
            v.setFilePurgedAt(now);
            versions.saveAndFlush(v);
            releaseIfUnreferenced(blob);
            trace(tenantId, "document.file.purged", v.getDocument().getId(),
                    "Fichier de " + v.getDocument().getCode() + " v" + v.getVersionNumber()
                            + " retiré au terme de sa conservation",
                    "{\"versionId\":\"" + v.getId() + "\",\"sha256\":\"" + v.getContentHash() + "\"}");
        }
        return due.size();
    }

    /**
     * Retire le contenu que plus aucune version ne désigne. L'objet n'est supprimé
     * qu'APRÈS la validation de la transaction : supprimé avant, une transaction
     * annulée laisserait une ligne pointant vers un objet disparu.
     */
    void releaseIfUnreferenced(DocumentBlob blob) {
        if (versions.existsByBlobId(blob.getId())) {
            return;
        }
        blobs.delete(blob);
        String key = blob.getObjectKey();
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(storage, key);
                }
            });
        } else {
            deleteQuietly(storage, key);
        }
    }

    private static void deleteQuietly(ObjectStorage storage, String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            // L'objet restera sans ligne : le balayage des orphelins le reprendra.
            log.warn("[documents] objet {} non supprimé : {}", key, e.getMessage());
        }
    }

    // ---------- outils ----------

    private ObjectStorage requireStorage() {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new StorageDisabledException();
        }
        return storage;
    }

    private static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /**
     * Journal chaîné du tenant (§11.5). L'empreinte y figure — c'est elle qui
     * prouve QUEL fichier — mais jamais la clé d'objet.
     */
    private void trace(UUID tenantId, String action, UUID documentId, String summary, String payload) {
        UUID actor = CurrentUser.userId().orElse(null);
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "document",
                documentId,
                summary,
                payload,
                null,
                null));
    }
}
