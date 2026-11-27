package com.openlab.qualitos.quality.capa.effectiveness.domain;

import java.util.function.BooleanSupplier;

/**
 * Le verdict porté sur une ACTION solde-t-il le dossier qui la porte
 * (ADR 0143) ?
 *
 * <p>Depuis l'ADR 0135, une CAPA porte une action et une seule : juger l'action
 * depuis la discipline 8D — D5 pour une corrective, D7 pour une préventive —,
 * c'est juger le dossier. Redemander ensuite une évaluation du dossier sur
 * l'écran d'efficacité faisait porter DEUX fois le même jugement sur le même
 * geste, et le second ne pouvait rien apprendre de plus que le premier.
 *
 * <p>Trois conditions, et les trois sont nécessaires :
 * <ul>
 *   <li>le verdict CLÔT — {@code EFFECTIVE} ; « partiellement » et
 *       « inefficace » demandent au contraire la suite du travail ;</li>
 *   <li>le dossier est RÉSOLU — toutes ses actions sont faites, et c'est la
 *       seule situation où l'on pouvait déjà le clore à la main ;</li>
 *   <li>plus rien ne s'oppose à la clôture : l'écart rattaché est refermé,
 *       les autres vérifications promises ont conclu, l'objectif qualité est
 *       revenu sous sa cible (ADR 0133). Le verdict d'une action n'est donc
 *       PAS une porte dérobée — c'est la même liste d'obstacles que celle
 *       opposée au verdict du dossier (ADR 0137).</li>
 * </ul>
 *
 * <p>Fonction pure, et à l'écart du service : la question « ce jugement
 * ferme-t-il le dossier ? » est une règle métier, elle se lit et se teste sans
 * base ni contexte de sécurité (CLAUDE.md §18.3).
 */
public final class CaseSettlement {

    private CaseSettlement() {}

    /**
     * @param actionVerdict le verdict qui vient d'être porté sur l'action, ou
     *                      {@code null} si aucun
     * @param caseResolved  le dossier est-il déclaré résolu ?
     * @param closureClear  la liste des obstacles à la clôture est-elle vide ?
     *                      DEMANDÉE EN DERNIER, et seulement si le reste est
     *                      réuni : l'établir coûte des lectures — l'écart
     *                      rattaché, les autres vérifications promises,
     *                      l'objectif qualité — et un verdict qui ne clôt rien
     *                      n'a aucune raison de les payer
     * @return vrai quand ce verdict clôt le dossier sans autre formalité
     */
    public static boolean settlesCase(EffectivenessVerdict actionVerdict,
                                      boolean caseResolved, BooleanSupplier closureClear) {
        return actionVerdict != null && actionVerdict.closesCase()
                && caseResolved && closureClear.getAsBoolean();
    }
}
