package com.openlab.qualitos.quality.nonconformity.eightd.domain;

/**
 * Les huit disciplines, dans l'ordre de la méthode.
 *
 * <p>L'ordre n'est pas décoratif : D4 ne se comprend qu'après D3, et D7 ne veut
 * rien dire sans D5. L'énumération porte donc la position, et le rapport la suit
 * sans la recalculer.
 *
 * <p>Chaque discipline dit aussi <b>d'où vient son contenu</b> :
 * {@link #estSaisie()} distingue les trois qui n'ont aucune source dans la
 * plateforme (l'équipe, l'endiguement, la reconnaissance) des cinq qui agrègent
 * ce que les autres modules savent déjà. C'est la frontière qui justifie
 * l'existence du module : on n'ajoute un champ de saisie que là où rien ne
 * répond.
 */
public enum EightDDiscipline {

    /** L'équipe qui a traité l'écart. Aucune source : se saisit. */
    D1("D1", "Équipe", true),

    /** Le problème tel qu'il a été constaté. Source : la non-conformité. */
    D2("D2", "Description du problème", false),

    /** Ce qui a protégé le client en attendant la cause. Aucune source : se saisit. */
    D3("D3", "Actions d'endiguement immédiates", true),

    /** La cause racine. Sources : Ishikawa, 5 pourquoi, cause racine de la NC. */
    D4("D4", "Cause racine", false),

    /** Les actions correctives décidées. Source : la CAPA escaladée. */
    D5("D5", "Actions correctives retenues", false),

    /** Leur mise en œuvre et ses preuves. Source : la CAPA escaladée. */
    D6("D6", "Mise en œuvre et preuves", false),

    /** Ce qui empêche le retour de l'écart. Sources : PFMEA, plan de surveillance. */
    D7("D7", "Prévention de la récurrence", false),

    /** La reconnaissance de l'équipe. Aucune source : se saisit. */
    D8("D8", "Reconnaissance de l'équipe", true);

    private final String code;
    private final String titre;
    private final boolean saisie;

    EightDDiscipline(String code, String titre, boolean saisie) {
        this.code = code;
        this.titre = titre;
        this.saisie = saisie;
    }

    public String code() {
        return code;
    }

    /** Le titre tel qu'il paraît dans le document émis. */
    public String titre() {
        return titre;
    }

    /**
     * Vrai quand la plateforme n'a rien à agréger et que seul un humain peut
     * renseigner la discipline.
     */
    public boolean estSaisie() {
        return saisie;
    }
}
