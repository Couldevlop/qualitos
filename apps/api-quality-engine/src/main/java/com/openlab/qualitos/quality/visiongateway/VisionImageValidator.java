package com.openlab.qualitos.quality.visiongateway;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Validation réutilisable d'une image téléversée avant relais vers le service de
 * vision 5S (CLAUDE.md §3.2). Mutualise la logique entre {@code VisionController}
 * (endpoint générique {@code /api/v1/vision/5s/analyze}) et le détail d'audit 5S
 * ({@code FiveSController}, {@code POST /api/v1/fives/audits/{id}/vision}).
 *
 * <p>Sécurité (OWASP) : taille plafonnée, content-type sur liste blanche, et
 * vérification des « magic bytes » (le content-type déclaré est falsifiable).
 */
public final class VisionImageValidator {

    /** Plafond applicatif (10 Mo) — aligné sur la limite multipart Spring (§4.3). */
    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Whitelist content-type → marqueur binaire (le type est validé, pas le nom de fichier). */
    static final Map<String, int[]> ALLOWED_TYPES = Map.of(
            "image/jpeg", new int[]{0xFF, 0xD8, 0xFF},
            "image/png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/webp", new int[]{0x52, 0x49, 0x46, 0x46} // "RIFF" (sous-marque WEBP vérifiée ensuite)
    );

    private VisionImageValidator() {
    }

    /** Image validée prête à relayer (type normalisé, nom, octets). */
    public record ValidatedImage(String contentType, String filename, byte[] bytes) {
    }

    /**
     * Valide la partie multipart (alias {@code image} prioritaire sur {@code file}) et
     * renvoie ses octets. Lève {@link VisionImageValidationException} /
     * {@link VisionImageTooLargeException} en cas de problème (mappées en 400/413).
     */
    public static ValidatedImage validate(MultipartFile image, MultipartFile file) throws IOException {
        MultipartFile part = image != null ? image : file;
        if (part == null || part.isEmpty()) {
            throw new VisionImageValidationException("Missing or empty 'image' part");
        }
        byte[] bytes = part.getBytes();
        if (bytes.length > MAX_SIZE_BYTES) {
            throw new VisionImageTooLargeException(bytes.length, MAX_SIZE_BYTES);
        }
        String normalizedType = part.getContentType() == null
                ? "" : part.getContentType().toLowerCase().trim();
        if (!ALLOWED_TYPES.containsKey(normalizedType)) {
            throw new VisionImageValidationException("Unsupported content type: " + part.getContentType()
                    + " (allowed: " + ALLOWED_TYPES.keySet() + ")");
        }
        // Le content-type déclaré est falsifiable : on vérifie la signature binaire.
        if (!magicBytesMatch(normalizedType, bytes)) {
            throw new VisionImageValidationException(
                    "File content does not match the declared type '" + normalizedType + "'");
        }
        return new ValidatedImage(normalizedType, part.getOriginalFilename(), bytes);
    }

    /** Vérifie les "magic bytes" en cohérence avec le content-type déclaré. */
    static boolean magicBytesMatch(String normalizedType, byte[] c) {
        if ("image/webp".equals(normalizedType)) {
            return c.length >= 12
                    && startsWith(c, ALLOWED_TYPES.get("image/webp"))
                    && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
        }
        int[] prefix = ALLOWED_TYPES.get(normalizedType);
        return prefix != null && startsWith(c, prefix);
    }

    private static boolean startsWith(byte[] c, int[] prefix) {
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
}
