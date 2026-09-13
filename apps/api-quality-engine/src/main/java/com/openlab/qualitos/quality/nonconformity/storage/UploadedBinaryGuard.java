package com.openlab.qualitos.quality.nonconformity.storage;

import java.util.Map;

/**
 * Ce qu'on vérifie sur un binaire versé, quel que soit le module qui le reçoit.
 *
 * <p>Trois gardes, toujours les mêmes : le type déclaré appartient-il à la liste
 * blanche, les premiers octets correspondent-ils à ce type, et le nom d'origine
 * est-il neutralisé avant d'être conservé. Les preuves PDCA, les preuves CAPA et
 * les photos de non-conformité portent chacune leur copie de ces trois gardes —
 * écrites avant celle-ci, et qui pourront s'y replier. Le cycle APQP, lui, part
 * d'ici : une quatrième copie aurait été la copie de trop, et c'est toujours la
 * copie oubliée qui laisse passer l'exécutable renommé.
 *
 * <p>Sans état, sans dépendance : des fonctions pures, donc aucune raison d'en
 * faire un bean.
 */
public final class UploadedBinaryGuard {

    private UploadedBinaryGuard() {}

    /**
     * L'extension à donner au type, ou {@code null} si le type n'est pas admis.
     *
     * <p>L'extension vient du type VALIDÉ, jamais du nom de fichier du client
     * (OWASP) : c'est elle qui finit dans la clé de stockage.
     */
    public static String extensionOf(Map<String, String> allowedTypes, String normalizedType) {
        return allowedTypes.get(normalizedType);
    }

    /**
     * Le type déclaré est falsifiable : les premiers octets doivent lui
     * correspondre, sinon un exécutable renommé passerait la liste blanche.
     *
     * <ul>
     *   <li>PDF : {@code %PDF-}</li>
     *   <li>docx / xlsx : des archives ZIP, donc {@code PK} — la signature ne
     *       distingue pas les deux, et c'est assumé : elle écarte ce qui n'est pas
     *       une archive, le type déclaré fait le reste</li>
     *   <li>JPEG, PNG, WEBP, HEIC : signatures d'image usuelles</li>
     * </ul>
     *
     * @return {@code false} pour tout type hors de cette liste — un type inconnu
     *         n'est jamais « probablement bon »
     */
    public static boolean magicBytesMatch(String normalizedType, byte[] content) {
        if (content == null) {
            return false;
        }
        return switch (normalizedType) {
            case "application/pdf" -> startsWith(content, 0x25, 0x50, 0x44, 0x46, 0x2D);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    startsWith(content, 0x50, 0x4B, 0x03, 0x04);
            case "image/jpeg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "image/png" ->
                    startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> content.length >= 12
                    && startsWith(content, 0x52, 0x49, 0x46, 0x46)
                    && content[8] == 'W' && content[9] == 'E'
                    && content[10] == 'B' && content[11] == 'P';
            case "image/heic" -> content.length >= 8
                    && content[4] == 'f' && content[5] == 't'
                    && content[6] == 'y' && content[7] == 'p';
            default -> false;
        };
    }

    /** Le type tel qu'on le compare : sans casse, sans espaces, jamais {@code null}. */
    public static String normalizeType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase().trim();
    }

    /**
     * Le nom d'origine, conservé à titre informatif et neutralisé.
     *
     * <p>Le chemin est retiré et les caractères hors alphabet réduits : ce nom
     * s'affiche, se journalise, mais ne retourne jamais dans une clé de stockage.
     */
    public static String sanitizeFilename(String name) {
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

    private static boolean startsWith(byte[] content, int... prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((content[i] & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
