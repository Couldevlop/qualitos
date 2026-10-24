package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import com.openlab.qualitos.quality.nonconformity.storage.UploadedBinaryGuard;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Documents joints aux NC fournisseur (8D du fournisseur, preuves de CAPA) et
 * aux certificats (§4.6).
 *
 * <p>Même motif que les documents d'audit, parce qu'il est éprouvé : métadonnée
 * en base, binaire dans le stockage objet sous une clé tenantisée construite par
 * le serveur, URL présignée à la lecture, propriétaire déclaré au balayeur
 * d'orphelins, gardes communes de {@link UploadedBinaryGuard}.
 *
 * <p>Bornes : dix mégaoctets par fichier, vingt documents et cinquante
 * mégaoctets par NC ou par certificat. Verrou : une NC close (résolue ou
 * rejetée) ne reçoit ni ne perd plus de pièce — son dossier dit ce sur quoi la
 * clôture s'est prononcée. La rouvrir rouvre son dossier.
 */
@Service
@Transactional
public class SupplierDocumentService {

    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    static final int MAX_COUNT = 20;
    static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /** Liste blanche type → extension : l'extension de la clé vient du type validé. */
    static final Map<String, String> ALLOWED_TYPES = buildAllowedTypes();

    /** Les natures qu'admet chaque porteur : un 8D n'a rien à faire sur un certificat. */
    static final Map<SupplierDocumentOwnerType, Set<SupplierDocumentCategory>> CATEGORIES = Map.of(
            SupplierDocumentOwnerType.NON_CONFORMITY, EnumSet.of(
                    SupplierDocumentCategory.EIGHT_D,
                    SupplierDocumentCategory.CAPA_EVIDENCE,
                    SupplierDocumentCategory.OTHER),
            SupplierDocumentOwnerType.CERTIFICATE, EnumSet.of(
                    SupplierDocumentCategory.CERTIFICATE,
                    SupplierDocumentCategory.OTHER));

    private static final Set<NonConformityStatus> CLOSED =
            EnumSet.of(NonConformityStatus.RESOLVED, NonConformityStatus.REJECTED);

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

