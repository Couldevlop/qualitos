package com.openlab.qualitos.quality.fives;

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
 * Les photos prises sur le terrain pendant un audit 5S (§3.2, §15.3).
 *
 * <p>Même motif que les preuves PDCA, CAPA et APQP, parce qu'il est éprouvé :
 * métadonnée en base, binaire dans le stockage objet sous clé tenantisée, URL
 * présignée à la lecture, propriétaire déclaré au balayeur d'orphelins.
 *
 * <p>Trois écarts, et eux seuls, dictés par le terrain :
 *
 * <ul>
 *   <li><b>On vise le pilier par son nom</b>, pas la ligne de notation par son
 *       identifiant. Hors ligne la notation attend dans la file et n'a aucun
 *       identifiant serveur ; le pilier, lui, est connu avant le réseau. C'est
 *       cette adresse-là qui rend la prise de vue rejouable.</li>
 *   <li><b>Une photo arrive encore sur un audit terminé.</b> L'auditeur
 *       photographie en zone blanche, puis clôt son audit en rentrant : la file
 *       se vide après la clôture. Refuser ces photos détruirait en silence la
 *       preuve de ce qui a été vu, alors qu'elle a bien été prise pendant la
 *       visite. Seul un audit annulé n'accepte plus rien.</li>
 *   <li><b>Retirer, en revanche, s'arrête à la clôture.</b> Un dossier dont on
 *       peut faire disparaître les pièces après signature ne prouve plus rien.</li>
 * </ul>
 *
 * <p>Images seulement : ce que la caméra d'un téléphone produit. Un tableur
 * n'est pas le constat d'une zone encombrée.
 */
@Service
@Transactional
public class FiveSItemPhotoService {

    /** Plafond par photo — double rempart avec la limite multipart de Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Cinq photos par pilier.
     *
     * <p>Une zone encombrée se montre sous plusieurs angles, et l'auditeur n'a
     * pas à choisir sur place laquelle vaut la peine. Au-delà, c'est un reportage.
     */
    static final int MAX_PER_PILLAR = 5;

    /** Poids cumulé par audit : une visite ne doit pas peser un disque. */
    static final long MAX_TOTAL_BYTES = 40L * 1024 * 1024;

    /** TTL des URL présignées de lecture. */
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /**
     * Liste blanche type → extension, images seulement.
     *
     * <p>Le HEIC y figure parce que c'est le format par défaut d'un iPhone : le
     * refuser obligerait l'auditeur à reconfigurer son téléphone avant d'entrer
     * dans l'atelier.
     */
    static final Map<String, String> ALLOWED_TYPES = buildAllowedTypes();

