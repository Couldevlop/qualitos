package com.openlab.qualitos.quality.nonconformity.eightd.domain;

/**
 * Les huit disciplines, dans l'ordre de la méthode.
 *
 * <p>L'ordre n'est pas décoratif : D4 ne se comprend qu'après D3, et D7 ne veut
 * rien dire sans D5. L'énumération porte donc la position, et le rapport la suit
 * sans la recalculer.
 *
 * <p>Chaque discipline dit aussi <b>d'où vient son contenu</b> :
 * {@link #estSaisie()} ne distingue plus qu'UNE discipline sans source dans la
 * plateforme, la reconnaissance. L'équipe vient de la fiche de l'écart et
 * l'endiguement de son dossier CAPA : les redemander faisait retaper ce que la
 * plateforme savait déjà, et une discipline qu'on ressaisit est une discipline
 * qu'on oublie. C'est la frontière qui justifie l'existence du module : on
 * n'ajoute un champ de saisie que là où rien ne répond (ADR 0139).
 */
public enum EightDDiscipline {

    /** L'équipe qui a traité l'écart. Source : l'équipe nommée sur la fiche. */
    D1("D1", "Équipe", false),

    /** Le problème tel qu'il a été constaté. Source : la non-conformité. */
    D2("D2", "Description du problème", false),

    /** Ce qui a protégé le client en attendant la cause. Source : les actions du dossier. */
    // « D'ENDIGUEMENT » EN MOINS : le mot est du jargon de méthode, et cette
    // discipline se lit par un client. « Actions immédiates » dit la même
    // chose. Un rapport déjà scellé garde le titre qu'il porte dans son
    // instantané : son empreinte tient à cela.
    D3("D3", "Actions immédiates", false),

    /** La cause racine. Sources : Ishikawa, 5 pourquoi, cause racine de la NC. */
    D4("D4", "Cause racine", false),

    /**
     * Les CAPA définies — correctives et préventives. Source : la CAPA escaladée.
     *
     * <p>L'endiguement n'y figure pas : il a sa propre discipline, le D3, juste
     * au-dessus. Montré sous les deux, le même geste s'y lisait deux fois, et
     * deux tableaux portant les mêmes lignes laissaient croire à deux décisions
     * distinctes.
     */
    D5("D5", "CAPA définies", false),

    /** Leur mise en œuvre et ses preuves. Source : la CAPA escaladée. */
    D6("D6", "Mise en oeuvre et preuves", false),

    /** Ce qui empêche le retour de l'écart. Sources : PFMEA, plan de surveillance. */
    D7("D7", "Prévention de la récidive", false),

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

    /**
     * Le titre tel qu'il paraît dans le document émis.
     *
     * <p>En FRANÇAIS, et fixé ICI plutôt que dans la langue du lecteur : un
     * document opposable ne change pas de mots selon qui l'affiche, et son
     * empreinte est calculée sur le texte qu'il portait le jour de l'émission.
     * Le jour où un client anglophone en demandera un, la réponse est de figer
     * la langue À L'ÉMISSION et de la stocker avec l'instantané (ADR 0071),
     * jamais de traduire à la lecture.
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
