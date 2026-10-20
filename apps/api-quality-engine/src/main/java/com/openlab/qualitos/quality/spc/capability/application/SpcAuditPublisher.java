package com.openlab.qualitos.quality.spc.capability.application;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;

import java.util.UUID;

/**
 * Port d'audit (OWASP A09) : toute écriture sur une étude SPC laisse une trace
 * dans le journal chaîné. Un Cpk justifie la libération d'un procédé ; qu'il ait
 * été recalculé, et par qui, doit pouvoir se retrouver.
 */
public interface SpcAuditPublisher {

    void recorded(CapabilityStudy study, UUID actor);

    void revised(CapabilityStudy study, UUID actor);

    void actionPlanChanged(CapabilityStudy study, UUID actor);

    void deleted(CapabilityStudy study, UUID actor);

    void thresholdsChanged(UUID tenantId, CapabilityThresholds thresholds, UUID actor);
}
