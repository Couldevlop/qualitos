package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.ai.guard.AiGuardException;
import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiRequestRejectedException;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Les trois assistants du module, servis par la passerelle IA (ADR 0014, 0102).
 *
 * <p>Trois règles, et elles tiennent tout :
 * <ol>
 *   <li><b>L'IA ne sait pas écrire.</b> Cette classe rend des propositions ; le
 *       service ne les enregistre jamais de lui-même. La validation est un appel
 *       distinct, fait par quelqu'un (§12.3).</li>
 *   <li><b>Une sortie illisible rend une liste vide, pas une erreur.</b> Un
 *       modèle qui répond à côté ne doit pas casser un écran de registre : on
 *       journalise et on ne propose rien, ce qui se voit et se réessaie.</li>
 *   <li><b>Une exigence extraite porte sa citation.</b> Une exigence sans
 *       passage cité ne se vérifie pas — elle est écartée ici même, avant d'être
 *       proposée à un humain qui n'aurait aucun moyen de la contrôler.</li>
 * </ol>
 */
public class AiStakeholderAssistant implements StakeholderPorts.Assistant {

    private static final Logger log = LoggerFactory.getLogger(AiStakeholderAssistant.class);

    /** Au-delà, la liste n'est plus examinée, elle est subie. */
    static final int MAX_SUGGESTIONS = 12;
    static final int MAX_REQUIREMENTS = 20;
    static final int MAX_CHANGES = 15;

    /**
     * Le texte envoyé au modèle est borné : un contrat entier coûte sans servir.
     *
     * <p>Cette borne n'est pas libre. L'invite assemblée — consigne système comprise —
     * doit tenir sous {@code qualitos.ai.guard.max-prompt-chars} (20 000, ADR 0017),
     * qui est aussi la borne de {@code ai-service}. On réserve de quoi loger la consigne
     * système (~1 200 caractères) et l'en-tête du document, d'où 17 000. Au-delà,
     * l'appel était refusé en bloc : l'utilisateur voyait « rien trouvé » sur un
     * document parfaitement lisible.
     */
    static final int MAX_DOCUMENT_CHARS = StakeholderPorts.Assistant.DOCUMENT_MAX;

    /**
     * Comparer deux versions envoie deux textes dans la même invite : chacun n'a donc
     * droit qu'à la moitié du budget, moins les deux en-têtes de version.
     */
    static final int MAX_COMPARED_CHARS = StakeholderPorts.Assistant.COMPARED_MAX;

    private final AiGatewayClient ai;
    private final ObjectMapper mapper;

    public AiStakeholderAssistant(AiGatewayClient ai, ObjectMapper mapper) {
        this.ai = ai;
        this.mapper = mapper;
    }

    // ---------- 1. suggérer des parties intéressées ----------

    private static final String SUGGEST_SYSTEM = """
            Tu assistes un responsable qualité qui dresse le registre des parties
            intéressées de son organisme, au sens de l'ISO 9001 §4.2.

            Rends UNIQUEMENT un objet JSON de la forme :
            {"suggestions":[{"name":"...","category":"...","rationale":"..."}]}

            category vaut exactement l'une de ces valeurs :
            CUSTOMERS, SUPPLIERS, EMPLOYEES, GOVERNANCE, REGULATORS,
            CERTIFICATION_BODIES, BUSINESS_PARTNERS, SERVICE_PROVIDERS,
            FINANCIAL_PARTNERS, LOCAL_COMMUNITIES, OTHER.

            Règles :
            - propose des TYPES de parties intéressées, jamais des noms
              d'entreprises réelles que tu ne peux pas connaître ;
            - rationale dit en une phrase pourquoi elle est pertinente pour CE
              secteur et CETTE organisation ;
            - au plus 12 suggestions, sans doublon ;
            - aucun texte hors du JSON.
            """;

