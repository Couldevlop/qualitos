package com.openlab.qualitos.quality.change.domain;

/**
 * Les seuils qui classent un score d'impact, réglés par client (ADR 0087).
 *
 * <p>Score &lt; {@code major} : mineur ; de {@code major} à {@code critical} - 1 :
 * majeur ; à partir de {@code critical} : critique. Par défaut 25 et 50 : la
 * demande de la maquette (13 points sur 24, soit 54) est critique.
 *
 * <p>Invariant : {@code 0 < major < critical <= 100}. Un seuil majeur à 0
 * ferait de tout changement évalué au moins un « majeur », et un seuil critique
 * au-delà de 100 ne serait jamais atteint : deux réglages qui vident une
 * classification de son sens.
 */
public record MocThresholds(int major, int critical) {

    public static final MocThresholds DEFAULT = new MocThresholds(25, 50);

    public MocThresholds {
        if (major <= 0 || major >= critical || critical > 100) {
            throw MocException.invalid("moc.settings.thresholds",
                    "Les seuils doivent respecter 0 < seuil majeur < seuil critique <= 100.");
        }
    }

    /** La classification d'un score complet (0 à 100). */
    public MocClassification classify(int score) {
        if (score >= critical) {
            return MocClassification.CRITICAL;
        }
        return score >= major ? MocClassification.MAJOR : MocClassification.MINOR;
    }
}
