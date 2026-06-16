package com.openlab.qualitos.quality.storyboard;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service applicatif du Storyboard IA (CLAUDE.md §7.4) : construit un prompt DÉTERMINISTE à
 * partir des indicateurs fournis (KPIs, tendances, alertes) puis appelle la passerelle IA
 * ({@link AiGatewayClient#complete}, op « complete », garde-fou LLM04 déjà intégré). Le LLM
 * réel (Ollama/Anthropic + fallback) est servi par {@code ai-service} ; l'engine ne fait que
 * construire le prompt et présenter le résultat (clean architecture, comme SPC/Forecast).
 *
 * <p>Anti-hallucination (§21 « Hallucination LLM ») : le prompt impose de commenter
 * UNIQUEMENT les chiffres fournis, sans en inventer. Les chiffres source sont renvoyés dans
 * la réponse (explicabilité §12.3). Le tenant provient du JWT (via {@code TenantContext} dans
 * {@link AiGatewayClient}), jamais du body (règle 18.2 #2).
 */
// Nom de bean explicite : évite la collision avec quality.standards.StoryboardService
// (même nom simple de classe → nom de bean auto identique → ConflictingBeanDefinition au boot).
@Service("aiStoryboardService")
public class StoryboardService {

    /** Plafond de génération : un récit court (3 à 6 phrases) tient largement. */
    private static final int MAX_TOKENS = 320;

    private final AiGatewayClient ai;

    public StoryboardService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public StoryboardDto.StoryboardResponse generate(StoryboardDto.StoryboardRequest request) {
        String system = buildSystemPrompt();
        String user = buildUserPrompt(request);
        AiCompletionResult r = ai.complete(system, user, MAX_TOKENS);
        return new StoryboardDto.StoryboardResponse(
                r.text() == null ? "" : r.text().strip(),
                r.provider(),
                request.period(),
                request.points());
    }

    /**
     * Consigne système déterministe (français, factuel, anti-hallucination). Identique d'un
     * appel à l'autre : seul le bloc « données » varie, ce qui rend le prompt reproductible.
     */
    private String buildSystemPrompt() {
        return "Tu es analyste qualité. Rédige en français un COURT récit narratif "
                + "(3 à 6 phrases, un seul paragraphe fluide, ton professionnel de revue de "
                + "direction) qui commente l'évolution des indicateurs fournis sur la période "
                + "indiquée : ce qui progresse, ce qui se dégrade, les points d'attention, et "
                + "une recommandation finale concise. "
                + "Règles STRICTES : commente UNIQUEMENT les chiffres fournis ci-dessous ; "
                + "n'invente AUCUN chiffre, aucune cause et aucun fait non présent dans les "
                + "données ; pas de liste à puces, pas de titres, pas de tableau. "
                + "Si une tendance ou une cible est fournie, exploite-la (progression, écart à "
                + "la cible) ; sinon ne la mentionne pas.";
    }

    /**
     * Bloc « données » : période, contexte optionnel, puis un indicateur par ligne. Format
     * stable et lisible par le LLM (libellé : valeur [unité] [tendance] [cible]).
     */
    private String buildUserPrompt(StoryboardDto.StoryboardRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Période : ").append(request.period().strip()).append(".\n");
        if (notBlank(request.context())) {
            sb.append("Contexte : ").append(request.context().strip()).append(".\n");
        }
        sb.append("Indicateurs :\n");
        for (StoryboardDto.IndicatorPoint p : request.points()) {
            sb.append("- ").append(p.label().strip()).append(" : ").append(p.value().strip());
            if (notBlank(p.unit())) {
                sb.append(' ').append(p.unit().strip());
            }
            if (notBlank(p.trend())) {
                sb.append(" ; tendance ").append(p.trend().strip());
            }
            if (notBlank(p.target())) {
                sb.append(" ; cible ").append(p.target().strip());
            }
            sb.append('\n');
        }
        sb.append("Rédige le récit.");
        return sb.toString();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** Exposé pour les tests : permet de vérifier la construction déterministe du prompt. */
    List<String> debugPrompts(StoryboardDto.StoryboardRequest request) {
        return List.of(buildSystemPrompt(), buildUserPrompt(request));
    }
}
