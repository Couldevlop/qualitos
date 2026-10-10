package com.openlab.qualitos.quality.circuit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Les vues des circuits de validation. Des codes et des identifiants ; les mots se composent à l'écran. */
public final class CircuitDto {

    private CircuitDto() {}

    public record StepView(String name, String roleCode, int minApprovals) {}

    /** Le circuit d'un type d'objet ; sans étape, l'approbation simple d'avant s'applique. */
    public record CircuitView(String subject, List<StepView> steps) {}

    public record DecisionView(int stepIndex, UUID actorId, boolean approved, String comment, Instant at) {}

    /** Le passage d'un objet dans son circuit. */
    public record RunView(UUID id, String subject, UUID subjectId, String status, int currentStep,
                          List<StepView> steps, List<DecisionView> decisions, Instant startedAt, Instant endedAt) {}
}
