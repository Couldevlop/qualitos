package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.common.SearchText;

import java.util.regex.Pattern;

/**
 * Une recherche dans l'annuaire, validée (ADR 0091).
 *
 * <p>Les bornes empêchent qu'une seule requête aspire tout l'annuaire ou fasse
 * travailler la base sur un motif démesuré. Les messages de refus sont fixes :
 * ils ne reprennent jamais la valeur reçue (pas de reflet d'entrée).
 *
 * @param pattern motif LIKE « contient » sur la clé de recherche, déjà échappé
 */
public record DirectoryQuery(String pattern, int page, int size) {

    public static final int MAX_TERM_LENGTH = 100;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** 50 × 1 000 pages : au-delà, c'est un parcours, pas une recherche. */
    public static final int MAX_PAGE = 999;

    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

    public static DirectoryQuery of(String q, Integer page, Integer size) {
        String term = q == null ? "" : q.strip();
        if (term.length() > MAX_TERM_LENGTH) {
            throw new IllegalArgumentException("Search term too long (max " + MAX_TERM_LENGTH + " characters)");
        }
        if (CONTROL.matcher(term).find()) {
            throw new IllegalArgumentException("Search term contains control characters");
        }
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_PAGE_SIZE : size;
        if (p < 0 || p > MAX_PAGE) {
            throw new IllegalArgumentException("Page must be between 0 and " + MAX_PAGE);
        }
        if (s < 1 || s > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return new DirectoryQuery(SearchText.containsPattern(SearchText.normalize(term)), p, s);
    }
}
