package com.openlab.qualitos.quality.capa.effectiveness.domain;

/**
 * Le jugement porté sur l'efficacité d'une CAPA, et ce qu'il entraîne pour le
 * dossier (ADR 0108).
 *
 * <p><b>Pourquoi trois états et non deux.</b> La vérification était un booléen.
 * Une action qui fait passer le rebut de 4,2 % à 2,5 % pour une cible de 2 %
 * n'est ni efficace ni inefficace : le booléen oblige à mentir dans un sens ou
 * dans l'autre. Répondre « non » rouvre un dossier qui avançait et efface le
 * progrès du registre ; répondre « oui » clôt au-dessus d'un objectif non
 * atteint — et c'est la seconde réponse qu'on choisit, parce qu'elle fait
 * avancer le tableau de bord.
 *
 * <p><b>La conséquence est portée ici</b>, par l'énumération, et non par une
 * suite de conditions dans le service. Ajouter un état sans décider de son effet
 * sur le dossier devient alors impossible à oublier : le compilateur réclame les
 * deux réponses.
 *
 * <p><b>{@code PARTIAL} et {@code INEFFECTIVE} ramènent tous deux le dossier en
 * traitement, et ce n'est pas une raison de les confondre.</b> « Partiellement
 * efficace » dit : la cause était la bonne, l'action ne va pas assez loin — on
 * en ajoute une. « Inefficace » dit : la cause n'était pas celle-là — on reprend
 * l'analyse. Ce sont deux travaux différents, confiés à deux moments différents
 * du raisonnement.
 */
public enum EffectivenessVerdict {

    /**
     * Le critère est atteint. Le dossier se clôt.
     *
     * <p>Seul ce verdict déclenche les garde-fous de la clôture — actions toutes
     * réalisées, pas d'endiguement seul, aucune non-conformité rattachée encore
     * ouverte. Ils protègent la clôture, et rien d'autre.
     */
    EFFECTIVE(true, false),

    /**
     * Effet réel, critère non atteint. Une action complémentaire est attendue.
     *
     * <p>Aucune n'est créée automatiquement : une action engendrée par la
     * machine n'a ni responsable ni échéance choisis, et un dossier CAPA n'a pas
     * besoin d'une ligne que personne n'a voulue. L'écran la réclame et la
     * nomme ; la personne la définit.
     */
    PARTIAL(false, true),

    /**
     * Aucun effet. La cause a vraisemblablement été mal identifiée : l'analyse
     * est à reprendre (5 Pourquoi, Ishikawa).
     */
    INEFFECTIVE(false, false);

    private final boolean closesCase;
    private final boolean complementaryActionExpected;

    EffectivenessVerdict(boolean closesCase, boolean complementaryActionExpected) {
        this.closesCase = closesCase;
        this.complementaryActionExpected = complementaryActionExpected;
    }

    /** Le dossier se clôt-il sur ce verdict ? Sinon, il repart en traitement. */
    public boolean closesCase() {
        return closesCase;
    }

    /**
     * Une action complémentaire est-elle attendue ? Vrai pour {@code PARTIAL}
     * seulement : sur {@code INEFFECTIVE}, ce n'est pas une action de plus qu'il
     * faut, c'est une autre cause.
     */
    public boolean complementaryActionExpected() {
        return complementaryActionExpected;
    }

    /**
     * La projection sur l'ancien booléen {@code effectiveness_verified}, conservé
     * pour l'écran d'efficacité mesurée et pour les dossiers antérieurs à
     * l'ADR 0108.
     *
     * <p>{@code PARTIAL} y vaut {@code false}, et c'est une perte d'information
     * assumée : ce booléen n'a jamais su dire autre chose que oui ou non. Le
     * verdict reste la source de vérité.
     */
    public boolean asLegacyVerifiedFlag() {
        return closesCase;
    }

    /**
     * Le verdict correspondant à l'ancien champ booléen de l'API.
     *
     * <p>{@code false} devient {@code INEFFECTIVE} et non {@code PARTIAL} :
     * l'ancien champ ne pouvait pas exprimer la nuance, et la supposer serait
     * inventer un jugement que personne n'a porté.
     */
    public static EffectivenessVerdict fromLegacyFlag(boolean effective) {
        return effective ? EFFECTIVE : INEFFECTIVE;
    }
}
