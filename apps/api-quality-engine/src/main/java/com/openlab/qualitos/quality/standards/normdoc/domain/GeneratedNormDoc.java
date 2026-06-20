package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.util.List;

/**
 * Résultat de la génération IA : titre + sections rédigées + provider effectif
 * (value object retourné par le port {@link NormDocGenerator}).
 */
public record GeneratedNormDoc(
        String title,
        List<NormDocSection> sections,
        String provider) {

    public GeneratedNormDoc {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("at least one section required");
        }
        sections = List.copyOf(sections);
        provider = provider == null ? "" : provider;
    }
}
