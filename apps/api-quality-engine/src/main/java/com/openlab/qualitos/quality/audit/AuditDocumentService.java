package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Documents joints à un audit (§4.4).
 *
 * <p>Même motif que les preuves CAPA et APQP, parce qu'il est éprouvé : métadonnée
 * en base, binaire dans le stockage objet sous une clé tenantisée construite par
 * le serveur, URL présignée à la lecture, propriétaire déclaré au balayeur
 * d'orphelins, et les gardes communes de {@link UploadedBinaryGuard} — liste
 * blanche, signature binaire, nom assaini. Aucun nouveau mécanisme de stockage.
 *
 * <p>Les bornes : dix mégaoctets par fichier, vingt documents et cinquante
 * mégaoctets par audit. Le verrou : on joint et on retire tant que l'audit vit ;
 * plus rien sur un audit annulé ni une fois le rapport émis — un dossier qu'on
 * regarnit après la remise du rapport ne prouve plus ce que le rapport affirme.
 */
@Service
@Transactional
public class AuditDocumentService {

    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    static final int MAX_COUNT = 20;
    static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /** Liste blanche type → extension : l'extension de la clé vient du type validé. */
    static final Map<String, String> ALLOWED_TYPES = buildAllowedTypes();

    private static Map<String, String> buildAllowedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("application/pdf", "pdf");
        types.put("image/jpeg", "jpg");
        types.put("image/png", "png");
        types.put("image/webp", "webp");
        types.put("image/heic", "heic");
        types.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        types.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        types.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        return Map.copyOf(types);
    }

    private final AuditDocumentRepository documents;
    private final AuditPlanRepository plans;
    private final AuditReportRepository reports;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public AuditDocumentService(AuditDocumentRepository documents,
                                AuditPlanRepository plans,
                                AuditReportRepository reports,
                                ObjectProvider<ObjectStorage> storageProvider,
                                AuditEventService auditEvents) {
        this.documents = documents;
        this.plans = plans;
        this.reports = reports;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<AuditDocumentDto.ListItem> list(UUID planId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadPlan(planId, tenantId);
        return documents.findByTenantIdAndPlanIdOrderByCreatedAtAsc(tenantId, planId).stream()
                .map(d -> toListItem(d, storage.presignGet(d.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    public AuditDocumentDto.Response upload(UUID planId, String contentType, String originalFilename,
                                            byte[] content, UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        AuditPlan plan = loadOpenPlan(planId, tenantId);

        if (content == null || content.length == 0) {
            throw new AuditDocumentValidationException("Empty document upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new AuditDocumentTooLargeException("A single file cannot exceed "
                    + (MAX_SIZE_BYTES / (1024 * 1024)) + " MB");
        }
        String normalizedType = UploadedBinaryGuard.normalizeType(contentType);
        String ext = UploadedBinaryGuard.extensionOf(ALLOWED_TYPES, normalizedType);
        if (ext == null) {
            throw new AuditDocumentValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le type déclaré est falsifiable : un exécutable renommé en .pdf passerait.
        if (!UploadedBinaryGuard.magicBytesMatch(normalizedType, content)) {
            throw new AuditDocumentValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }
        if (documents.countByTenantIdAndPlanId(tenantId, planId) >= MAX_COUNT) {
            throw new AuditDocumentTooLargeException("An audit carries at most " + MAX_COUNT
                    + " documents: remove one before adding another");
        }
        if (documents.sumSizeBytes(tenantId, planId) + content.length > MAX_TOTAL_BYTES) {
            throw new AuditDocumentTooLargeException("Adding this file would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of documents allowed on an audit");
        }

        // Clé construite de bout en bout à partir d'identifiants tenus par la
        // plateforme : aucune séquence de remontée de chemin ne peut s'y glisser.
        String key = "tenants/" + tenantId + "/audits/" + plan.getId() + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnée d'abord, binaire ensuite : si le dépôt échoue, la transaction
        // annule la ligne. L'ordre inverse laisserait un binaire orphelin.
        AuditDocument doc = new AuditDocument();
        doc.setTenantId(tenantId);
        doc.setPlanId(plan.getId());
        doc.setObjectKey(key);
        doc.setContentType(normalizedType);
        doc.setSizeBytes(content.length);
        doc.setOriginalFilename(UploadedBinaryGuard.sanitizeFilename(originalFilename));
        doc.setUploadedBy(uploadedBy);
        AuditDocument saved = documents.save(doc);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "audit.document.uploaded", saved,
                "Document joint à l'audit " + plan.getReference());
        return toResponse(saved);
    }

    /**
     * Retire un document. Il est cherché POUR cet audit : l'identifiant d'un
     * document joint ailleurs ne doit pas le faire disparaître d'un audit voisin.
     */
    public void delete(UUID planId, UUID documentId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        AuditPlan plan = loadOpenPlan(planId, tenantId);
        AuditDocument doc = documents.findByIdAndTenantIdAndPlanId(documentId, tenantId, planId)
                .orElseThrow(() -> new AuditDocumentNotFoundException(documentId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite.
        documents.delete(doc);
        storage.delete(doc.getObjectKey());
        // Le retrait est la seule opération qui fait disparaître une pièce d'un
        // dossier d'audit : sans trace, le dossier ne dirait plus ce qu'il a porté.
        trace(tenantId, removedBy, "audit.document.removed", doc,
                "Document retiré de l'audit " + plan.getReference());
    }

    // --- garde-fous ---

    private AuditPlan loadPlan(UUID planId, UUID tenantId) {
        return plans.findByIdAndTenantId(planId, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(planId));
    }

    private AuditPlan loadOpenPlan(UUID planId, UUID tenantId) {
        AuditPlan plan = loadPlan(planId, tenantId);
        if (plan.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("Cannot change the documents of a cancelled audit");
        }
        if (reports.existsByPlanIdAndStatus(plan.getId(), AuditReportStatus.ISSUED)) {
            throw new AuditStateException("The audit report has been issued: its documents are locked");
        }
        return plan;
    }

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
     * Inscrit l'opération au journal chaîné du tenant (§11.5). Le nom d'origine y
     * figure — il dit QUELLE pièce — mais jamais la clé d'objet, qui donnerait un
     * chemin de stockage dans un journal qui se relit et s'exporte.
     */
    private void trace(UUID tenantId, UUID actor, String action, AuditDocument doc, String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "audit_document",
                doc.getId(),
                summary,
                payload(doc),
                null,
                null));
    }

    private static String payload(AuditDocument d) {
        return "{\"planId\":\"" + d.getPlanId()
                + "\",\"contentType\":\"" + d.getContentType()
                + "\",\"sizeBytes\":" + d.getSizeBytes()
                + ",\"originalFilename\":" + jsonString(d.getOriginalFilename()) + "}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static AuditDocumentDto.Response toResponse(AuditDocument d) {
        return new AuditDocumentDto.Response(d.getId(), d.getPlanId(), d.getContentType(),
                d.getSizeBytes(), d.getOriginalFilename(), d.getUploadedBy(), d.getCreatedAt());
    }

    private static AuditDocumentDto.ListItem toListItem(AuditDocument d, URL url) {
        return new AuditDocumentDto.ListItem(d.getId(), d.getPlanId(), d.getContentType(),
                d.getSizeBytes(), d.getOriginalFilename(), d.getUploadedBy(), d.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
