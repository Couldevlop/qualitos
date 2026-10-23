package com.openlab.qualitos.quality.spc.gagerr.application;

import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;

import java.util.UUID;

/**
 * Port d'audit (OWASP A09) : toute écriture sur une étude R&amp;R laisse une
 * trace. Un %GRR libère ou bloque un instrument ; qu'il ait été recalculé, et
 * par qui, doit pouvoir se retrouver.
 */
public interface GageRrAuditPublisher {

    void recorded(GageRrStudy study, UUID actor);

    void revised(GageRrStudy study, UUID actor);

    void deleted(GageRrStudy study, UUID actor);
}