    @Override
    public List<StakeholderPorts.SuggestedStakeholder> suggestStakeholders(String sector,
                                                                           String characteristics) {
        String prompt = "Secteur d'activité : " + sector + "\n"
                + "Caractéristiques de l'organisme : " + (characteristics.isBlank() ? "non précisées"
                        : characteristics);
        JsonNode root = ask(SUGGEST_SYSTEM, prompt, 1200, "suggestStakeholders");
        if (root == null) {
            return List.of();
        }
        List<StakeholderPorts.SuggestedStakeholder> out = new ArrayList<>();
        for (JsonNode node : root.path("suggestions")) {
            if (out.size() >= MAX_SUGGESTIONS) {
                break;
            }
            String name = text(node, "name");
            StakeholderCategory category = category(text(node, "category"));
            if (name == null || category == null) {
                continue;
            }
            out.add(new StakeholderPorts.SuggestedStakeholder(name, category, text(node, "rationale")));
        }
        return List.copyOf(out);
    }

    // ---------- 2. extraire les exigences d'un document ----------

    private static final String EXTRACT_SYSTEM = """
            Tu lis un contrat ou un cahier des charges pour en tirer les exigences
            opposables à l'organisme.

            Rends UNIQUEMENT un objet JSON de la forme :
            {"requirements":[{"quote":"...","reference":"...","requirement":"...",
                              "nature":"...","control":"..."}]}

            nature vaut exactement l'une de ces valeurs : CONTRACTUAL,
            REGULATORY, NORMATIVE, VOLUNTARY, INTERNAL.

            Règles impératives :
            - quote est le passage EXACT du document, recopié mot pour mot. Si tu
              ne peux pas citer, n'inclus pas l'exigence ;
            - reference dit où le retrouver (article, paragraphe, page) ;
            - requirement reformule l'exigence telle que l'organisme s'engagerait
              à la tenir, à l'infinitif ;
            - control propose une mesure de maîtrise réaliste ;
            - n'invente aucune exigence qui ne soit pas dans le texte ;
            - au plus 20 exigences ; aucun texte hors du JSON.
            """;

    @Override
    public List<StakeholderPorts.ExtractedRequirement> extractRequirements(String documentName,
                                                                            String documentText) {
        // Le texte effectivement soumis : c'est à CELUI-LÀ que les citations seront
        // confrontées, pas au document d'origine. Confronter à ce que le modèle n'a
        // pas vu rejetterait des citations exactes tombées après la troncature.
        String submitted = clip(documentText, MAX_DOCUMENT_CHARS);
        String prompt = "Document : " + documentName + "\n\n--- Texte ---\n" + submitted;
        JsonNode root = ask(EXTRACT_SYSTEM, prompt, 2000, "extractRequirements");
        if (root == null) {
            return List.of();
        }
        String haystack = flatten(submitted);
        List<StakeholderPorts.ExtractedRequirement> out = new ArrayList<>();
        for (JsonNode node : root.path("requirements")) {
            if (out.size() >= MAX_REQUIREMENTS) {
                break;
            }
            String quote = text(node, "quote");
            String requirement = text(node, "requirement");
            // Pas de citation, pas d'exigence : un humain n'aurait aucun moyen
            // de contrôler ce qu'on lui propose de faire entrer au registre.
            if (quote == null || requirement == null) {
                continue;
            }
            // Et une citation qui ne figure PAS dans le document n'est pas une
            // citation : c'est l'apparence d'une preuve. Le document vient d'un
            // tiers et traverse l'invite ; un contrat qui s'y adresse au modèle
            // obtiendrait sinon une exigence inventée, munie d'un passage inventé
            // — exactement l'élément sur lequel le relecteur s'appuie pour valider
            // (OWASP LLM01/LLM02). C'est la seule vérification locale et certaine
            // dont on dispose, elle ne coûte rien, elle est faite.
            if (!haystack.contains(flatten(quote))) {
                log.warn("[stakeholders] extractRequirements : citation absente du document, exigence écartée");
                continue;
            }
            out.add(new StakeholderPorts.ExtractedRequirement(quote, text(node, "reference"),
                    requirement, nature(text(node, "nature")), text(node, "control")));
        }
        return List.copyOf(out);
    }

    // ---------- 3. détecter les changements ----------

    private static final String CHANGES_SYSTEM = """
            Tu compares deux versions d'un même contrat ou cahier des charges et
            tu dis ce qui a changé pour l'organisme.

            Rends UNIQUEMENT un objet JSON de la forme :
            {"changes":[{"title":"...","detail":"...","source":"...","impact":"..."}]}

            impact vaut exactement LOW, MEDIUM ou HIGH.

            Règles :
            - title dit le changement en une ligne, chiffré quand le texte le
              chiffre (« taux de service porté de 95 % à 98 % ») ;
            - detail dit ce que la nouvelle version exige ;
            - source dit où le constater dans la nouvelle version ;
            - impact estime la portée sur les exigences déjà en place ;
            - ne signale que ce qui CHANGE une obligation : ignore la mise en
              forme, la numérotation et les reformulations sans effet ;
            - au plus 15 changements ; aucun texte hors du JSON.
            """;

