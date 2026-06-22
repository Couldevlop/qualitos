package com.openlab.qualitos.quality.marketplace.domain;

import java.util.List;

/**
 * Résultat du scan de sécurité/structure d'un manifeste de pack à la soumission.
 *
 * @param ok       true si le manifeste passe tous les contrôles bloquants
 * @param errors   anomalies bloquantes (refus de la soumission) — peut être vide
 * @param warnings remarques non bloquantes (acceptées mais signalées à l'éditeur)
 */
public record ManifestScanResult(boolean ok, List<String> errors, List<String> warnings) {

    public ManifestScanResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static ManifestScanResult clean(List<String> warnings) {
        return new ManifestScanResult(true, List.of(), warnings);
    }

    public static ManifestScanResult rejected(List<String> errors, List<String> warnings) {
        return new ManifestScanResult(false, errors, warnings);
    }
}