    private final SupplierDocumentRepository documents;
    private final SupplierRepository suppliers;
    private final SupplierNonConformityRepository ncs;
    private final SupplierCertificateRepository certificates;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public SupplierDocumentService(SupplierDocumentRepository documents,
                                   SupplierRepository suppliers,
                                   SupplierNonConformityRepository ncs,
                                   SupplierCertificateRepository certificates,
                                   ObjectProvider<ObjectStorage> storageProvider,
                                   AuditEventService auditEvents) {
        this.documents = documents;
        this.suppliers = suppliers;
        this.ncs = ncs;
        this.certificates = certificates;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<SupplierDocumentDto.ListItem> list(UUID supplierId, SupplierDocumentOwnerType ownerType,
                                                   UUID ownerId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        resolveOwner(tenantId, supplierId, ownerType, ownerId);
        return documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(tenantId, ownerType, ownerId)
                .stream()
                .map(d -> toListItem(d, storage.presignGet(d.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    public SupplierDocumentDto.Response upload(UUID supplierId, SupplierDocumentOwnerType ownerType, UUID ownerId,
                                               SupplierDocumentCategory category, String contentType,
                                               String originalFilename, byte[] content, UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        Owner owner = resolveOpenOwner(tenantId, supplierId, ownerType, ownerId);

        SupplierDocumentCategory effective = category == null ? defaultCategory(ownerType) : category;
        if (!CATEGORIES.get(ownerType).contains(effective)) {
            throw new SupplierDocumentValidationException("Category " + effective
                    + " is not allowed here (allowed: " + CATEGORIES.get(ownerType) + ")");
        }
        if (content == null || content.length == 0) {
            throw new SupplierDocumentValidationException("Empty document upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new SupplierDocumentTooLargeException("A single file cannot exceed "
                    + (MAX_SIZE_BYTES / (1024 * 1024)) + " MB");
        }
        String normalizedType = UploadedBinaryGuard.normalizeType(contentType);
        String ext = UploadedBinaryGuard.extensionOf(ALLOWED_TYPES, normalizedType);
        if (ext == null) {
            throw new SupplierDocumentValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le type déclaré est falsifiable : un exécutable renommé en .pdf passerait.
        if (!UploadedBinaryGuard.magicBytesMatch(normalizedType, content)) {
            throw new SupplierDocumentValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }
        if (documents.countByTenantIdAndOwnerTypeAndOwnerId(tenantId, ownerType, ownerId) >= MAX_COUNT) {
            throw new SupplierDocumentTooLargeException("At most " + MAX_COUNT
                    + " documents can be attached here: remove one before adding another");
        }
        if (documents.sumSizeBytes(tenantId, ownerType, ownerId) + content.length > MAX_TOTAL_BYTES) {
            throw new SupplierDocumentTooLargeException("Adding this file would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of documents allowed here");
        }

        // Clé construite de bout en bout à partir d'identifiants tenus par la
        // plateforme : aucune séquence de remontée de chemin ne peut s'y glisser.
        String key = "tenants/" + tenantId + "/suppliers/" + supplierId + "/" + segment(ownerType) + "/"
                + ownerId + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnée d'abord, binaire ensuite : si le dépôt échoue, la transaction
        // annule la ligne. L'ordre inverse laisserait un binaire orphelin.
        SupplierDocument doc = new SupplierDocument();
        doc.setTenantId(tenantId);
        doc.setSupplierId(supplierId);
        doc.setOwnerType(ownerType);
        doc.setOwnerId(ownerId);
        doc.setCategory(effective);
        doc.setObjectKey(key);
        doc.setContentType(normalizedType);
        doc.setSizeBytes(content.length);
        doc.setOriginalFilename(UploadedBinaryGuard.sanitizeFilename(originalFilename));
        doc.setUploadedBy(uploadedBy);
        SupplierDocument saved = documents.save(doc);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "supplier.document.uploaded", saved,
                "Document joint " + owner.to());
        return toResponse(saved);
    }

    /**
     * Retire un document. Il est cherché POUR ce porteur : l'identifiant d'un
     * document joint ailleurs ne doit pas le faire disparaître d'un dossier voisin.
     */
    public void delete(UUID supplierId, SupplierDocumentOwnerType ownerType, UUID ownerId,
                       UUID documentId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        Owner owner = resolveOpenOwner(tenantId, supplierId, ownerType, ownerId);
        SupplierDocument doc = documents.findByIdAndTenantIdAndOwnerTypeAndOwnerId(
                        documentId, tenantId, ownerType, ownerId)
                .orElseThrow(() -> new SupplierChildNotFoundException("Document", documentId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite.
        documents.delete(doc);
        storage.delete(doc.getObjectKey());
        trace(tenantId, removedBy, "supplier.document.removed", doc,
                "Document retiré " + owner.from());
    }

    /**
     * Tous les documents fournisseur du tenant, pour la vue « Documents
     * fournisseurs » de la GED : chacun nommé par son fournisseur et son porteur,
     * avec un lien de lecture présigné. Fournisseurs, NC et certificats de la
     * page sont chargés en une requête chacun.
     */
    @Transactional(readOnly = true)
    public Page<SupplierDocumentDto.CatalogItem> listAll(UUID supplierId, SupplierDocumentOwnerType ownerType,
                                                         SupplierDocumentCategory category, String text,
                                                         Pageable pageable) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        Specification<SupplierDocument> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (supplierId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        }
        if (ownerType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerType"), ownerType));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (text != null && !text.isBlank()) {
            // Borné : une recherche de plusieurs kilo-octets n'est pas une recherche.
            String needle = text.strip();
            needle = needle.length() > 200 ? needle.substring(0, 200) : needle;
            String pattern = "%" + escapeLike(needle.toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("originalFilename")), pattern, '\\'));
        }
        Pageable sorted = pageable.getSort().isSorted() || pageable.isUnpaged() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc("createdAt")));
        Page<SupplierDocument> page = documents.findAll(spec, sorted);

        List<SupplierDocument> rows = page.getContent();
        Map<UUID, Supplier> owners = suppliers.findAllById(rows.stream().map(SupplierDocument::getSupplierId)
                        .distinct().toList()).stream()
                .filter(s -> s.getTenantId().equals(tenantId))
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        Map<UUID, String> labels = new HashMap<>();
        ncs.findAllById(ownerIds(rows, SupplierDocumentOwnerType.NON_CONFORMITY)).stream()
                .filter(n -> n.getTenantId().equals(tenantId))
                .forEach(n -> labels.put(n.getId(), n.getPartNumber()));
        certificates.findAllById(ownerIds(rows, SupplierDocumentOwnerType.CERTIFICATE)).stream()
                .filter(c -> c.getTenantId().equals(tenantId))
                .forEach(c -> labels.put(c.getId(), c.getStandardCode()
                        + (c.getReference() == null ? "" : " — " + c.getReference())));

        return page.map(d -> {
            Supplier s = owners.get(d.getSupplierId());
            URL url = storage.presignGet(d.getObjectKey(), PRESIGN_TTL);
            return new SupplierDocumentDto.CatalogItem(d.getId(), d.getSupplierId(),
                    s == null ? null : s.getCode(), s == null ? null : s.getName(),
                    d.getOwnerType(), d.getOwnerId(), labels.get(d.getOwnerId()),
                    d.getCategory(), d.getContentType(), d.getSizeBytes(), d.getOriginalFilename(),
                    d.getUploadedBy(), d.getCreatedAt(), url == null ? null : url.toString());
        });
    }

    private static List<UUID> ownerIds(List<SupplierDocument> rows, SupplierDocumentOwnerType type) {
        return rows.stream().filter(d -> d.getOwnerType() == type).map(SupplierDocument::getOwnerId)
                .distinct().toList();
    }

    /** Un « % » ou un « _ » tapés dans la recherche se cherchent tels quels. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Les documents d'une NC pour sa fiche imprimée : métadonnée seule, sans URL. */
    @Transactional(readOnly = true)
    public List<SupplierDocument> forReport(UUID tenantId, UUID ncId) {
        return documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(
                tenantId, SupplierDocumentOwnerType.NON_CONFORMITY, ncId);
    }

    /** Nombre de documents de chaque porteur de la liste ; absent = aucun. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> countByOwners(UUID tenantId, SupplierDocumentOwnerType ownerType,
                                         Collection<UUID> ownerIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (ownerIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : documents.countByOwners(tenantId, ownerType, ownerIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Retire les documents d'un certificat qu'on supprime : la base n'a pas de cascade pour eux. */
    public void purgeOwner(UUID tenantId, SupplierDocumentOwnerType ownerType, UUID ownerId) {
        purge(documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(tenantId, ownerType, ownerId));
    }

    /** Retire tous les documents d'un fournisseur qu'on supprime, binaires compris. */
    public void purgeSupplier(UUID tenantId, UUID supplierId) {
        purge(documents.findByTenantIdAndSupplierId(tenantId, supplierId));
    }

    private void purge(List<SupplierDocument> docs) {
        if (docs.isEmpty()) {
            return;
        }
        documents.deleteAll(docs);
        // Stockage coupé : les lignes partent quand même, le balayeur d'orphelins
        // reprendra les binaires quand il reviendra.
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage != null) {
            docs.forEach(d -> storage.delete(d.getObjectKey()));
        }
    }

    // --- garde-fous ---

    /** Un porteur, et la façon de le nommer dans le journal (« joint à … », « retiré de … »). */
    private record Owner(String to, String from) {}

    private Owner resolveOwner(UUID tenantId, UUID supplierId, SupplierDocumentOwnerType ownerType, UUID ownerId) {
        return resolve(tenantId, supplierId, ownerType, ownerId, false);
    }

    private Owner resolveOpenOwner(UUID tenantId, UUID supplierId, SupplierDocumentOwnerType ownerType,
                                   UUID ownerId) {
        return resolve(tenantId, supplierId, ownerType, ownerId, true);
    }

    private Owner resolve(UUID tenantId, UUID supplierId, SupplierDocumentOwnerType ownerType, UUID ownerId,
                          boolean forWrite) {
        Supplier supplier = suppliers.findById(supplierId)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        return switch (ownerType) {
            case NON_CONFORMITY -> {
                SupplierNonConformity nc = ncs.findById(ownerId)
                        .filter(n -> n.getTenantId().equals(tenantId) && n.getSupplierId().equals(supplierId))
                        .orElseThrow(() -> new SupplierChildNotFoundException("Non-conformity", ownerId));
                if (forWrite && CLOSED.contains(nc.getStatus())) {
                    throw new SupplierStateException(
                            "The non-conformity is closed (" + nc.getStatus() + "): its documents are locked");
                }
                String name = "NC fournisseur " + supplier.getCode()
                        + (nc.getPartNumber() == null ? "" : " / " + nc.getPartNumber());
                yield new Owner("à la " + name, "de la " + name);
            }
            case CERTIFICATE -> {
                SupplierCertificate c = certificates.findById(ownerId)
                        .filter(x -> x.getTenantId().equals(tenantId) && x.getSupplierId().equals(supplierId))
                        .orElseThrow(() -> new SupplierChildNotFoundException("Certificate", ownerId));
                String name = "certificat " + c.getStandardCode() + " du fournisseur " + supplier.getCode();
                yield new Owner("au " + name, "du " + name);
            }
        };
    }

    private static SupplierDocumentCategory defaultCategory(SupplierDocumentOwnerType ownerType) {
        return ownerType == SupplierDocumentOwnerType.CERTIFICATE
                ? SupplierDocumentCategory.CERTIFICATE
                : SupplierDocumentCategory.OTHER;
    }

    private static String segment(SupplierDocumentOwnerType ownerType) {
        return ownerType == SupplierDocumentOwnerType.CERTIFICATE ? "certificates" : "non-conformities";
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
     * figure — il dit QUELLE pièce — mais jamais la clé d'objet.
     */
    private void trace(UUID tenantId, UUID actor, String action, SupplierDocument doc, String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "supplier_document",
                doc.getId(),
                summary,
                payload(doc),
                null,
                null));
    }

    private static String payload(SupplierDocument d) {
        return "{\"supplierId\":\"" + d.getSupplierId()
                + "\",\"ownerType\":\"" + d.getOwnerType()
                + "\",\"ownerId\":\"" + d.getOwnerId()
                + "\",\"category\":\"" + d.getCategory()
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

    private static SupplierDocumentDto.Response toResponse(SupplierDocument d) {
        return new SupplierDocumentDto.Response(d.getId(), d.getSupplierId(), d.getOwnerType(), d.getOwnerId(),
                d.getCategory(), d.getContentType(), d.getSizeBytes(), d.getOriginalFilename(),
                d.getUploadedBy(), d.getCreatedAt());
    }

    private static SupplierDocumentDto.ListItem toListItem(SupplierDocument d, URL url) {
        return new SupplierDocumentDto.ListItem(d.getId(), d.getSupplierId(), d.getOwnerType(), d.getOwnerId(),
                d.getCategory(), d.getContentType(), d.getSizeBytes(), d.getOriginalFilename(),
                d.getUploadedBy(), d.getCreatedAt(), url == null ? null : url.toString());
    }
}
