package com.openlab.qualitos.quality.pdca;

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
 * Preuves jointes aux étapes d'un cycle PDCA (§3.1, ADR 0061).
 *
 * <p>Une étape déclarée faite sans document ne prouve rien : elle affirme. La
 * preuve de la mise en place d'une action est toujours un document signé, et
 * c'est celui-là que l'auditeur réclame. Ce service porte les bornes qui rendent
 * la colonne lisible et le verrou qui la rend crédible.
 *
 * <p>Les bornes, toutes refusées explicitement plutôt que contournées en
 * silence : dix méga-octets par pièce, UNE pièce par étape, vingt-cinq
 * méga-octets cumulés par cycle. La colonne « Preuve » du tableau montre un
 * document, pas une liste ; deux pièces rendraient la cellule indécidable.
 * Remplacer se fait en deux gestes — retirer, puis reverser — et les deux se
 * consignent, ce qu'un remplacement silencieux ne ferait pas.
 *
 * <p>Le verrou d'état suit celui du cycle : on verse et on retire tant que le
 * cycle vit, plus rien une fois clos ou annulé. Un dossier qu'on peut regarnir
 * après coup ne prouve plus rien, puisque la pièce a pu être fabriquée après le
 * constat.
 */
@Service
@Transactional
public class PdcaStepEvidenceService {

    /** Plafond par pièce — double rempart avec la limite multipart de Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Nombre de pièces par ÉTAPE : une seule, comme pour les actions CAPA.
     *
     * <p>La cellule du tableau affiche un document ; en accepter deux
     * obligerait soit à en cacher un, soit à faire déborder la colonne.
     */
    static final int MAX_PER_STEP = 1;

    /** Poids cumulé par cycle : un cycle bien fourni ne doit pas peser un disque. */
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

