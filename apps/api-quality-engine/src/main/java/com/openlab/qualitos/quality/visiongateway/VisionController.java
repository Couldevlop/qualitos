package com.openlab.qualitos.quality.visiongateway;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Endpoint de relais vers le service de vision 5S (CLAUDE.md §3.2). Le SPA téléverse une
 * photo terrain ; api-quality-engine valide puis relaie vers {@code ai-vision-5s} et
 * renvoie le résultat typé. Calqué sur {@code NcPhotoController} (multipart + validation)
 * et {@code SpcController} (relais vers une passerelle d'inférence).
 *
 * <p>Sécurité : authentifié (SecurityConfig {@code anyRequest().authenticated()}), sans
 * role-gating dédié — l'analyse 5S est une action métier qualité (audit terrain §3.2)
 * accessible à tout utilisateur authentifié, comme la consultation des autres modules
 * qualité. Le tenant est dérivé du JWT côté passerelle ({@link VisionGatewayClient}),
 * jamais du corps de requête (règle 18.2 #2).
 *
 * <p>Champ multipart : {@code image} (aligné sur le contrat figé d'ai-vision-5s). Un
 * alias {@code file} est aussi accepté pour rester cohérent avec {@code NcPhotoController}.
 */
@RestController
@RequestMapping("/api/v1/vision/5s")
public class VisionController {

    /** Plafond applicatif (10 Mo) — aligné sur la limite multipart Spring (§4.3). */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Whitelist content-type → marqueur (OWASP : type validé, pas le nom de fichier). */
    static final Map<String, int[]> ALLOWED_TYPES = Map.of(
            "image/jpeg", new int[]{0xFF, 0xD8, 0xFF},
            "image/png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/webp", new int[]{0x52, 0x49, 0x46, 0x46} // "RIFF" (sous-marque WEBP vérifiée ensuite)
    );

    private final VisionGatewayClient gateway;

    public VisionController(VisionGatewayClient gateway) {
        this.gateway = gateway;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionDto.VisionAnalysis analyze(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
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
        return gateway.analyze(normalizedType, part.getOriginalFilename(), bytes);
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
