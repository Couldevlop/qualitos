package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
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
 * Preuves jointes à un dossier CAPA (§4.2, ISO 9001 §10.2).
 *
 * <p>Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se
 * prouve. Ce service porte les bornes qui rendent le dossier lisible et le verrou
 * qui le rend crédible.
 *
 * <p>Trois bornes, toutes refusées explicitement plutôt que contournées en
 * silence : dix méga-octets par pièce, dix pièces par dossier, vingt-cinq
 * méga-octets au total. Un dossier de preuve se lit ; au-delà, l'auditeur ne
 * trouve plus rien, et supprimer d'office la pièce la plus ancienne pour faire
 * de la place serait indéfendable dans un dossier d'audit.
 *
 * <p>Le verrou d'état suit celui des actions : on verse et on retire tant que le
 * dossier vit, plus rien une fois clos. Un dossier qu'on peut regarnir après coup
 * ne prouve plus rien, puisque la pièce a pu être fabriquée après le constat.
 */
@Service
@Transactional
public class CapaEvidenceService {

    /** Plafond par pièce — double rempart avec la limite multipart de Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Nombre de pièces par dossier. */
    static final int MAX_COUNT = 10;

    /** Poids cumulé par dossier : dix pièces pleines feraient dix fois trop. */
    static final long MAX_TOTAL_BYTES = 25L * 1024 * 1024;