    @Override
    public List<StakeholderPorts.DetectedChange> detectChanges(String documentName, String previousText,
                                                                String currentText) {
        String prompt = "Document : " + documentName
                + "\n\n--- Version précédente ---\n" + clip(previousText, MAX_COMPARED_CHARS)
                + "\n\n--- Version en vigueur ---\n" + clip(currentText, MAX_COMPARED_CHARS);
        JsonNode root = ask(CHANGES_SYSTEM, prompt, 1800, "detectChanges");
        if (root == null) {
            return List.of();
        }
        List<StakeholderPorts.DetectedChange> out = new ArrayList<>();
        for (JsonNode node : root.path("changes")) {
            if (out.size() >= MAX_CHANGES) {
                break;
            }
            String title = text(node, "title");
            if (title == null) {
                continue;
            }
            out.add(new StakeholderPorts.DetectedChange(title, text(node, "detail"),
                    text(node, "source"), impact(text(node, "impact"))));
        }
        return List.copyOf(out);
    }

    // ---------- lecture de la réponse ----------

    /**
     * Interroge le modèle et rend l'objet JSON de sa réponse, ou {@code null}
     * si elle n'en contient pas.
     *
     * <p>Les modèles encadrent volontiers leur JSON de texte ou de balises de
     * bloc de code ; on isole donc la première accolade et la dernière, plutôt
     * que d'exiger une réponse parfaite.
     */
    private JsonNode ask(String system, String prompt, int maxTokens, String what) {
        try {
            AiCompletionResult result = ai.complete(system, prompt, maxTokens);
            String body = result == null ? null : result.text();
            if (body == null || body.isBlank()) {
                log.warn("[stakeholders] {} : réponse vide du modèle", what);
                return null;
            }
            int start = body.indexOf('{');
            int end = body.lastIndexOf('}');
            if (start < 0 || end <= start) {
                log.warn("[stakeholders] {} : la réponse ne contient pas d'objet JSON", what);
                return null;
            }
            return mapper.readTree(body.substring(start, end + 1));
        } catch (AiGuardException | AiRequestRejectedException e) {
            // Un plafond atteint, un quota épuisé, un disjoncteur ouvert, une invite
            // refusée : ce ne sont pas des réponses illisibles, ce sont des refus, et
            // ils ont leur code HTTP (413/429/503/422, ADR 0017). Les confondre avec
            // « rien trouvé » rendait un abus de débit indiscernable d'un document
            // sans exigence, et ne laissait qu'une ligne de journal (OWASP LLM04, A09).
            throw e;
        } catch (Exception e) {
            // Une panne de l'IA ne doit pas casser un écran de registre : on ne
            // propose rien, ce qui se voit, et l'utilisateur réessaie.
            log.warn("[stakeholders] {} a échoué : {}", what, e.getClass().getSimpleName());
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String t = value.asText("").strip();
        return t.isEmpty() ? null : t;
    }

    private static StakeholderCategory category(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return StakeholderCategory.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // Une catégorie inconnue vaut mieux que rien : la suggestion garde
            // son intérêt, l'utilisateur la reclassera.
            return StakeholderCategory.OTHER;
        }
    }

    private static MandatoryNature nature(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return MandatoryNature.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ImpactLevel impact(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ImpactLevel.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String clip(String text, int max) {
        String t = text.strip();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /**
     * Replie un texte pour comparer une citation à son document : espaces, sauts de
     * ligne et tabulations réduits à une espace unique, casse ignorée.
     *
     * <p>Un modèle recopie fidèlement les mots et bien plus rarement la mise en page
     * — une césure, une double espace, une majuscule de début de ligne. Comparer les
     * chaînes brutes écarterait des citations authentiques ; c'est la présence du
     * passage qu'on vérifie, pas sa typographie.
     */
    private static String flatten(String text) {
        return text.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }
}
