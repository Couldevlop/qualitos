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
 * plateforme (l'équipe, la sécurisation, la reconnaissance) des cinq qui agrègent
 * ce que les autres modules savent déjà. C'est la frontière qui justifie
 * l'existence du module : on n'ajoute un champ de saisie que là où rien ne
 * répond.
 */
public enum EightDDiscipline {

    /** L'équipe qui a traité l'écart. Aucune source : se saisit. */
    D1("D1", "Team", true),

    /** Le problème tel qu'il a été constaté. Source : la non-conformité. */
    D2("D2", "Problem description", false),

    /** Ce qui a protégé le client en attendant la cause. Aucune source : se saisit. */
    D3("D3", "Immediate containment actions", true),

    /** La cause racine. Sources : Ishikawa, 5 pourquoi, cause racine de la NC. */
    D4("D4", "Root cause", false),

    /** Les actions correctives décidées. Source : la CAPA escaladée. */
    D5("D5", "Chosen corrective actions", false),

    /** Leur mise en œuvre et ses preuves. Source : la CAPA escaladée. */
    D6("D6", "Implementation and evidence", false),

    /** Ce qui empêche le retour de l'écart. Sources : PFMEA, plan de surveillance. */
    D7("D7", "Recurrence prevention", false),

    /** La reconnaissance de l'équipe. Aucune source : se saisit. */
    D8("D8", "Team recognition", true);

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

    /**
     * Le titre tel qu'il paraît dans le document émis.
     *
     * <p>En ANGLAIS, et non dans la langue de l'interface : le 8D est remis à un
     * client, la pratique est anglophone, et un document opposable ne change pas
     * de langue selon qui l'affiche. L'interface autour reste traduite.
     */
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