    /** TTL des URLs présignées de lecture. */
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /**
     * Liste blanche content-type → extension (OWASP : l'extension vient du type
     * validé, jamais du nom de fichier client). Les formats bureautiques sont
     * admis par choix d'exploitation, en connaissance de leur défaut : un
     * classeur reste modifiable après coup, contrairement à un PDF ou une image.
     */
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
        return Map.copyOf(types);
    }

    private final CapaEvidenceRepository evidenceRepository;
    private final CapaCaseRepository caseRepository;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public CapaEvidenceService(CapaEvidenceRepository evidenceRepository,
                               CapaCaseRepository caseRepository,
                               ObjectProvider<ObjectStorage> storageProvider,
                               AuditEventService auditEvents) {
        this.evidenceRepository = evidenceRepository;
        this.caseRepository = caseRepository;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    public CapaEvidenceDto.Response upload(UUID capaId, String contentType, String originalFilename,
                                           byte[] content, UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        CapaCase capa = loadOpenCase(capaId, tenantId);

        if (content == null || content.length == 0) {
            throw new CapaEvidenceValidationException("Empty evidence upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new CapaEvidenceTooLargeException(content.length, MAX_SIZE_BYTES);
        }

        String normalizedType = contentType == null ? "" : contentType.toLowerCase().trim();
        String ext = ALLOWED_TYPES.get(normalizedType);
        if (ext == null) {
            throw new CapaEvidenceValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le content-type déclaré est falsifiable : la signature binaire doit
        // correspondre au type annoncé, sinon un exécutable renommé passerait.
        if (!magicBytesMatch(normalizedType, content)) {
            throw new CapaEvidenceValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }

        long already = evidenceRepository.countByTenantIdAndCapaId(tenantId, capaId);
        if (already >= MAX_COUNT) {
            throw new CapaStateException("This CAPA already carries its " + MAX_COUNT
                    + " evidence files: remove one before adding another");
        }
        long total = evidenceRepository.sumSizeBytes(tenantId, capaId);
        if (total + content.length > MAX_TOTAL_BYTES) {
            throw new CapaStateException("Adding this file would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of evidence allowed on a CAPA");
        }

        String key = "tenants/" + tenantId + "/capa/" + capa.getId() + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnées d'abord, binaire ensuite : si le put échoue, la transaction
        // annule la ligne et rien n'est écrit. L'ordre inverse laisserait un
        // binaire orphelin, invisible et facturé.
        CapaEvidence evidence = new CapaEvidence();
        evidence.setTenantId(tenantId);
        evidence.setCapaId(capa.getId());
        evidence.setObjectKey(key);
        evidence.setContentType(normalizedType);
        evidence.setSizeBytes(content.length);
        evidence.setOriginalFilename(sanitizeFilename(originalFilename));
        evidence.setUploadedBy(uploadedBy);
        CapaEvidence saved = evidenceRepository.save(evidence);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "capa.evidence.uploaded", saved,
                "Preuve versée au dossier CAPA " + capa.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CapaEvidenceDto.ListItem> list(UUID capaId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadCase(capaId, tenantId); // 404 si le dossier est absent ou d'un autre tenant
        return evidenceRepository.findByTenantIdAndCapaIdOrderByCreatedAtAsc(tenantId, capaId).stream()
                .map(e -> toListItem(e, storage.presignGet(e.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    public void delete(UUID capaId, UUID evidenceId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadOpenCase(capaId, tenantId);
        CapaEvidence evidence = evidenceRepository
                .findByIdAndTenantIdAndCapaId(evidenceId, tenantId, capaId)
                .orElseThrow(() -> new CapaEvidenceNotFoundException(evidenceId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite. Si la
        // suppression du binaire échoue, la transaction rétablit la ligne et le
        // dossier reste cohérent plutôt que de pointer vers un objet disparu.
        evidenceRepository.delete(evidence);
        storage.delete(evidence.getObjectKey());
        // Le retrait est tracé AVANT tout le reste dans l'ordre d'importance :
        // c'est la seule opération qui fait disparaître une preuve d'un dossier
        // d'audit. Sans trace, le dossier ne dirait plus ce qu'il a porté, et
        // l'écran promet à l'utilisateur que ce retrait laisse une marque.
        trace(tenantId, removedBy, "capa.evidence.removed", evidence,
                "Preuve retirée du dossier CAPA " + capaId);
    }

    /**
     * Inscrit l'opération au journal chaîné du tenant (§11.5). Le nom d'origine
     * y figure — c'est ce qui permet de dire QUELLE pièce a été versée ou
     * retirée — mais jamais la clé d'objet : elle donnerait un chemin de
     * stockage dans un journal qui se relit et s'exporte.
     */
    private void trace(UUID tenantId, UUID actor, String action, CapaEvidence evidence, String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "capa_evidence",
                evidence.getId(),
                summary,
                payload(evidence),
                null,
                null));
    }

    /** JSON minimal et construit à la main : trois champs, aucun sérialiseur à convoquer. */
    private static String payload(CapaEvidence e) {
        return "{\"capaId\":\"" + e.getCapaId()
                + "\",\"contentType\":\"" + e.getContentType()
                + "\",\"sizeBytes\":" + e.getSizeBytes()
                + ",\"originalFilename\":" + jsonString(e.getOriginalFilename()) + "}";
    }

    /**
     * Le nom d'origine est déjà assaini à l'entrée, mais un journal ne doit pas
     * dépendre d'une hypothèse tenue ailleurs : les guillemets et les
     * antislashs sont échappés ici aussi, faute de quoi un nom bien choisi
     * casserait le JSON de la ligne.
     */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // --- garde-fous ----------------------------------------------------------

    private CapaCase loadCase(UUID capaId, UUID tenantId) {
        return caseRepository.findByIdAndTenantId(capaId, tenantId)
                .orElseThrow(() -> new CapaNotFoundException(capaId));
    }

    /** Même verrou que les actions : un dossier clos ou rejeté ne bouge plus. */
    private CapaCase loadOpenCase(UUID capaId, UUID tenantId) {
        CapaCase capa = loadCase(capaId, tenantId);
        if (capa.getStatus() == CapaStatus.CLOSED || capa.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot change evidence on a " + capa.getStatus() + " CAPA");
        }
        return capa;
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
     * Vérifie que les premiers octets correspondent au type déclaré. Sniff
     * manuel, sans dépendance.
     *
     * <ul>
     *   <li>PDF : {@code %PDF-}</li>
     *   <li>docx / xlsx : archives ZIP, donc {@code PK} — la
     *       signature ne distingue pas les deux, et c'est assumé : elle écarte ce
     *       qui n'est pas une archive, le type déclaré fait le reste</li>
     *   <li>JPEG, PNG, WEBP, HEIC : signatures d'image usuelles</li>
     * </ul>
     */
    static boolean magicBytesMatch(String normalizedType, byte[] c) {
        return switch (normalizedType) {
            case "application/pdf" -> startsWith(c, 0x25, 0x50, 0x44, 0x46, 0x2D); // %PDF-
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    startsWith(c, 0x50, 0x4B, 0x03, 0x04); // PK..
            case "image/jpeg" -> startsWith(c, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(c, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> c.length >= 12
                    && startsWith(c, 0x52, 0x49, 0x46, 0x46)
                    && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
            case "image/heic" -> c.length >= 8
                    && c[4] == 'f' && c[5] == 't' && c[6] == 'y' && c[7] == 'p';
            default -> false; // type hors liste blanche : déjà refusé en amont
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

    /** Nom d'origine conservé à titre informatif, neutralisé, jamais réinjecté dans la clé. */
    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private static CapaEvidenceDto.Response toResponse(CapaEvidence e) {
        return new CapaEvidenceDto.Response(
                e.getId(), e.getCapaId(), e.getContentType(), e.getSizeBytes(),
                e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt());
    }

    private static CapaEvidenceDto.ListItem toListItem(CapaEvidence e, URL url) {
        return new CapaEvidenceDto.ListItem(
                e.getId(), e.getCapaId(), e.getContentType(), e.getSizeBytes(),
                e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
