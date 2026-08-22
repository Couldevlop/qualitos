package com.openlab.qualitos.quality.controlplan.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance du control plan. Interface posée dans le domaine : c'est
 * lui qui dicte ce dont il a besoin, l'infrastructure s'y plie.
 */
public interface ControlPlanRepository {

    ControlPlan save(ControlPlan plan);

    Optional<ControlPlan> findById(UUID id);

    List<ControlPlan> findByProduct(UUID tenantId, UUID productId);

    Optional<ControlPlan> findActive(UUID tenantId, UUID productId, ControlPlanPhase phase);

    Optional<ControlPlan> findDraft(UUID tenantId, UUID productId, ControlPlanPhase phase);

    ControlPlanLine saveLine(ControlPlanLine line);

    List<ControlPlanLine> linesOf(UUID planId);

    Optional<ControlPlanLine> findLine(UUID id);

    void deleteLine(UUID id);
}
