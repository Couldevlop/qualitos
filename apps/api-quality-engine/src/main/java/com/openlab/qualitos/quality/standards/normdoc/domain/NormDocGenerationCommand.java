package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.util.List;
import java.util.Objects;

/**
 * Commande de génération d'un document normatif (domaine pur). Porte le contexte
 * tenant non sensible (nom, secteur, taille, langue, processus connus) et la
 * structure des sections à rédiger. Aucune PII, aucun tenant_id technique.
 */
public final class NormDocGenerationCommand {

    /** Section à rédiger : titre + clauses couvertes + consigne facultative. */
    public record SectionRequest(String key, String title, List<String> clauses, String guidance) {
        public SectionRequest {
            key = requireText(key, "section key");
            title = requireText(title, "section title");
            clauses = clauses == null ? List.of() : List.copyOf(clauses);
            guidance = guidance == null ? "" : guidance;
        }
        private static String requireText(String v, String field) {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException(field + " required");
            }
            return v;
        }
    }

    private final NormDocKind kind;
    private final String standardCode;
    private final String standardName;
    private final String organizationName;
    private final String industry;
    private final String size;
    private final String language;
    private final List<String> knownProcesses;
    private final List<SectionRequest> sections;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public NormDocGenerationCommand(NormDocKind kind, String standardCode, String standardName,
                                    String organizationName, String industry, String size,
                                    String language, List<String> knownProcesses,
                                    List<SectionRequest> sections) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.standardCode = requireText(standardCode, "standardCode");
        this.standardName = requireText(standardName, "standardName");
        this.organizationName = requireText(organizationName, "organizationName");
        this.industry = requireText(industry, "industry");
        this.size = requireText(size, "size");
        this.language = (language == null || language.isBlank()) ? "fr" : language;
        this.knownProcesses = knownProcesses == null ? List.of() : List.copyOf(knownProcesses);
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("at least one section required");
        }
        this.sections = List.copyOf(sections);
    }

    private static String requireText(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return v;
    }

    public NormDocKind kind() { return kind; }
    public String standardCode() { return standardCode; }
    public String standardName() { return standardName; }
    public String organizationName() { return organizationName; }
    public String industry() { return industry; }
    public String size() { return size; }
    public String language() { return language; }
    public List<String> knownProcesses() { return knownProcesses; }
    public List<SectionRequest> sections() { return sections; }
}
