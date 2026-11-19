package com.openlab.qualitos.quality.nonconformity;

import java.util.UUID;

/**
 * L'analyse de risque du produit a-t-elle ete revisee POUR CET ECART ?
 *
 * <p>Un ecart declare impactant le PFMEA ou le plan de surveillance ne se clot
 * pas tant que la revision correspondante n'est pas actee : un defaut survenu
 * pour de bon et une analyse de risque qui l'ignore encore se contredisent sur
 * la meme page (IATF 16949 -- toute non-conformite se repercute sur la PFMEA et
 * le plan de surveillance).
 *
 * <p>UN PORT, et non un appel direct au module des revisions. Celui-ci depend
 * deja de la non-conformite -- il ecoute ses evenements et lit ses fiches --
 * et l'appeler d'ici fermerait le cycle. La fleche garde donc son sens :
 * c'est le module des revisions qui vient brancher son adaptateur.
 *
 * <p>Le tenant voyage explicitement : cette question se pose aussi hors d'une
 * requete, et dependre du contexte d'execution y serait fragile.
 */
public interface NcRiskRevisionsPort {

    /**
     * Une revision du PFMEA declenchee par cet ecart est-elle ACCEPTEE ?
     *
     * <p>Acceptee, et non simplement proposee : une proposition en attente
     * montre qu'on a vu la question, pas qu'on l'a tranchee. Une proposition
     * rejetee ne compte pas non plus -- elle dit qu'on ne revise pas, ce qui
     * contredit la declaration d'impact. Dans ce cas la sortie est de corriger
     * la reponse, pas de forcer la cloture.
     */
    boolean pfmeaRevisedFor(UUID tenantId, UUID ncId);

    /** Idem pour le plan de surveillance : les deux documents se revisent a part. */
    boolean controlPlanRevisedFor(UUID tenantId, UUID ncId);
}
