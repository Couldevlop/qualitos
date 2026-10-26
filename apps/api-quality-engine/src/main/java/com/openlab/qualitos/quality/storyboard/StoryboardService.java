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
    private static final int MAX_TOKENS = 700;

    private static final java.util.regex.Pattern NUMBER =
            java.util.regex.Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final java.util.regex.Pattern COMPARISON =
            java.util.regex.Pattern.compile("^(<=|>=|<|>|≤|≥)\\s*(\\d+(?:[.,]\\d+)?)$");
    private static final java.util.regex.Pattern FRACTION =
            java.util.regex.Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\d+(?:[.,]\\d+)?)$");

    private final AiGatewayClient ai;

    public StoryboardService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public StoryboardDto.StoryboardResponse generate(StoryboardDto.StoryboardRequest request) {
        String system = buildSystemPrompt();
        String user = buildUserPrompt(request);
        AiCompletionResult r = ai.complete(system, user, MAX_TOKENS);
        String narrative = r.text() == null ? "" : r.text().strip();
        return new StoryboardDto.StoryboardResponse(
                narrative,
                r.provider(),
                request.period(),
                request.points(),
                unverifiedNumbers(narrative, request));
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
                + "la cible) ; sinon ne la mentionne pas. "
                + "Le statut de cible indiqué (ATTEINTE ou NON ATTEINTE) est calculé : reprends-le "
                + "tel quel, ne le contredis jamais. N'évoque aucune valeur antérieure, aucune "
                + "autre période et aucune année qui ne figurent pas dans les données. "
                + "Termine toujours ta dernière phrase.";
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
                Boolean met = targetMet(p.value(), p.target());
                if (met != null) {
                    sb.append(" ; statut : cible ").append(met ? "ATTEINTE" : "NON ATTEINTE");
                }
            }
            sb.append('\n');
        }
        sb.append("Rédige le récit.");
        return sb.toString();
    }

    /**
     * La cible est-elle atteinte ? Calcul déterministe, pour que le modèle n'ait pas à
     * comparer lui-même des nombres (un petit modèle s'y trompe). {@code null} quand la
     * valeur ou la cible ne se lit pas : rien n'est alors affirmé.
     */
    static Boolean targetMet(String value, String target) {
        if (value == null || target == null) {
            return null;
        }
        String t = target.strip();
        java.util.regex.Matcher frac = FRACTION.matcher(t);
        java.util.regex.Matcher val = FRACTION.matcher(value.strip());
        if (frac.matches() && val.matches()) {
            return num(val.group(1)) / num(val.group(2)) >= num(frac.group(1)) / num(frac.group(2));
        }
        java.util.regex.Matcher cmp = COMPARISON.matcher(t);
        java.util.regex.Matcher v = NUMBER.matcher(value);
        if (!cmp.matches() || !v.find()) {
            return null;
        }
        double x = num(v.group());
        double bound = num(cmp.group(2));
        return switch (cmp.group(1)) {
            case "<" -> x < bound;
            case "<=", "≤" -> x <= bound;
            case ">" -> x > bound;
            default -> x >= bound;
        };
    }

    /**
     * Les nombres du récit qu'on ne retrouve dans aucune donnée fournie (valeur, tendance,
     * cible, période, contexte). Ce n'est pas une preuve d'erreur, c'est une alerte : le
     * lecteur sait quels chiffres vérifier avant de reprendre le texte.
     */
    static List<String> unverifiedNumbers(String narrative, StoryboardDto.StoryboardRequest request) {
        java.util.Set<Double> known = new java.util.HashSet<>();
        StringBuilder sources = new StringBuilder(request.period()).append(' ').append(request.context());
        for (StoryboardDto.IndicatorPoint p : request.points()) {
            sources.append(' ').append(p.value()).append(' ').append(p.trend()).append(' ').append(p.target());
        }
        NUMBER.matcher(sources).results().forEach(m -> known.add(num(m.group())));
        java.util.Set<String> unknown = new java.util.LinkedHashSet<>();
        NUMBER.matcher(narrative).results()
                .filter(m -> !known.contains(num(m.group())))
                .forEach(m -> unknown.add(m.group()));
        return List.copyOf(unknown);
    }

    private static double num(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** Exposé pour les tests : permet de vérifier la construction déterministe du prompt. */
    List<String> debugPrompts(StoryboardDto.StoryboardRequest request) {
        return List.of(buildSystemPrompt(), buildUserPrompt(request));
    }
}
