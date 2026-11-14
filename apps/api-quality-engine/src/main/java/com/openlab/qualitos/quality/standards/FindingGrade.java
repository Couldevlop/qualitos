package com.openlab.qualitos.quality.standards;

/**
 * Classement d’un écart au référentiel, dans le vocabulaire et selon les
 * critères des normes en vigueur — et non selon une échelle maison.
 *
 * <p><b>Pourquoi ce value object.</b> L’audit blanc décidait qu’un écart était
 * majeur quand l’exigence portait un « risque » HIGH ou CRITICAL, une échelle à
 * quatre niveaux saisie à la main sur chaque exigence du catalogue. Aucune norme
 * ne porte cette échelle, et la graduation d’un constat n’est pas une propriété
 * de la clause : elle dépend de ce que l’auditeur trouve, le jour de l’audit, en
 * face de la clause. Un client qui lisait « majeure » sur une clause dont il
 * détenait pourtant la moitié des preuves préparait une réponse à un reproche
 * qu’il n’entendrait pas ; l’inverse était plus grave encore.
 *
 * <p><b>Ce que disent les textes.</b> Trois sources, et trois seulement.
 * <ul>
 *   <li><b>ISO/IEC 17021-1:2015, 3.12 et 3.13</b> (exigences pour les organismes
 *       qui certifient les systèmes de management) définissent les deux seuls
 *       degrés qu’un certificateur emploie : une non-conformité <b>majeure</b>
 *       est celle qui <i>met en cause l’aptitude du système de management à
 *       atteindre les résultats escomptés</i> ; une non-conformité
 *       <b>mineure</b> est celle qui ne la met pas en cause. Le mot
 *       « critique » n’y figure pas.</li>
 *   <li><b>ISO 19011:2018, 6.4.8</b> admet que les constats soient gradués selon
 *       le contexte et les risques, qualitativement ou quantitativement, et
 *       <i>ne prescrit aucune échelle</i>. C’est une permission, pas un
 *       barème : le barème est à justifier, ce que fait ce fichier.</li>
 *   <li><b>ISO/IEC Directives, Partie 2</b> fixe le sens des verbes d’une norme :
 *       « doit » (<i>shall</i>) énonce une exigence, « devrait »
 *       (<i>should</i>) une recommandation, « peut » (<i>may</i>) une
 *       permission. Seul le premier peut être manqué.</li>
 * </ul>
 * ISO 9001:2015 ne gradue pas, elle : son 10.2.1 demande une action
 * « appropriée aux effets » du manquement — la proportionnalité, pas des
 * niveaux.
 *
 * <p><b>La règle, et sa trace.</b> Deux faits suffisent, tous deux vérifiables.
 * <ol>
 *   <li>Le <b>verbe de la norme</b> (l’obligation portée par l’exigence) dit si
 *       le manquement <i>peut</i> être une non-conformité. Une recommandation
 *       manquée n’en est pas une : c’est une observation, ce qu’ISO 19011
 *       appelle une piste d’amélioration.</li>
 *   <li>L’<b>état des preuves de la clause</b> dit lequel des deux degrés
 *       s’applique. Aucune preuve dans toute la clause : la disposition demandée
 *       est absente, le système ne peut pas produire le résultat escompté —
 *       <b>majeure</b>, au sens littéral de 3.12. Des preuves existent dans la
 *       clause mais n’atteignent pas cette exigence : la disposition est en
 *       place et tenue imparfaitement, c’est un manquement isolé —
 *       <b>mineure</b>.</li>
 * </ol>
 * La clause est la bonne maille : c’est l’unité que la norme elle-même donne à
 * une disposition, et c’est celle sur laquelle un certificateur conclut. Elle
 * porte aussi, sans seuil à inventer, le critère d’accumulation des organismes
 * accrédités — plusieurs manquements mineurs sur une même disposition valent
 * une majeure : quand ils portent sur <i>toutes</i> les exigences de la clause,
 * la clause est sans preuve, et la règle rend déjà « majeure ».
 *
 * <p>Le niveau de risque saisi sur une exigence ne classe plus rien. Il garde
 * son seul emploi légitime : ordonner le programme d’audit (ISO 19011, 6.3,
 * approche par les risques), où il dit par quoi commencer, jamais ce qu’un
 * auditeur conclura.
 */
public enum FindingGrade {

    /** Non-conformité majeure : l’aptitude du système est en cause (17021-1, 3.12). */
    MAJOR(1),

    /** Non-conformité mineure : manquement isolé, aptitude non remise en cause (3.13). */
    MINOR(2),

    /** Observation : pas une non-conformité ; piste d’amélioration (ISO 19011). */
    OBSERVATION(3);

    private final int remediationPriority;

    FindingGrade(int remediationPriority) {
        this.remediationPriority = remediationPriority;
    }

    /** Ordre de traitement : les majeures d’abord, elles bloquent le certificat. */
    public int remediationPriority() {
        return remediationPriority;
    }

    /**
     * Classe un écart constaté sur une exigence non couverte.
     *
     * @param obligation        le verbe de la norme pour cette exigence ; une
     *                          recommandation ne peut pas être manquée
     * @param clauseHasEvidence vrai si au moins une exigence de la MÊME clause
     *                          est démontrée par une preuve côté client
     */
    public static FindingGrade of(ObligationLevel obligation, boolean clauseHasEvidence) {
        if (obligation != ObligationLevel.MUST) {
            return OBSERVATION;
        }
        return clauseHasEvidence ? MINOR : MAJOR;
    }
}
