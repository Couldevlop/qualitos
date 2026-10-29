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
    /** Un nombre et son signe collé ; « 10-12 » n'est pas un -12 (le tiret suit un chiffre). */
    private static final java.util.regex.Pattern SIGNED =
            java.util.regex.Pattern.compile("(?<![\\w,.])([-+−])?(\\d+(?:[.,]\\d+)?)");
    /** « de 3 % à 1,8 % » : une valeur antérieure, que les données ne fournissent jamais. */
    private static final java.util.regex.Pattern TRANSITION = java.util.regex.Pattern.compile(
            "\\bde\\s+[-+−]?(\\d+(?:[.,]\\d+)?)\\s*(?:%|j|jours?|pts?|points?)?\\s+à\\s+[-+−]?\\d+(?:[.,]\\d+)?"
                    + "(?:\\s*(?:%|j|jours?|pts?|points?)(?!\\w))?",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
    private static final java.util.regex.Pattern UP = java.util.regex.Pattern.compile(
            "hausse|augment|accroiss|croissance|grimp|\\bmont[ée]");
    private static final java.util.regex.Pattern DOWN = java.util.regex.Pattern.compile(
            "baisse|diminu|recul|\\bchut");

    private final AiGatewayClient ai;

    public StoryboardService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public StoryboardDto.StoryboardResponse generate(StoryboardDto.StoryboardRequest request) {
        String system = buildSystemPrompt();
        String user = buildUserPrompt(request);
        AiCompletionResult r = ai.complete(system, user, MAX_TOKENS);
        String narrative = text(r);
        List<String> errors = errors(narrative, request);
        if (errors.isEmpty()) {
            return response(narrative, r.provider(), request, List.of(), "ai");
        }
        // Une seconde demande, au même fournisseur, qui nomme les erreurs relevées.
        AiCompletionResult retry = ai.complete(system, user
                + "\n\nTon récit précédent comportait des erreurs : " + String.join(" ; ", errors)
                + ". Réécris-le en n'utilisant que les valeurs données ci-dessus, sans valeur "
                + "antérieure ni évolution « de X à Y », et en respectant le sens de chaque tendance.",
                MAX_TOKENS);
        String corrected = text(retry);
        if (!corrected.isEmpty() && errors(corrected, request).isEmpty()) {
            return response(corrected, retry.provider(), request, List.of(), "ai-corrected");
        }
        // Deux fois un récit faux : on ne le publie pas, le moteur rédige le récit
        // factuel à partir des mêmes données.
        return response(factualNarrative(request), retry.provider(), request, List.of(), "factual");
    }

    /**
     * Ce qui rend un récit faux alors même que ses nombres viennent des données : un
     * chiffre absent (ou de signe inversé), une évolution « de X à Y » que les données ne
     * contiennent pas (elles n'ont aucune valeur antérieure), un sens contraire à la
     * tendance fournie. Vide : le récit peut être publié.
     */
    static List<String> errors(String narrative, StoryboardDto.StoryboardRequest request) {
        List<String> errors = new java.util.ArrayList<>();
        List<String> unknown = unverifiedNumbers(narrative, request);
        if (!unknown.isEmpty()) {
            errors.add("chiffres absents des données : " + String.join(", ", unknown));
        }
        // « en hausse de 1,2 pt à 97,4 % » part d'une tendance fournie : ce n'est pas une valeur antérieure.
        java.util.Set<Double> trends = new java.util.HashSet<>();
        for (StoryboardDto.IndicatorPoint p : request.points()) {
            if (p.trend() != null) {
                NUMBER.matcher(p.trend()).results().forEach(m -> trends.add(num(m.group())));
            }
        }
        TRANSITION.matcher(narrative).results()
                .filter(m -> !trends.contains(num(m.group(1))))
                .forEach(m -> errors.add("évolution inventée « " + m.group().strip() + " »"));
        for (String sentence : narrative.split("(?<=[.!?])\\s+")) {
            StoryboardDto.IndicatorPoint p = onlyIndicatorOf(sentence, request);
            if (p == null) {
                continue;
            }
            Integer trend = direction(p.trend());
            String s = sentence.toLowerCase(java.util.Locale.ROOT);
            boolean up = UP.matcher(s).find();
            boolean down = DOWN.matcher(s).find();
            boolean wrong = trend != null && (trend == 0 ? up || down : trend > 0 ? down && !up : up && !down);
            if (wrong) {
                errors.add("« " + p.label().strip() + " » a pour tendance " + p.trend().strip()
                        + ", le récit dit le contraire");
            }
        }
        return errors;
    }

    /** L'indicateur dont parle une phrase, par son libellé ou sa valeur ; null s'il y en a zéro ou plusieurs. */
    private static StoryboardDto.IndicatorPoint onlyIndicatorOf(String sentence, StoryboardDto.StoryboardRequest request) {
        String s = sentence.toLowerCase(java.util.Locale.ROOT);
        java.util.Set<Double> numbers = new java.util.HashSet<>();
        NUMBER.matcher(sentence).results().forEach(m -> numbers.add(num(m.group())));
        StoryboardDto.IndicatorPoint found = null;
        for (StoryboardDto.IndicatorPoint p : request.points()) {
            java.util.regex.Matcher v = NUMBER.matcher(p.value());
            boolean byValue = v.find() && numbers.contains(num(v.group()));
            if (byValue || s.contains(p.label().strip().toLowerCase(java.util.Locale.ROOT))) {
                if (found != null) {
                    return null;
                }
                found = p;
            }
        }
        return found;
    }

    /** Sens d'une tendance fournie : +1, -1, 0 pour « stable » ; null quand elle ne se lit pas. */
    static Integer direction(String trend) {
        if (trend == null || trend.isBlank()) {
            return null;
        }
        String t = trend.strip().toLowerCase(java.util.Locale.ROOT);
        if (t.contains("stable")) {
            return 0;
        }
        java.util.regex.Matcher signed = SIGNED.matcher(t);
        if (signed.find() && signed.group(1) != null) {
            return signed.group(1).equals("+") ? 1 : -1;
        }
        if (UP.matcher(t).find()) {
            return 1;
        }
        return DOWN.matcher(t).find() ? -1 : null;
    }

    private static StoryboardDto.StoryboardResponse response(String narrative, String provider,
                                                             StoryboardDto.StoryboardRequest request,
                                                             List<String> unverified, String mode) {
        return new StoryboardDto.StoryboardResponse(narrative, provider, request.period(),
                request.points(), unverified, mode);
    }

    private static String text(AiCompletionResult r) {
        return r == null || r.text() == null ? "" : r.text().strip();
    }

    /**
     * Le récit sans IA : une phrase par indicateur, avec sa valeur, sa tendance et le
     * statut de sa cible calculé par {@link #targetMet}. Il ne contient que des chiffres
     * fournis — il ne peut pas être faux.
     */
    static String factualNarrative(StoryboardDto.StoryboardRequest request) {
        StringBuilder sb = new StringBuilder("Sur la période ").append(request.period().strip());
        if (request.context() != null && !request.context().isBlank()) {
            sb.append(" (").append(request.context().strip()).append(')');
        }
        sb.append(" :");
        for (StoryboardDto.IndicatorPoint p : request.points()) {
            sb.append(' ').append(p.label().strip()).append(" s'établit à ").append(p.value().strip());
            if (p.unit() != null && !p.unit().isBlank()) {
                sb.append(' ').append(p.unit().strip());
            }
            if (p.trend() != null && !p.trend().isBlank()) {
                sb.append(", tendance ").append(p.trend().strip());
            }
            if (p.target() != null && !p.target().isBlank()) {
                Boolean met = targetMet(p.value(), p.target());
                sb.append(met == null ? " ; cible " + p.target().strip()
                        : met ? " ; la cible " + p.target().strip() + " est atteinte"
                        : " ; la cible " + p.target().strip() + " n'est pas atteinte");
            }
            sb.append('.');
        }
        return sb.toString();
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
        SIGNED.matcher(sources).results().forEach(m -> known.add(signed(m)));
        java.util.Set<String> unknown = new java.util.LinkedHashSet<>();
        // Un nombre sans signe peut reprendre une tendance négative (« recule de 12 % ») ;
        // un nombre signé doit avoir le signe des données (« -2 % » pour une cible « < 2 » est faux).
        SIGNED.matcher(narrative).results()
                .filter(m -> m.group(1) != null ? !known.contains(signed(m))
                        : !known.contains(num(m.group(2))) && !known.contains(-num(m.group(2))))
                .forEach(m -> unknown.add(m.group().strip()));
        return List.copyOf(unknown);
    }

    private static double signed(java.util.regex.MatchResult m) {
        double x = num(m.group(2));
        return m.group(1) == null || m.group(1).equals("+") ? x : -x;
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