    private static Map<String, String> buildAllowedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("image/jpeg", "jpg");
        types.put("image/png", "png");
        types.put("image/webp", "webp");
        types.put("image/heic", "heic");
        return Map.copyOf(types);
    }

    private final FiveSItemPhotoRepository photos;
    private final FiveSAuditRepository audits;
    private final FiveSAuditItemRepository items;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public FiveSItemPhotoService(FiveSItemPhotoRepository photos,
                                 FiveSAuditRepository audits,
                                 FiveSAuditItemRepository items,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 AuditEventService auditEvents) {
        this.photos = photos;
        this.audits = audits;
        this.items = items;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    /**
     * Les photos de TOUS les piliers de l'audit, en un appel.
     *
     * <p>La fiche affiche une vignette sur chacune des cinq lignes ; une requête
     * par ligne ferait cinq allers et retours pour remplir une seule colonne.
     */
    @Transactional(readOnly = true)
    public List<FiveSItemPhotoDto.ListItem> listForAudit(UUID auditId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadAudit(auditId, tenantId); // 404 si l'audit est absent ou d'un autre client
        return photos.findByTenantIdAndAuditIdOrderByCreatedAtAsc(tenantId, auditId).stream()
                .map(p -> toListItem(p, storage.presignGet(p.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    /** Les photos d'UN pilier, pour la vue agrandie. */
    @Transactional(readOnly = true)
    public List<FiveSItemPhotoDto.ListItem> listForPillar(UUID auditId, FiveSPillar pillar) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadAudit(auditId, tenantId);
        return photos.findByTenantIdAndAuditIdAndPillarOrderByCreatedAtAsc(tenantId, auditId, pillar)
                .stream()
                .map(p -> toListItem(p, storage.presignGet(p.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    /**
     * Verse une photo sur un pilier de l'audit.
     *
     * <p>Le pilier doit porter une note : la photo documente un constat, et un
     * pilier qu'on n'a pas évalué n'en porte aucun. C'est aussi ce qui garantit
     * l'ordre au rejeu de la file — la notation part avant la photo.
     */
    public FiveSItemPhotoDto.Response upload(UUID auditId, FiveSPillar pillar, String contentType,
                                             String originalFilename, byte[] content,
                                             UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        FiveSAudit audit = loadAudit(auditId, tenantId);
        if (audit.getStatus() == FiveSAuditStatus.CANCELLED) {
            throw new FiveSStateException("Cannot attach a photo to a CANCELLED audit");
        }
        requireScoredPillar(auditId, pillar);

        if (content == null || content.length == 0) {
            throw new FiveSItemPhotoValidationException("Empty photo upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new FiveSItemPhotoTooLargeException("A single photo cannot exceed "
                    + (MAX_SIZE_BYTES / (1024 * 1024)) + " MB");
        }

        String normalizedType = UploadedBinaryGuard.normalizeType(contentType);
        String ext = UploadedBinaryGuard.extensionOf(ALLOWED_TYPES, normalizedType);
        if (ext == null) {
            throw new FiveSItemPhotoValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le content-type déclaré est falsifiable : la signature binaire doit
        // correspondre au type annoncé, sinon un exécutable renommé passerait.
        if (!UploadedBinaryGuard.magicBytesMatch(normalizedType, content)) {
            throw new FiveSItemPhotoValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }

        if (photos.countByTenantIdAndAuditIdAndPillar(tenantId, auditId, pillar)
                >= MAX_PER_PILLAR) {
            throw new FiveSItemPhotoTooLargeException("A pillar carries at most " + MAX_PER_PILLAR
                    + " photos: remove one before adding another");
        }
        long total = photos.sumSizeBytes(tenantId, auditId);
        if (total + content.length > MAX_TOTAL_BYTES) {
            throw new FiveSItemPhotoTooLargeException("Adding this photo would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of photos allowed on a 5S audit");
        }

        // La clé est construite de bout en bout à partir d'identifiants tenus par
        // la plateforme — jamais du nom fourni par le client — donc aucune
        // séquence de remontée de chemin ne peut s'y glisser.
        String key = "tenants/" + tenantId + "/fives/" + auditId + "/" + pillar
                + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnée d'abord, binaire ensuite : si le dépôt échoue, la transaction
        // annule la ligne et rien n'est écrit. L'ordre inverse laisserait un
        // binaire orphelin, invisible et facturé.
        FiveSItemPhoto photo = new FiveSItemPhoto();
        photo.setTenantId(tenantId);
        photo.setAuditId(auditId);
        photo.setPillar(pillar);
        photo.setObjectKey(key);
        photo.setContentType(normalizedType);
        photo.setSizeBytes(content.length);
        photo.setOriginalFilename(UploadedBinaryGuard.sanitizeFilename(originalFilename));
        photo.setUploadedBy(uploadedBy);
        FiveSItemPhoto saved = photos.save(photo);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "fives.item-photo.uploaded", saved,
                "Photo versée au pilier " + pillar + " de l'audit 5S zone « " + audit.getZone() + " »");
        return toResponse(saved);
    }

    /**
     * Retire une photo.
     *
     * <p>La photo est cherchée POUR cet audit : passer l'identifiant d'une photo
     * prise ailleurs ne doit pas la faire disparaître d'un audit voisin.
     */
    public void delete(UUID auditId, UUID photoId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        FiveSAudit audit = loadAudit(auditId, tenantId);
        if (audit.getStatus() == FiveSAuditStatus.COMPLETED
                || audit.getStatus() == FiveSAuditStatus.CANCELLED) {
            throw new FiveSStateException("Cannot remove a photo from a "
                    + audit.getStatus() + " audit");
        }

        FiveSItemPhoto photo = photos.findByIdAndTenantIdAndAuditId(photoId, tenantId, auditId)
                .orElseThrow(() -> new FiveSItemPhotoNotFoundException(photoId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite. Si la
        // suppression du binaire échoue, la transaction rétablit la ligne et
        // l'audit reste cohérent plutôt que de pointer vers un objet disparu.
        photos.delete(photo);
        storage.delete(photo.getObjectKey());
        // Le retrait est la seule opération qui fait disparaître une preuve d'un
        // audit. Sans trace, l'audit ne dirait plus ce qu'il a porté.
        trace(tenantId, removedBy, "fives.item-photo.removed", photo,
                "Photo retirée du pilier " + photo.getPillar() + " de l'audit 5S zone « "
                        + audit.getZone() + " »");
    }

    // ---------- garde-fous ----------

    private FiveSAudit loadAudit(UUID auditId, UUID tenantId) {
        return audits.findByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> new FiveSAuditNotFoundException(auditId));
    }

    /**
     * La ligne de notation du pilier, à condition qu'elle existe ET porte une
     * note. L'audit est déjà filtré par client, donc ce contrôle-ci est bien
     * celui qui empêche d'attacher une photo à un pilier jamais évalué.
     */
    private FiveSAuditItem requireScoredPillar(UUID auditId, FiveSPillar pillar) {
        return items.findByAuditIdAndPillar(auditId, pillar)
                .filter(it -> it.getScore() != null)
                .orElseThrow(() -> new FiveSStateException(
                        "Pillar " + pillar + " has no score on this audit"));
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
     * Inscrit l'opération au journal chaîné du client (§11.5). Le nom d'origine y
     * figure — c'est ce qui permet de dire QUELLE photo a été versée ou retirée —
     * mais jamais la clé d'objet : elle donnerait un chemin de stockage dans un
     * journal qui se relit et s'exporte.
     */
    private void trace(UUID tenantId, UUID actor, String action, FiveSItemPhoto photo,
                       String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "fives_item_photo",
                photo.getId(),
                summary,
                payload(photo),
                null,
                null));
    }

    /**
     * JSON minimal, écrit à la main : cinq champs, aucun sérialiseur à convoquer.
     * Le pilier visé en fait partie — sans lui, le journal dirait qu'une photo a
     * quitté l'audit sans dire quel constat elle montrait.
     */
    private static String payload(FiveSItemPhoto p) {
        return "{\"auditId\":\"" + p.getAuditId()
                + "\",\"pillar\":\"" + p.getPillar()
                + "\",\"contentType\":\"" + p.getContentType()
                + "\",\"sizeBytes\":" + p.getSizeBytes()
                + ",\"originalFilename\":" + jsonString(p.getOriginalFilename()) + "}";
    }

    /**
     * Le nom d'origine est déjà assaini à l'entrée, mais un journal ne doit pas
     * dépendre d'une hypothèse tenue ailleurs : guillemets et antislashs sont
     * échappés ici aussi, faute de quoi un nom bien choisi casserait le JSON de
     * la ligne.
     */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static FiveSItemPhotoDto.Response toResponse(FiveSItemPhoto p) {
        return new FiveSItemPhotoDto.Response(
                p.getId(), p.getAuditId(), p.getPillar(), p.getContentType(), p.getSizeBytes(),
                p.getOriginalFilename(), p.getUploadedBy(), p.getCreatedAt());
    }

    private static FiveSItemPhotoDto.ListItem toListItem(FiveSItemPhoto p, URL url) {
        return new FiveSItemPhotoDto.ListItem(
                p.getId(), p.getAuditId(), p.getPillar(), p.getContentType(), p.getSizeBytes(),
                p.getOriginalFilename(), p.getUploadedBy(), p.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
