package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion du stockage binaire des photos de Non-Conformités (§4.3).
 * Multi-tenant strict (tenant du JWT, jamais du body). Le binaire vit dans un
 * {@link ObjectStorage} S3-compatible ; la BDD ne porte que les métadonnées + la
 * clé. Quand le stockage est désactivé, lève {@link StorageDisabledException} (503).
 */
@Service
@Transactional
public class NcPhotoService {

    /** Plafond applicatif (10 Mo) — double rempart avec la limite multipart Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** TTL des URLs pré-signées de lecture. */
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /** Whitelist content-type → extension (OWASP : extension déduite du type, pas du nom). */
    static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic"
    );

    private final NcPhotoRepository photoRepository;
    private final NonConformityRepository ncRepository;
    private final ObjectProvider<ObjectStorage> storageProvider;

    public NcPhotoService(NcPhotoRepository photoRepository,
                          NonConformityRepository ncRepository,
                          ObjectProvider<ObjectStorage> storageProvider) {
        this.photoRepository = photoRepository;
        this.ncRepository = ncRepository;
        this.storageProvider = storageProvider;
    }

    public NcPhotoDto.Response upload(UUID ncId, String contentType, String originalFilename, byte[] content) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        NonConformity nc = loadNc(ncId, tenantId);

        if (nc.getStatus() == NcStatus.CLOSED || nc.getStatus() == NcStatus.CANCELLED) {
            throw new NcStateException("Cannot attach a photo to a " + nc.getStatus() + " non-conformity");
        }
        if (content == null || content.length == 0) {
            throw new NcPhotoValidationException("Empty photo upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new NcPhotoTooLargeException(content.length, MAX_SIZE_BYTES);
        }
        String normalizedType = contentType == null ? "" : contentType.toLowerCase().trim();
        String ext = ALLOWED_TYPES.get(normalizedType);
        if (ext == null) {
            throw new NcPhotoValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }

        // M1 — Le content-type déclaré est falsifiable : on vérifie la SIGNATURE binaire
        // (magic bytes) et on exige qu'elle corresponde au type annoncé. Un .exe renommé
        // en .png (ou un content-type menteur) est rejeté en 400.
        if (!magicBytesMatch(normalizedType, content)) {
            throw new NcPhotoValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }

        // Clé tenantisée. L'extension vient du content-type validé, JAMAIS du nom de
        // fichier client (qui peut contenir des séquences de traversée de chemin).
        String key = "tenants/" + tenantId + "/nc/" + ncId + "/" + UUID.randomUUID() + "." + ext;

        // BUG #5 — Ordre métadonnées → binaire. On persiste d'abord la ligne (dans la
        // transaction), puis on écrit l'objet. Si le put échoue, l'exception remonte et la
        // transaction rollback la ligne : aucune métadonnée orpheline. L'objet, lui, n'a
        // jamais été écrit. (Inverser l'ordre laisserait au contraire un binaire orphelin
        // si le save échouait.)
        NcPhoto photo = new NcPhoto();
        photo.setTenantId(tenantId);
        photo.setNcId(ncId);
        photo.setObjectKey(key);
        photo.setContentType(normalizedType);
        photo.setSizeBytes(content.length);
        photo.setOriginalFilename(sanitizeFilename(originalFilename));
        NcPhoto saved = photoRepository.save(photo);

        storage.put(key, normalizedType, content);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NcPhotoDto.ListItem> list(UUID ncId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadNc(ncId, tenantId); // 404 si NC absente / autre tenant
        return photoRepository.findByTenantIdAndNcIdOrderByCreatedAtAsc(tenantId, ncId).stream()
                .map(p -> toListItem(p, storage.presignGet(p.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    public void delete(UUID ncId, UUID photoId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        NcPhoto photo = photoRepository.findByIdAndTenantIdAndNcId(photoId, tenantId, ncId)
                .orElseThrow(() -> new NcPhotoNotFoundException(photoId));
        // BUG #5 (symétrique) — On supprime d'abord la ligne, puis l'objet. Si le delete
        // storage échoue, la transaction rollback la suppression de la ligne : la photo
        // reste cohérente (ligne + objet présents) plutôt que de laisser une ligne pointant
        // vers un objet déjà supprimé. Le storage.delete est idempotent.
        photoRepository.delete(photo);
        storage.delete(photo.getObjectKey());
    }

    // --- helpers ---

    private NonConformity loadNc(UUID ncId, UUID tenantId) {
        return ncRepository.findByIdAndTenantId(ncId, tenantId)
                .orElseThrow(() -> new NcNotFoundException(ncId));
    }

    private ObjectStorage requireStorage() {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new StorageDisabledException();
        }
        return storage;
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /**
     * M1 — Vérifie que les premiers octets ("magic bytes") correspondent au content-type
     * déclaré. Sniff manuel, aucune dépendance externe.
     *
     * <ul>
     *   <li>JPEG : {@code FF D8 FF}</li>
     *   <li>PNG  : {@code 89 50 4E 47 0D 0A 1A 0A}</li>
     *   <li>WEBP : {@code 'RIFF' .... 'WEBP'} (octets 0-3 = RIFF, 8-11 = WEBP)</li>
     *   <li>HEIC : boîte {@code ftyp} à l'offset 4 (octets 4-7 = 'ftyp')</li>
     * </ul>
     */
    static boolean magicBytesMatch(String normalizedType, byte[] c) {
        return switch (normalizedType) {
            case "image/jpeg" -> startsWith(c, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(c, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> c.length >= 12
                    && startsWith(c, 0x52, 0x49, 0x46, 0x46) // "RIFF"
                    && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
            // HEIC : 'ftyp' à l'offset 4 ; la marque (heic/heix/mif1/…) suit, on reste
            // permissif sur la sous-marque mais on exige la boîte ftyp.
            case "image/heic" -> c.length >= 8
                    && c[4] == 'f' && c[5] == 't' && c[6] == 'y' && c[7] == 'p';
            default -> false; // type non whitelisté : déjà rejeté en amont
        };
    }

    private static boolean startsWith(byte[] c, int... prefix) {
        if (c.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((c[i] & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /** Garde le nom d'origine à titre informatif uniquement, neutralisé (jamais réutilisé dans la clé). */
    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // Conserve uniquement le segment de base, sans chemin, et tronque.
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private static NcPhotoDto.Response toResponse(NcPhoto p) {
        return new NcPhotoDto.Response(
                p.getId(), p.getNcId(), p.getObjectKey(), p.getContentType(),
                p.getSizeBytes(), p.getOriginalFilename(), p.getCreatedAt());
    }

    private static NcPhotoDto.ListItem toListItem(NcPhoto p, URL url) {
        return new NcPhotoDto.ListItem(
                p.getId(), p.getNcId(), p.getObjectKey(), p.getContentType(),
                p.getSizeBytes(), p.getOriginalFilename(), p.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
