package com.openlab.qualitos.quality.standards.auditblanc.domain;

/**
 * Port de génération IA d'un audit blanc (Standards Hub §8.4 onglet 7).
 * L'adapter d'infrastructure relaie la commande à la passerelle IA
 * ({@code ai-service} via {@code AiGatewayClient}) qui rédige réellement les
 * questions et les constats — aucune question/écart en dur. Le tenant provient
 * du JWT côté passerelle (jamais du body, CLAUDE.md §18.2 #2/#4).
 */
public interface MockAuditGenerator {

    GeneratedMockAudit generate(MockAuditGenerationCommand command);
}
