package com.openlab.qualitos.quality.dmaic.resolution.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le paramétrage par défaut est celui du classeur, et le réglage d'un
 * administrateur est nettoyé puis vérifié avant d'être accepté.
 */
class ResolutionSettingsTest {

    static SheetQuestion question(String code) {
        return new SheetQuestion(code, SheetSection.MEASURE, null, "Une question ?", AnswerType.TEXT);
    }

    static List<SeverityLevel> uneSeverite() {
        return List.of(new SeverityLevel(1, "Grave"));
    }

    @Test
    void lesDefautsReprennentLeClasseurPhaseParPhase() {
        ResolutionSettings d = ResolutionSettings.platformDefaults();

        assertThat(d.defaults()).isTrue();
        assertThat(d.questions().stream().collect(Collectors.groupingBy(SheetQuestion::section, Collectors.counting())))
                .containsEntry(SheetSection.DEFINE, 14L)
                .containsEntry(SheetSection.MEASURE, 3L)
                .containsEntry(SheetSection.CONTAIN, 6L)
                .containsEntry(SheetSection.ANALYZE, 3L)
                .containsEntry(SheetSection.IMPROVE, 10L)
                .containsEntry(SheetSection.CONTROL, 12L);
        assertThat(d.question("DEF-12")).get().extracting(SheetQuestion::answerType)
                .isEqualTo(AnswerType.YES_NO_UNKNOWN);
        assertThat(d.question("CON-01")).get().extracting(SheetQuestion::answerType).isEqualTo(AnswerType.YES_NO_NA);
        assertThat(d.question("CTL-12")).get().extracting(SheetQuestion::answerType).isEqualTo(AnswerType.YES_NO);
        assertThat(d.failureTypes()).hasSize(56);
        assertThat(d.failureTypes().get(0).code()).isEqualTo("V01");
        assertThat(d.failureTypes().get(d.failureTypes().size() - 1).label()).isEqualTo("Late delivery / OTD");
        assertThat(d.severities()).extracting(SeverityLevel::level).containsExactly(1, 2, 3);
    }

    @Test
    void lesDefautsPassentLaMemeVerificationQuUnReglage() {
        ResolutionSettings d = ResolutionSettings.platformDefaults();

        ResolutionSettings r = ResolutionSettings.configured(d.questions(), d.failureTypes(), d.severities());

        assertThat(r.questions()).isEqualTo(d.questions());
        assertThat(r.failureTypes()).isEqualTo(d.failureTypes());
        assertThat(r.defaults()).isFalse();
        Set<String> codes = new HashSet<>();
        d.questions().forEach(q -> assertThat(codes.add(q.code())).isTrue());
        assertThat(d.questions()).filteredOn(q -> q.section() == SheetSection.DEFINE)
                .allSatisfy(q -> assertThat(q.topic()).isNotNull());
    }

    @Test
    void leReglageEstNettoye() {
        ResolutionSettings r = ResolutionSettings.configured(
                List.of(new SheetQuestion("  q-1 ", SheetSection.DEFINE, DefineTopic.WHO, "  Qui ?  ", AnswerType.TEXT)),
                List.of(new FailureType(" v9 ", "  Rayure ")),
                List.of(new SeverityLevel(2, " Moyenne ")));

        assertThat(r.questions().get(0)).isEqualTo(
                new SheetQuestion("Q-1", SheetSection.DEFINE, DefineTopic.WHO, "Qui ?", AnswerType.TEXT));
        assertThat(r.failureTypes().get(0)).isEqualTo(new FailureType("V9", "Rayure"));
        assertThat(r.severities().get(0).label()).isEqualTo("Moyenne");
        assertThat(r.knowsFailureType("V9")).isTrue();
        assertThat(r.knowsFailureType("V10")).isFalse();
        assertThat(r.knowsSeverity(2)).isTrue();
        assertThat(r.knowsSeverity(1)).isFalse();
        assertThat(r.question("Q-1")).isPresent();
        assertThat(r.question("Q-2")).isEmpty();
    }

    @Test
    void unCatalogueDeDefaillancesVideEstAdmis() {
        assertThat(ResolutionSettings.configured(List.of(question("A")), null, uneSeverite()).failureTypes()).isEmpty();
    }

    @Test
    void lesQuestionsSontVerifiees() {
        assertRefus(() -> ResolutionSettings.configured(null, List.of(), uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(), List.of(), uneSeverite()), "questions");
        List<SheetQuestion> trop = new ArrayList<>();
        for (int i = 0; i <= ResolutionSettings.MAX_QUESTIONS; i++) {
            trop.add(question("Q" + i));
        }
        assertRefus(() -> ResolutionSettings.configured(trop, List.of(), uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(question("A"), question("a")), List.of(),
                uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(question("é")), List.of(), uneSeverite()),
                "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(question(null)), List.of(), uneSeverite()),
                "questions");
        assertRefus(() -> ResolutionSettings.configured(Collections.singletonList(null), List.of(), uneSeverite()),
                "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(new SheetQuestion("A", null, null, "x",
                AnswerType.TEXT)), List.of(), uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(new SheetQuestion("A", SheetSection.CONTROL,
                DefineTopic.WHO, "x", AnswerType.TEXT)), List.of(), uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(new SheetQuestion("A", SheetSection.CONTROL,
                null, " ", AnswerType.TEXT)), List.of(), uneSeverite()), "questions");
        assertRefus(() -> ResolutionSettings.configured(List.of(new SheetQuestion("A", SheetSection.CONTROL,
                null, "x".repeat(501), AnswerType.TEXT)), List.of(), uneSeverite()), "questions");
    }

    @Test
    void lesTypesDeDefaillanceSontVerifies() {
        List<SheetQuestion> q = List.of(question("A"));
        List<FailureType> trop = new ArrayList<>();
        for (int i = 0; i <= ResolutionSettings.MAX_FAILURE_TYPES; i++) {
            trop.add(new FailureType("V" + i, "x"));
        }
        assertRefus(() -> ResolutionSettings.configured(q, trop, uneSeverite()), "failureTypes");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(new FailureType("V1", "a"),
                new FailureType("v1", "b")), uneSeverite()), "failureTypes");
        assertRefus(() -> ResolutionSettings.configured(q, Collections.singletonList(null), uneSeverite()),
                "failureTypes");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(new FailureType("V1", "")), uneSeverite()),
                "failureTypes");
    }

    @Test
    void lesSeveritesSontVerifiees() {
        List<SheetQuestion> q = List.of(question("A"));
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), null), "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), List.of()), "severities");
        List<SeverityLevel> trop = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            trop.add(new SeverityLevel(i, "n" + i));
        }
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), trop), "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), List.of(new SeverityLevel(0, "x"))),
                "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), List.of(new SeverityLevel(11, "x"))),
                "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), Collections.singletonList(null)),
                "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), List.of(new SeverityLevel(1, "a"),
                new SeverityLevel(1, "b"))), "severities");
        assertRefus(() -> ResolutionSettings.configured(q, List.of(), List.of(new SeverityLevel(1, "x".repeat(81)))),
                "severities");
    }

    static void assertRefus(Runnable action, String field) {
        assertThatThrownBy(action::run).isInstanceOf(ResolutionValidationException.class)
                .satisfies(e -> assertThat(((ResolutionValidationException) e).getField()).isEqualTo(field));
    }
}
