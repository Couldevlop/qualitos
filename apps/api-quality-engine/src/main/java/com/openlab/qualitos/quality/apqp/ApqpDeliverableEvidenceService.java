package com.openlab.qualitos.quality.apqp;

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
 * Les pièces versées en preuve des livrables APQP.
 *
 * <p>Même motif que les preuves PDCA et CAPA, parce qu'il est éprouvé :
 * métadonnée en base, binaire dans le stockage objet sous clé tenantisée, URL
 * présignée à la lecture, propriétaire déclaré au balayeur d'orphelins. Trois
 * écarts voulus, et eux seuls : cinq pièces par livrable au lieu d'une, le
 * {@code .docx} en tête de la liste blanche, et cinquante mégaoctets par client.
 */
@Service
@Transactional
public class ApqpDeliverableEvidenceService {

    /** Plafond par pièce — double rempart avec la limite multipart de Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Cinq pièces par livrable.
     *
     * <p>Là où une étape PDCA n'en admet qu'une, un livrable APQP en cumule
     * plusieurs de plein droit : un dossier PPAP se compose de pièces distinctes,
     * et les forcer dans un seul fichier reviendrait à demander à l'utilisateur de
     * les agréger lui-même avant de les verser.
     */
    static final int MAX_PER_DELIVERABLE = 5;

    /** Poids cumulé par client : un cycle bien fourni ne doit pas peser un disque. */
    static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;

    /** TTL des URL présignées de lecture. */
    static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    /**
     * Liste blanche type → extension, le {@code .docx} en tête.
     *
     * <p>C'est LA forme sous laquelle ces livrables circulent : reçus par
     * courriel, versés tels quels. Qu'un document bureautique reste modifiable
     * après coup est assumé ici comme il l'est pour les preuves PDCA — c'est la
     * pièce réelle, et le journal d'audit fige ce qui a été versé, quand, et par
     * qui.
     */
    static final Map<String, String> ALLOWED_TYPES = buildAllowedTypes();

