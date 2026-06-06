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

        // Clé tenantisée. L'extension vient du content-type validé, JAMAIS du nom de
        // fichier client (qui peut contenir des séquences de traversée de chemin).
        String key = "tenants/" + tenantId + "/nc/" + ncId + "/" + UUID.randomUUID() + "." + ext;
        storage.put(key, normalizedType, content);

        NcPhoto photo = new NcPhoto();
        photo.setTenantId(tenantId);
        photo.setNcId(ncId);
        photo.setObjectKey(key);
        photo.setContentType(normalizedType);
        photo.setSizeBytes(content.length);
        photo.setOriginalFilename(sanitizeFilename(originalFilename));
        NcPhoto saved = photoRepository.save(photo);
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
        storage.delete(photo.getObjectKey()); // idempotent côté storage
        photoRepository.delete(photo);
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
