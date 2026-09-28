package com.openlab.qualitos.quality.costofquality.application;

import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;

import java.util.UUID;

/**
 * Port d'audit (OWASP A09) : toute écriture sur une ligne de coût laisse une
 * trace dans le journal chaîné. Un montant de non-qualité alimente une revue de
 * direction ; qu'il ait été modifié, et par qui, doit pouvoir se retrouver.
 */
public interface CoqAuditPublisher {

    void recorded(CoqEntry entry, UUID actor);

    void revised(CoqEntry entry, UUID actor);

    void deleted(CoqEntry entry, UUID actor);
}
