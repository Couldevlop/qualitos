package com.openlab.qualitos.quality.crossbordertransfers.domain;

/**
 * Mécanismes autorisés pour les transferts internationaux (RGPD Chapitre V).
 * <ul>
 *   <li>ADEQUACY_DECISION — décision d'adéquation de la Commission (Art. 45).</li>
 *   <li>STANDARD_CONTRACTUAL_CLAUSES — clauses contractuelles types (Art. 46.2.c-d).</li>
 *   <li>BINDING_CORPORATE_RULES — règles d'entreprise contraignantes (Art. 47).</li>
 *   <li>CODE_OF_CONDUCT — code de conduite approuvé (Art. 46.2.e).</li>
 *   <li>CERTIFICATION — mécanisme de certification (Art. 46.2.f).</li>
 *   <li>DEROGATION_ART49 — dérogation pour situation particulière (Art. 49).
 *       Doit être motivée et reste exceptionnelle.</li>
 * </ul>
 */
public enum TransferMechanism {
    ADEQUACY_DECISION,
    STANDARD_CONTRACTUAL_CLAUSES,
    BINDING_CORPORATE_RULES,
    CODE_OF_CONDUCT,
    CERTIFICATION,
    DEROGATION_ART49;

    public boolean requiresDerogationJustification() {
        return this == DEROGATION_ART49;
    }
}
