package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.util.Set;

/**
 * La forme de réponse qu'attend une question : du texte, ou l'une des listes
 * fermées du classeur (Oui/Non, Oui/Non/N.A., Oui/Non/Inconnu).
 *
 * <p>Les listes fermées enregistrent un code ({@code YES}, {@code NA}…) et non
 * un libellé : l'écran le traduit, et une réponse saisie en français se relit
 * en anglais sans perdre son sens.
 */
public enum AnswerType {
    TEXT(Set.of()),
    YES_NO(Set.of("YES", "NO")),
    YES_NO_NA(Set.of("YES", "NO", "NA")),
    YES_NO_UNKNOWN(Set.of("YES", "NO", "UNKNOWN"));

    /** Longueur maximale d'une réponse libre. */
    public static final int TEXT_MAX = 2000;

    private final Set<String> choices;

    AnswerType(Set<String> choices) {
        this.choices = choices;
    }

    public Set<String> choices() {
        return choices;
    }

    /** Vrai si la réponse (déjà nettoyée, non vide) convient à cette question. */
    public boolean accepts(String answer) {
        if (this == TEXT) {
            return answer.length() <= TEXT_MAX;
        }
        return choices.contains(answer);
    }
}