    private static Map<String, String> buildAllowedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        types.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        types.put("application/pdf", "pdf");
        types.put("image/png", "png");
        types.put("image/jpeg", "jpg");
        return Map.copyOf(types);
    }

    private final ApqpDeliverableEvidenceRepository evidences;
    private final ApqpPhaseRepository phases;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final AuditEventService auditEvents;

    public ApqpDeliverableEvidenceService(ApqpDeliverableEvidenceRepository evidences,
                                          ApqpPhaseRepository phases,
                                          ObjectProvider<ObjectStorage> storageProvider,
                                          AuditEventService auditEvents) {
        this.evidences = evidences;
        this.phases = phases;
        this.storageProvider = storageProvider;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<ApqpDeliverableEvidenceDto.ListItem> list(UUID phaseId, UUID deliverableId) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        requireDeliverable(phaseId, deliverableId, tenantId);

        return evidences.findByTenantIdAndDeliverableIdOrderByCreatedAtAsc(tenantId, deliverableId)
                .stream()
                .map(e -> toListItem(e, storage.presignGet(e.getObjectKey(), PRESIGN_TTL)))
                .toList();
    }

    /**
     * Verse une pièce au livrable.
     *
     * <p>Le livrable est relu DANS la phase passée en paramètre, jamais cru sur
     * parole : un identifiant de livrable d'une autre phase — ou d'un autre client,
     * puisque la phase est déjà filtrée — rattacherait une preuve à un travail
     * étranger. C'est un 404 et non un 403 : ne rien dire de l'existence de la
     * ressource est le comportement attendu (OWASP A01).
     */
    public ApqpDeliverableEvidenceDto.Response upload(UUID phaseId, UUID deliverableId,
                                                      String contentType, String originalFilename,
                                                      byte[] content, UUID uploadedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        ApqpDeliverable livrable = requireDeliverable(phaseId, deliverableId, tenantId);

        if (content == null || content.length == 0) {
            throw new ApqpDeliverableEvidenceValidationException("Empty evidence upload");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new ApqpDeliverableEvidenceTooLargeException("A single file cannot exceed "
                    + (MAX_SIZE_BYTES / (1024 * 1024)) + " MB");
        }

        String normalizedType = UploadedBinaryGuard.normalizeType(contentType);
        String ext = UploadedBinaryGuard.extensionOf(ALLOWED_TYPES, normalizedType);
        if (ext == null) {
            throw new ApqpDeliverableEvidenceValidationException("Unsupported content type: "
                    + contentType + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        if (!UploadedBinaryGuard.magicBytesMatch(normalizedType, content)) {
            throw new ApqpDeliverableEvidenceValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }

        if (evidences.countByTenantIdAndDeliverableId(tenantId, deliverableId)
                >= MAX_PER_DELIVERABLE) {
            throw new ApqpDeliverableEvidenceTooLargeException("A deliverable carries at most "
                    + MAX_PER_DELIVERABLE + " files: remove one before adding another");
        }
        long total = evidences.sumSizeBytes(tenantId);
        if (total + content.length > MAX_TOTAL_BYTES) {
            throw new ApqpDeliverableEvidenceTooLargeException("Adding this file would exceed the "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB of evidence allowed on an APQP cycle");
        }

        // La clé est construite de bout en bout à partir d'identifiants tenus par
        // la plateforme — jamais du nom fourni par le client — donc aucune séquence
        // de remontée de chemin ne peut s'y glisser.
        String key = "tenants/" + tenantId + "/apqp/" + phaseId + "/deliverables/" + deliverableId
                + "/" + UUID.randomUUID() + "." + ext;

        // Métadonnée d'abord, binaire ensuite : si le dépôt échoue, la transaction
        // annule la ligne et rien n'est écrit. L'ordre inverse laisserait un
        // binaire orphelin, invisible et facturé.
        ApqpDeliverableEvidence evidence = new ApqpDeliverableEvidence();
        evidence.setTenantId(tenantId);
        evidence.setPhaseId(phaseId);
        evidence.setDeliverableId(deliverableId);
        evidence.setObjectKey(key);
        evidence.setContentType(normalizedType);
        evidence.setSizeBytes(content.length);
        evidence.setOriginalFilename(UploadedBinaryGuard.sanitizeFilename(originalFilename));
        evidence.setUploadedBy(uploadedBy);
        ApqpDeliverableEvidence saved = evidences.save(evidence);

        storage.put(key, normalizedType, content);
        trace(tenantId, uploadedBy, "apqp.deliverable-evidence.uploaded", saved,
                "Preuve versée au livrable « " + livrable.getLabel() + " »");
        return toResponse(saved);
    }

    /**
     * Retire une pièce.
     *
     * <p>La pièce est cherchée POUR ce livrable : passer l'identifiant d'une pièce
     * versée ailleurs ne doit pas la faire disparaître d'un livrable voisin.
     */
    public void delete(UUID phaseId, UUID deliverableId, UUID evidenceId, UUID removedBy) {
        UUID tenantId = requireTenantId();
        ObjectStorage storage = requireStorage();
        ApqpDeliverable livrable = requireDeliverable(phaseId, deliverableId, tenantId);

        ApqpDeliverableEvidence evidence = evidences
                .findByIdAndTenantIdAndDeliverableId(evidenceId, tenantId, deliverableId)
                .orElseThrow(() -> new ApqpDeliverableEvidenceNotFoundException(evidenceId));

        // Symétrique du dépôt : la ligne d'abord, l'objet ensuite. Si la
        // suppression du binaire échoue, la transaction rétablit la ligne et le
        // livrable reste cohérent plutôt que de pointer vers un objet disparu.
        evidences.delete(evidence);
        storage.delete(evidence.getObjectKey());
        // Le retrait est la seule opération qui fait disparaître une preuve d'un
        // dossier PPAP. Sans trace, le dossier ne dirait plus ce qu'il a porté.
        trace(tenantId, removedBy, "apqp.deliverable-evidence.removed", evidence,
                "Preuve retirée du livrable « " + livrable.getLabel() + " »");
    }

    // ---------- garde-fous ----------

    /**
     * Le livrable, à condition qu'il appartienne à cette phase et à ce client.
     *
     * <p>La phase est filtrée par client, le livrable par phase : deux contrôles
     * qui, ensemble, ferment la porte à un identifiant emprunté ailleurs.
     */
    private ApqpDeliverable requireDeliverable(UUID phaseId, UUID deliverableId, UUID tenantId) {
        ApqpPhase phase = phases.findByIdAndTenantId(phaseId, tenantId)
                .orElseThrow(() -> new ApqpPhaseNotFoundException(phaseId));
        return phase.getDeliverables().stream()
                .filter(d -> d.getId().equals(deliverableId))
                .findFirst()
                .orElseThrow(() -> new ApqpDeliverableNotFoundException(deliverableId));
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
     * figure — c'est ce qui permet de dire QUELLE pièce a été versée ou retirée —
     * mais jamais la clé d'objet : elle donnerait un chemin de stockage dans un
     * journal qui se relit et s'exporte.
     */
    private void trace(UUID tenantId, UUID actor, String action,
                       ApqpDeliverableEvidence evidence, String summary) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                action,
                "apqp_deliverable_evidence",
                evidence.getId(),
                summary,
                payload(evidence),
                null,
                null));
    }

    /**
     * JSON minimal, écrit à la main : quatre champs, aucun sérialiseur à
     * convoquer. Le livrable visé en fait partie — sans lui, le journal dirait
     * qu'une preuve a quitté le cycle sans dire ce qu'elle étayait.
     */
    private static String payload(ApqpDeliverableEvidence e) {
        return "{\"phaseId\":\"" + e.getPhaseId()
                + "\",\"deliverableId\":\"" + e.getDeliverableId()
                + "\",\"contentType\":\"" + e.getContentType()
                + "\",\"sizeBytes\":" + e.getSizeBytes()
                + ",\"originalFilename\":" + jsonString(e.getOriginalFilename()) + "}";
    }

    /**
     * Le nom d'origine est déjà assaini à l'entrée, mais un journal ne doit pas
     * dépendre d'une hypothèse tenue ailleurs : guillemets et antislashs sont
     * échappés ici aussi, faute de quoi un nom bien choisi casserait le JSON de la
     * ligne.
     */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static ApqpDeliverableEvidenceDto.Response toResponse(ApqpDeliverableEvidence e) {
        return new ApqpDeliverableEvidenceDto.Response(
                e.getId(), e.getPhaseId(), e.getDeliverableId(), e.getContentType(),
                e.getSizeBytes(), e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt(),
                e.getObjectKey());
    }

    private static ApqpDeliverableEvidenceDto.ListItem toListItem(
            ApqpDeliverableEvidence e, URL url) {
        return new ApqpDeliverableEvidenceDto.ListItem(
                e.getId(), e.getPhaseId(), e.getDeliverableId(), e.getContentType(),
                e.getSizeBytes(), e.getOriginalFilename(), e.getUploadedBy(), e.getCreatedAt(),
                url == null ? null : url.toString());
    }
}
