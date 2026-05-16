package com.openlab.qualitos.quality.training;

/**
 * Échelle de compétence (§4.7). Inspirée de Dreyfus + ITIL 4 People Competencies.
 *
 * 0 NONE         : aucune connaissance
 * 1 AWARE        : connaissance théorique
 * 2 PRACTITIONER : applique sous supervision
 * 3 COMPETENT    : autonome
 * 4 EXPERT       : capable de former, faire référence, contribuer aux référentiels
 *
 * L'entier ordinal sert directement de niveau atteint, pour faciliter
 * les comparaisons SQL et le calcul de gap.
 */
public enum CompetencyLevel {
    NONE,
    AWARE,
    PRACTITIONER,
    COMPETENT,
    EXPERT;

    public int level() { return ordinal(); }

    public static CompetencyLevel ofLevel(int level) {
        if (level < 0 || level >= values().length) {
            throw new IllegalArgumentException("Invalid level: " + level);
        }
        return values()[level];
    }
}
