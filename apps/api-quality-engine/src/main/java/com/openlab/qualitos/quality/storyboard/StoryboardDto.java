package com.openlab.qualitos.quality.storyboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO du Storyboard IA (CLAUDE.md §7.4) : à partir d'une liste d'indicateurs déjà calculés
 * (KPIs, tendances, alertes) et d'une période, l'IA rédige un COURT récit narratif factuel
 * destiné à une revue de direction. L'IA raconte, l'humain décide (§12.3).
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT validé (règle 18.2 #2). La taille du
 * lot d'indicateurs est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous IA).
 * Les chiffres source sont renvoyés tels quels dans la réponse (explicabilité §12.3) : le
 * lecteur peut confronter le récit aux données qui l'ont produit.
 */
public final class StoryboardDto {

    private StoryboardDto() {}

    /**
     * Un point d'indicateur à commenter. {@code label} et {@code value} obligatoires (le strict
     * minimum « libellé / valeur »). {@code trend} et {@code target} optionnels : s'ils sont
     * présents, l'IA les exploite (progression, écart à la cible) ; sinon ils sont ignorés.
     * {@code unit} optionnel (ex. « %, j, ppm ») pour un récit précis.
     */
    public record IndicatorPoint(
            @NotBlank @Size(max = 200) String label,
            @NotBlank @Size(max = 80) String value,
            @Size(max = 80) String trend,
            @Size(max = 80) String target,
            @Size(max = 32) String unit
    ) {}

    /**
     * Requête de storyboard. {@code period} obligatoire (ex. « Mai 2026 », « T1 2026 ») : le
     * récit la cite. {@code points} borné (1..50) pour rester factuel et borner le coût IA.
     * {@code context} optionnel (ex. « Site de Lyon, atelier mécanique ») pour situer le récit.
     */
    public record StoryboardRequest(
            @NotBlank @Size(max = 120) String period,
            @Size(max = 400) String context,
            @NotNull @NotEmpty @Size(max = 50) @Valid List<IndicatorPoint> points
    ) {}

    /**
     * Résultat : le récit narratif IA + les chiffres source qui l'ont produit (explicabilité)
     * + métadonnées (fournisseur LLM, période). {@code sources} est une copie fidèle des
     * indicateurs reçus (rappel des chiffres pour validation humaine).
     */
    public record StoryboardResponse(
            String narrative,
            String provider,
            String period,
            List<IndicatorPoint> sources
    ) {}
}
