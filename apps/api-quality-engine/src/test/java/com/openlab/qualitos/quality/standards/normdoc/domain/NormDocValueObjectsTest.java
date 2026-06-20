package com.openlab.qualitos.quality.standards.normdoc.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormDocValueObjectsTest {

    @Test
    void section_requiresKeyAndTitle() {
        assertThatThrownBy(() -> new NormDocSection(" ", "t", List.of(), "b"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
        assertThatThrownBy(() -> new NormDocSection(null, "t", List.of(), "b"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
        assertThatThrownBy(() -> new NormDocSection("k", " ", List.of(), "b"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> new NormDocSection("k", null, List.of(), "b"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void section_equalsShortCircuitsOnEachField() {
        NormDocSection base = new NormDocSection("k", "t", List.of("1"), "b");
        assertThat(base).isNotEqualTo(new NormDocSection("X", "t", List.of("1"), "b")); // key
        assertThat(base).isNotEqualTo(new NormDocSection("k", "X", List.of("1"), "b")); // title
        assertThat(base).isNotEqualTo(new NormDocSection("k", "t", List.of("9"), "b")); // clauses
        assertThat(base).isNotEqualTo(new NormDocSection("k", "t", List.of("1"), "X")); // body
    }

    @Test
    void section_nullsDefaultToEmpty() {
        NormDocSection s = new NormDocSection("k", "t", null, null);
        assertThat(s.getClauses()).isEmpty();
        assertThat(s.getBodyMarkdown()).isEmpty();
    }

    @Test
    void section_equalsAndHashCode() {
        NormDocSection a = new NormDocSection("k", "t", List.of("1"), "b");
        NormDocSection b = new NormDocSection("k", "t", List.of("1"), "b");
        NormDocSection c = new NormDocSection("k", "t", List.of("2"), "b");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("not-a-section");
        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void command_validatesAndDefaultsLanguage() {
        NormDocGenerationCommand cmd = new NormDocGenerationCommand(
                NormDocKind.POLICY, "iso-9001", "ISO 9001", "ACME", "it", "PME",
                null, null,
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", null, null)));
        assertThat(cmd.language()).isEqualTo("fr");
        assertThat(cmd.knownProcesses()).isEmpty();
        assertThat(cmd.sections()).hasSize(1);
        assertThat(cmd.kind()).isEqualTo(NormDocKind.POLICY);
        assertThat(cmd.standardName()).isEqualTo("ISO 9001");
    }

    @Test
    void command_keepsProvidedLanguage() {
        NormDocGenerationCommand cmd = new NormDocGenerationCommand(
                NormDocKind.MANUAL, "c", "n", "o", "i", "s", "en", List.of("p1"),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of("1"), "g")));
        assertThat(cmd.language()).isEqualTo("en");
        assertThat(cmd.knownProcesses()).containsExactly("p1");
    }

    @Test
    void command_rejectsBlankFields() {
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, " ", "n",
                "o", "i", "s", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("standardCode");
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", " ",
                "o", "i", "s", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("standardName");
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", "n",
                " ", "i", "s", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("organizationName");
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", "n",
                "o", " ", "s", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("industry");
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", "n",
                "o", "i", " ", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size");
    }

    @Test
    void command_blankLanguageDefaultsToFr() {
        NormDocGenerationCommand cmd = new NormDocGenerationCommand(
                NormDocKind.MANUAL, "c", "n", "o", "i", "s", "   ", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), "")));
        assertThat(cmd.language()).isEqualTo("fr");
    }

    @Test
    void command_rejectsNullFields() {
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, null, "n",
                "o", "i", "s", "fr", List.of(),
                List.of(new NormDocGenerationCommand.SectionRequest("k", "t", List.of(), ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("standardCode");
    }

    @Test
    void command_rejectsEmptyAndNullSections() {
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", "n",
                "o", "i", "s", "fr", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
        assertThatThrownBy(() -> new NormDocGenerationCommand(NormDocKind.MANUAL, "c", "n",
                "o", "i", "s", "fr", List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
    }

    @Test
    void sectionRequest_validatesKeyAndTitle_defaultsClausesGuidance() {
        NormDocGenerationCommand.SectionRequest s =
                new NormDocGenerationCommand.SectionRequest("k", "t", null, null);
        assertThat(s.clauses()).isEmpty();
        assertThat(s.guidance()).isEmpty();
        assertThatThrownBy(() -> new NormDocGenerationCommand.SectionRequest(" ", "t", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
        assertThatThrownBy(() -> new NormDocGenerationCommand.SectionRequest(null, "t", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
        assertThatThrownBy(() -> new NormDocGenerationCommand.SectionRequest("k", " ", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> new NormDocGenerationCommand.SectionRequest("k", null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        NormDocGenerationCommand.SectionRequest full =
                new NormDocGenerationCommand.SectionRequest("k", "t", List.of("1"), "g");
        assertThat(full.clauses()).containsExactly("1");
        assertThat(full.guidance()).isEqualTo("g");
    }

    @Test
    void generatedNormDoc_validates() {
        assertThatThrownBy(() -> new GeneratedNormDoc(" ",
                List.of(new NormDocSection("k", "t", List.of(), "b")), "ollama"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> new GeneratedNormDoc(null,
                List.of(new NormDocSection("k", "t", List.of(), "b")), "ollama"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> new GeneratedNormDoc("T", List.of(), "ollama"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
        assertThatThrownBy(() -> new GeneratedNormDoc("T", null, "ollama"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
        GeneratedNormDoc g = new GeneratedNormDoc("T",
                List.of(new NormDocSection("k", "t", List.of(), "b")), null);
        assertThat(g.provider()).isEmpty();
        GeneratedNormDoc g2 = new GeneratedNormDoc("T",
                List.of(new NormDocSection("k", "t", List.of(), "b")), "ollama");
        assertThat(g2.provider()).isEqualTo("ollama");
    }

    @Test
    void notFoundException_message() {
        java.util.UUID id = java.util.UUID.randomUUID();
        assertThat(new NormDocNotFoundException(id).getMessage()).contains(id.toString());
        assertThat(new NormDocStateException("boom").getMessage()).isEqualTo("boom");
    }
}
