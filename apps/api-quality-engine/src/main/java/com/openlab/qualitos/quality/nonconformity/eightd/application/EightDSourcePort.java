package com.openlab.qualitos.quality.nonconformity.eightd.application;

import java.util.UUID;

/**
 * Port — rassemble ce que les autres modules savent de la non-conformité.
 *
 * <p>Un seul port, et non six dépendances directes : le cas d'usage a besoin de
 * « l'état du dossier », pas de savoir que l'Ishikawa se lit par {@code ncId}
 * quand les 5 pourquoi se lisent par association JPA. L'adaptateur porte ces
 * détails, le service porte la méthode.
 */
public interface EightDSourcePort {

    /**
     * @throws com.openlab.qualitos.quality.nonconformity.NcNotFoundException si la
     *         non-conformité n'existe pas dans ce tenant — l'absence ne se
     *         distingue pas d'un accès à un autre tenant, et c'est voulu
     *         (OWASP A01 : pas d'oracle d'existence).
     */
    EightDSources collect(UUID tenantId, UUID ncId);
}
