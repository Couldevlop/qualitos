package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * Les objets d'autres modules auxquels une exigence peut être rattachée.
 *
 * <p>C'est le lien qui fait du registre des parties intéressées autre chose
 * qu'un tableau : une exigence rattachée à un risque, à une non-conformité, à
 * un audit et à un document devient traçable de bout en bout — c'est le
 * référentiel transverse de §3.6.
 */
public enum RequirementLink {
    RISK,
    NON_CONFORMITY,
    AUDIT,
    DOCUMENT
}