    private final PdcaStepEvidenceRepository evidenceRepository;
    private final PdcaCycleRepository cycleRepository;
    private final PdcaStepRepository stepRepository;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public PdcaStepEvidenceService(PdcaStepEvidenceRepository evidenceRepository,
                                   PdcaCycleRepository cycleRepository,
                                   PdcaStepRepository stepRepository,
                                   ObjectProvider<ObjectStorage> storageProvider,
                                   AuditEventService auditEvents) {
        this.evidenceRepository = evidenceRepository;
        this.cycleRepository = cycleRepository;
        this.stepRepository = stepRepository;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    /**
     * Pièces de TOUTES les étapes du cycle, en un appel.
     *
     * <p>Le tableau les range ensuite par étape. Une requête par ligne ferait
     * autant d'allers et retours que d'étapes pour remplir une seule colonne.
     */
    @Transactional(readOnly = true)
    public List<PdcaStepEvidenceDto.ListItem> listForCycle(UUID cycleId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadCycle(cycleId, tenantId); // 404 si le cycle est absent ou d'un autre tenant
        return evidenceRepository.findForCycle(tenantId, cycleId).stream()
                .map(e -> toListItem(e, storage.presignGet(e.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    /**
     * Dépôt d'une pièce sur UNE étape du cycle.
     *
     * <p>L'étape est relue dans le cycle passé en paramètre, jamais crue sur
     * parole : un identifiant d'étape d'un AUTRE cycle — ou d'un autre tenant,
     * puisque le cycle est déjà filtré — rattacherait une preuve à un travail
     * étranger. C'est un 404, pas un 403 : ne rien dire de l'existence de la
     * ressource est le comportement attendu (OWASP A01).
     */
    public PdcaStepEvidenceDto.Response upload(UUID cycleId, UUID stepId, String contentType,
                                               String originalFilename, byte[] content,
                                               UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        PdcaCycle cycle = loadLiveCycle(cycleId, tenantId);
        requireStep(cycleId, stepId);

        if (content == null || content.length == 0) {
            throw new PdcaStepEvidenceValidationException("Empty evidence upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new PdcaStepEvidenceTooLargeException(content.length, MAX_SIZE_BYTES);
        }

        String normalizedType = contentType == null ? "" : contentType.toLowerCase().trim();
        String ext = ALLOWED_TYPES.get(normalizedType);
        if (ext == null) {
            throw new PdcaStepEvidenceValidationException("Unsupported content type: " + contentType
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le content-type déclaré est falsifiable : la signature binaire doit
        // correspondre au type annoncé, sinon un exécutable renommé passerait.
        if (!magicBytesMatch(normalizedType, content)) {
            throw new PdcaStepEvidenceValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }

        if (evidenceRepository.countByTenantIdAndStepId(tenantId, stepId) >= MAX_PER_STEP) {
            throw new PdcaStateException(
                    "This step already carries its evidence file: remove it before adding another");
        }
        // Le poids cumulé protège le disque : il se compte au niveau du cycle,
        // qui est l'unité qu'un tenant crée et multiplie.
        long total = evidenceRepository.sumSizeBytes(tenantId, cycleId);
        if (total + content.length > MAX_TOTAL_BYTES) {
            throw new PdcaStateException("Adding this file would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of evidence allowed on a PDCA cycle");
        }

        // La clé est construite de bout en bout à partir d'identifiants tenus par
        // la plateforme — jamais du nom fourni par le client — donc aucune
        // séquence de remontée de chemin ne peut s'y glisser.
        String key = "tenants/" + tenantId + "/pdca/" + cycle.getId() + "/steps/" + stepId
                + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnées d'abord, binaire ensuite : si le put échoue, la transaction
        // annule la ligne et rien n'est écrit. L'ordre inverse laisserait un
        // binaire orphelin, invisible et facturé.
        PdcaStepEvidence evidence = new PdcaStepEvidence();
        evidence.setTenantId(tenantId);
        evidence.setCycleId(cycle.getId());
        evidence.setStepId(stepId);
        evidence.setObjectKey(key);
        evidence.setContentType(normalizedType);
        evidence.setSizeBytes(content.length);
        evidence.setOriginalFilename(sanitizeFilename(originalFilename));
        evidence.setUploadedBy(uploadedBy);
        PdcaStepEvidence saved = evidenceRepository.save(evidence);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "pdca.step-evidence.uploaded", saved,
                "Preuve versée à l'étape " + stepId + " du cycle PDCA " + cycle.getId());
        return toResponse(saved);
    }

    /**
     * Retrait d'une pièce d'étape. L'étape est vérifiée pour ce cycle, et la
     * pièce pour cette étape : passer l'identifiant d'une pièce versée ailleurs
     * ne doit pas la faire disparaître d'un cycle voisin.
     */
    public void delete(UUID cycleId, UUID stepId, UUID evidenceId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        loadLiveCycle(cycleId, tenantId);
        requireStep(cycleId, stepId);

        PdcaStepEvidence evidence = evidenceRepository
                .findByIdAndTenantIdAndStepId(evidenceId, tenantId, stepId)
                .orElseThrow(() -> new PdcaStepEvidenceNotFoundException(evidenceId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite. Si la
        // suppression du binaire échoue, la transaction rétablit la ligne et le
        // cycle reste cohérent plutôt que de pointer vers un objet disparu.
        evidenceRepository.delete(evidence);
        storage.delete(evidence.getObjectKey());
        // Le retrait est la seule opération qui fait disparaître une preuve d'un
        // dossier d'audit. Sans trace, le cycle ne dirait plus ce qu'il a porté.
        trace(tenantId, removedBy, "pdca.step-evidence.removed", evidence,
                "Preuve retirée de l'étape " + stepId + " du cycle PDCA " + cycleId);
    }

    /**
     * Inscrit l'opération au journal chaîné du tenant (§11.5). Le nom d'origine
     * y figure — c'est ce qui permet de dire QUELLE pièce a été versée ou
     * retirée — mais jamais la clé d'objet : elle donnerait un chemin de
     * stockage dans un journal qui se relit et s'exporte.
     */
    private void trace(UUID tenantId, UUID actor, String action, PdcaStepEvidence evidence,
                       String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "pdca_step_evidence",
                evidence.getId(),
                summary,
                payload(evidence),
                null,
                null));
    }

    /**
     * JSON minimal et construit à la main : cinq champs, aucun sérialiseur à
     * convoquer. L'étape visée en fait partie — sans elle, le journal dirait
     * qu'une preuve a quitté le cycle sans dire laquelle de ses lignes elle
     * étayait.
     */
    private static String payload(PdcaStepEvidence e) {
        return "{\"cycleId\":\"" + e.getCycleId()
                + "\",\"stepId\":\"" + e.getStepId()
                + "\",\"contentType\":\"" + e.getContentType()
                + "\",\"sizeBytes\":" + e.getSizeBytes()
                + ",\"originalFilename\":" + jsonString(e.getOriginalFilename()) + "}";
    }

    /**
     * Le nom d'origine est déjà assaini à l'entrée, mais un journal ne doit pas
     * dépendre d'une hypothèse tenue ailleurs : les guillemets et les antislashs
     * sont échappés ici aussi, faute de quoi un nom bien choisi casserait le
     * JSON de la ligne.
     */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // --- garde-fous ----------------------------------------------------------

    private PdcaCycle loadCycle(UUID cycleId, UUID tenantId) {
        return cycleRepository.findByIdAndTenantId(cycleId, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(cycleId));
    }

    /** Même verrou que les étapes : un cycle clos ou annulé ne bouge plus. */
    private PdcaCycle loadLiveCycle(UUID cycleId, UUID tenantId) {
        PdcaCycle cycle = loadCycle(cycleId, tenantId);
        if (cycle.getStatus() == PdcaStatus.COMPLETED || cycle.getStatus() == PdcaStatus.CANCELLED) {
            throw new PdcaStateException("Cannot change evidence on a " + cycle.getStatus() + " cycle");
        }
        return cycle;
    }

    /**
     * L'étape doit appartenir AU cycle visé. Le cycle a déjà été filtré par
     * tenant ; c'est donc ce contrôle-ci qui empêche de coller une preuve à une
     * étape d'un autre cycle — ou d'un autre tenant, par ricochet.
     */
    private PdcaStep requireStep(UUID cycleId, UUID stepId) {
        return stepRepository.findByIdAndCycleId(stepId, cycleId)
                .orElseThrow(() -> new PdcaStepNotFoundException(stepId));
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
     *   <li>docx / xlsx : archives ZIP, donc {@code PK} — la signature ne
     *       distingue pas les deux, et c'est assumé : elle écarte ce qui n'est
     *       pas une archive, le type déclaré fait le reste</li>
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

    private static PdcaStepEvidenceDto.Response toResponse(PdcaStepEvidence e) {
        return new PdcaStepEvidenceDto.Response(
                e.getId(), e.getCycleId(), e.getStepId(), e.getContentType(), e.getSizeBytes(),
                e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt());
    }

    private static PdcaStepEvidenceDto.ListItem toListItem(PdcaStepEvidence e, URL url) {
        return new PdcaStepEvidenceDto.ListItem(
                e.getId(), e.getCycleId(), e.getStepId(), e.getContentType(), e.getSizeBytes(),
                e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
