package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.util.List;
import java.util.Objects;

/**
 * Section rédigée d'un document normatif (value object immuable du domaine pur).
 * Porte sa clé, son titre, les clauses couvertes et son corps Markdown.
 */
public final class NormDocSection {

    private final String key;
    private final String title;
    private final List<String> clauses;
    private final String bodyMarkdown;

    public NormDocSection(String key, String title, List<String> clauses, String bodyMarkdown) {
        this.key = requireText(key, "section key");
        this.title = requireText(title, "section title");
        this.clauses = clauses == null ? List.of() : List.copyOf(clauses);
        this.bodyMarkdown = bodyMarkdown == null ? "" : bodyMarkdown;
    }

    private static String requireText(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return v;
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public List<String> getClauses() { return clauses; }
    public String getBodyMarkdown() { return bodyMarkdown; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NormDocSection that)) return false;
        return key.equals(that.key) && title.equals(that.title)
                && clauses.equals(that.clauses) && bodyMarkdown.equals(that.bodyMarkdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, title, clauses, bodyMarkdown);
    }
}
