package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Une photo de la fiche, sur l'un des six emplacements du classeur, ou une pièce
 * jointe d'une phase (emplacements 11-15 pour Définir … 51-55 pour Contrôler),
 * avec sa remarque. Le binaire vit dans le stockage objet ; ceci n'en porte que
 * la clé.
 */
public record SheetPhoto(UUID id, UUID tenantId, UUID projectId, int slot, String objectKey, String contentType,
                         long sizeBytes, String remark, UUID createdBy, Instant createdAt) {

    public static final int SLOTS = 6;
    public static final int REMARK_MAX = 1000;
    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Les formats d'image admis et l'extension qu'ils donnent à la clé (jamais le nom du client). */
    public static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    /** Pièces jointes par phase : cinq emplacements par phase, de 11-15 à 51-55. */
    public static final int ATTACHMENTS_PER_PHASE = 5;

    /** Une pièce jointe de phase admet aussi le PDF et les documents Office. */
    public static final Map<String, String> ATTACHMENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp",
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");

    public static boolean isAttachmentSlot(int slot) {
        int phase = slot / 10;
        int rank = slot % 10;
        return phase >= 1 && phase <= 5 && rank >= 1 && rank <= ATTACHMENTS_PER_PHASE;
    }

    public static void checkSlot(int slot) {
        if ((slot < 1 || slot > SLOTS) && !isAttachmentSlot(slot)) {
            throw new ResolutionValidationException("slot",
                    "A photo slot is between 1 and " + SLOTS + ", an attachment slot between 11 and 55");
        }
    }

    /** Les types admis sur cet emplacement. */
    public static Map<String, String> typesFor(int slot) {
        return isAttachmentSlot(slot) ? ATTACHMENT_TYPES : ALLOWED_TYPES;
    }

    public static String cleanRemark(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String remark = raw.strip();
        if (remark.length() > REMARK_MAX) {
            throw new ResolutionValidationException("remark", "At most " + REMARK_MAX + " characters");
        }
        return remark;
    }

    public SheetPhoto withRemark(String newRemark) {
        return new SheetPhoto(id, tenantId, projectId, slot, objectKey, contentType, sizeBytes,
                cleanRemark(newRemark), createdBy, createdAt);
    }
}
