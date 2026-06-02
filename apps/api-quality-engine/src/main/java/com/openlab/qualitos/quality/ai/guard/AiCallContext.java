package com.openlab.qualitos.quality.ai.guard;

/**
 * Contexte d'un appel sortant vers la passerelle IA, évalué par {@link AiGuard}
 * avant tout départ réseau.
 *
 * <p>Le {@code tenantId} provient du {@code TenantContext} (claim JWT, CLAUDE.md
 * §18.2-2) : les quotas et le disjoncteur sont cloisonnés par tenant pour qu'un
 * locataire ne puisse pas épuiser le modèle au détriment des autres (OWASP
 * LLM04 — Model Denial of Service).
 *
 * @param tenantId   tenant courant (ou {@code "unknown"} si absent du contexte)
 * @param operation  opération logique (ex. {@code "complete"}, {@code "nlq"}) — pour les logs/métriques
 * @param promptChars taille cumulée du prompt (système + utilisateur) en caractères
 */
public record AiCallContext(String tenantId, String operation, int promptChars) {

    public AiCallContext {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "unknown";
        }
        if (operation == null || operation.isBlank()) {
            operation = "ai";
        }
        if (promptChars < 0) {
            promptChars = 0;
        }
    }
}
