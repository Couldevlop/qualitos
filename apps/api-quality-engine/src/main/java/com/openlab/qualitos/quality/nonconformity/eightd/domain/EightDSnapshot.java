package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import java.util.List;
import java.util.Objects;

/**
 * Le contenu FIGÉ d'un rapport 8D : huit sections de texte, déjà mises en forme.
 *
 * <p>C'est le cœur de la décision de conception. Le rapport ne conserve pas des
 * identifiants vers les objets qu'il a agrégés, mais <b>le texte qu'il en a
 * tiré</b>. Deux raisons, toutes deux des raisons de preuve :
 *
 * <ol>
 *   <li>Un 8D émis doit dire, dans dix ans, ce qu'il disait le jour de la
 *       clôture. Des identifiants le feraient varier avec les objets qu'ils
 *       désignent — une CAPA rouverte, un Ishikawa complété, un plan de
 *       surveillance révisé — et le document ne vaudrait plus rien.</li>
 *   <li>L'empreinte scellée est calculée sur le PDF. Pour qu'elle reste
 *       vérifiable, le rendu doit être une fonction PURE de ce qu'on stocke :
 *       pas d'horloge, pas de requête, pas d'identifiant à résoudre. D'où des
 *       lignes de texte, et des dates déjà formatées (même leçon que l'ADR
 *       0062 : une empreinte ne se calcule que sur des valeurs stockables).</li>
 * </ol>
 *
 * <p>Les enregistrements sont immuables et sans dépendance technique : le même
 * objet sert au rendu PDF, à la réponse HTTP et à la sérialisation JSON.
 */
public record EightDSnapshot(
        String ncReference,
        String ncTitle,
        String tenantLabel,
        String issuedAtText,
        String issuedByName,
        boolean partial,
        List<Section> sections) {

    public EightDSnapshot {
        ncReference = requireText(ncReference, "ncReference");
        ncTitle = requireText(ncTitle, "ncTitle");
        sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
        if (sections.size() != EightDDiscipline.values().length) {
            throw new IllegalArgumentException(
                    "un rapport 8D porte exactement " + EightDDiscipline.values().length
                    + " sections, reçu " + sections.size());
        }
    }

    /**
     * Une discipline rendue.
     *
     * @param code         « D1 » … « D8 »
     * @param title        le titre de la discipline
     * @param sourced      vrai si du contenu a été trouvé ou saisi
     * @param sourceLabel  d'où vient le contenu, ou pourquoi il manque — jamais vide :
     *                     une section vide et silencieuse laisse croire qu'il n'y avait
     *                     rien à dire, alors qu'elle dit seulement qu'on n'a pas cherché
     * @param lines        le contenu, ligne à ligne, déjà mis en forme
     */
    public record Section(
            String code,
            String title,
            boolean sourced,
            String sourceLabel,
            List<String> lines) {

        public Section {
            code = requireText(code, "code");
            title = requireText(title, "title");
            sourceLabel = requireText(sourceLabel, "sourceLabel");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    /** Les codes des disciplines qui n'ont rien à montrer — ce qui rend le rapport partiel. */
    public List<String> missingCodes() {
        return sections.stream().filter(s -> !s.sourced()).map(Section::code).toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " requis");
        }
        return value;
    }
}
