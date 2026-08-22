package com.openlab.qualitos.quality.revisionrequests.domain;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rapproche le texte d'une non-conformité des modes de défaillance déjà analysés.
 *
 * <p>Il ne décide de rien : il ordonne des candidats qu'un humain confirme.
 * Rattacher une NC au mauvais mode fausserait une cotation d'occurrence, et une
 * cotation fausse se propage dans toute l'analyse de risque.
 *
 * <p>Fruste et déterministe, à dessein. Il tourne dans le chemin de création d'une
 * non-conformité : y brancher un service d'inférence ferait dépendre la saisie
 * d'un défaut au poste de la disponibilité de ce service. Le jour où l'on voudra
 * des plongements lexicaux, on fournira une autre implémentation.
 */
public final class FailureModeMatcher {

    /** En deçà, le recouvrement relève du hasard : mieux vaut ne rien proposer. */
    private static final double THRESHOLD = 0.2;

    private static final Set<String> STOP_WORDS = Set.of(
            "sur", "dans", "avec", "pour", "par", "les", "des", "une", "aux", "que",
            "qui", "est", "son", "sa", "ses", "the", "and", "for", "with", "from",
            "this", "that", "was", "are", "not", "lot", "piece", "part", "constatee",
            "constate");

    private FailureModeMatcher() {}

    public record Candidate(UUID fmeaItemId, String failureMode, String failureEffect) {}

    public record Suggestion(UUID fmeaItemId, double score, String matchedTerms) {}

    public static List<Suggestion> suggest(String ncText, List<Candidate> candidates, int limit) {
        Set<String> left = terms(ncText);
        if (left.isEmpty() || candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> score(left, candidate))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Suggestion::score).reversed())
                .limit(limit)
                .toList();
    }

    private static Suggestion score(Set<String> left, Candidate candidate) {
        Set<String> right = terms(candidate.failureMode() + " " + candidate.failureEffect());
        if (right.isEmpty()) return null;

        Set<String> shared = new TreeSet<>(left);
        shared.retainAll(right);
        if (shared.isEmpty()) return null;

        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        double jaccard = (double) shared.size() / union.size();
        if (jaccard < THRESHOLD) return null;

        return new Suggestion(candidate.fmeaItemId(), jaccard, String.join(", ", shared));
    }

    /** Minuscules, accents dépliés, ponctuation retirée, mots vides et mots courts écartés. */
    private static Set<String> terms(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String folded = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(folded.split("\\s+"))
                .filter(term -> term.length() >= 3)
                .filter(term -> !STOP_WORDS.contains(term))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
