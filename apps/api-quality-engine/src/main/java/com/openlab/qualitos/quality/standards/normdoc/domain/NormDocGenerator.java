package com.openlab.qualitos.quality.standards.normdoc.domain;

/**
 * Port de génération IA d'un document normatif (Standards Hub §8.8). L'adapter
 * d'infrastructure relaie la commande à la passerelle IA (ai-service via
 * {@code AiGatewayClient}) — aucun document en dur. Le tenant provient du JWT
 * côté passerelle (jamais du body, CLAUDE.md §18.2 #2/#4).
 */
public interface NormDocGenerator {

    GeneratedNormDoc generate(NormDocGenerationCommand command);
}
