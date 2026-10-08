package com.openlab.qualitos.quality.circuit.application;

import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce qu'un module métier demande aux circuits de validation (ADR 0080).
 *
 * <p>Tant que le client n'a pas réglé de circuit pour un type d'objet, les deux
 * méthodes rendent {@link Optional#empty()} : le module garde son approbation
 * simple d'avant. Le client et l'acteur viennent du jeton.
 */
public interface ApprovalCircuits {

    /** L'objet entre en revue : son passage commence, si un circuit est réglé. */
    Optional<CircuitDto.RunView> start(CircuitSubject subject, UUID subjectId, UUID authorId);

    /** L'utilisateur du jeton approuve ou refuse l'étape en cours, s'il y a un passage ouvert. */
    Optional<CircuitDto.RunView> decide(CircuitSubject subject, UUID subjectId, boolean approve, String comment);
}
