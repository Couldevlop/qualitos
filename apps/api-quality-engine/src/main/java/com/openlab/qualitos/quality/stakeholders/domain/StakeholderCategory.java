package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * Les familles de parties intéressées, au sens de l'ISO 9001 §4.2.
 *
 * <p>Ce sont des rôles, pas des secteurs : un hôpital, une banque et une usine
 * ont tous des clients, des fournisseurs, des collaborateurs, une autorité de
 * tutelle et un certificateur. La liste ne porte donc aucun vocabulaire
 * sectoriel, et n'a pas à être paramétrable comme l'est la nature d'un écart
 * (ADR 0103).
 */
public enum StakeholderCategory {

    /** Ceux à qui le produit ou le service est destiné. */
    CUSTOMERS,

    /** Ceux dont dépend ce qu'on produit. */
    SUPPLIERS,

    /** Les salariés et leurs représentants. */
    EMPLOYEES,

    /** Direction, actionnaires, conseil. */
    GOVERNANCE,

    /** Les autorités dont l'activité relève. */
    REGULATORS,

    /** Les organismes de certification et d'accréditation. */
    CERTIFICATION_BODIES,

    /** Les partenaires commerciaux, hors fournisseurs. */
    BUSINESS_PARTNERS,

    /** Les prestataires de services (métrologie, informatique, maintenance…). */
    SERVICE_PROVIDERS,

    /** Assureurs, banques et autres partenaires financiers. */
    FINANCIAL_PARTNERS,

    /** Riverains, collectivités, associations, et autres acteurs externes. */
    LOCAL_COMMUNITIES,

    /** Ce que la liste ci-dessus ne couvre pas. */
    OTHER
}
