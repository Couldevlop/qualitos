package com.openlab.qualitos.quality.visiongateway;

import java.util.List;

/**
 * DTOs typés exposés au SPA pour l'analyse 5S par vision (CLAUDE.md §3.2). Mappent la
 * réponse JSON (snake_case) du service {@code ai-vision-5s} vers des records camelCase
 * sérialisés par Jackson, comme le module {@code spc} le fait pour {@code ai-service}.
 *
 * <p>L'engine relaie et présente (clean architecture) : aucune inférence ici, tout le
 * traitement vision (YOLOv8/ONNX + scoring) vit dans {@code ai-vision-5s}.
 */
public final class VisionDto {

    private VisionDto() {}

    /**
     * Score 5S par pilier (0-100) + agrégat {@code overall}. Entiers : le service de
     * vision renvoie des notes entières par pilier.
     */
    public record VisionScore(
            int seiri,
            int seiton,
            int seiso,
            int seiketsu,
            int shitsuke,
            int overall) {
    }

    /**
     * Constat détecté sur l'image. {@code pillar} dans {SEIRI, SEITON, SEISO, SEIKETSU,
     * SHITSUKE} ; {@code severity} (ex. LOW/MEDIUM/HIGH) ; {@code confidence} ∈ [0,1] ;
     * {@code bbox} = [x, y, w, h] en pixels, ou {@code null} si non localisé.
     */
    public record VisionFinding(
            String pillar,
            String description,
            String severity,
            double confidence,
            List<Integer> bbox) {
    }

    /**
     * Résultat complet d'une analyse : empreinte de l'image (sha-256 hex), dimensions,
     * score 5S et liste des constats.
     */
    public record VisionAnalysis(
            String imageSha256,
            int width,
            int height,
            VisionScore score,
            List<VisionFinding> findings) {
    }
}
