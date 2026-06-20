package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round-trip JSON entité ⇄ domaine (§8.4 onglet 7). Test dans le package du mapper. */
class MockAuditRunMapperRoundTripTest {

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    private static MockAuditRun sample() {
        List<MockAuditQuestion> questions = List.of(
                new MockAuditQuestion("8.1", "Comment maîtrisez-vous ?", "risque"));
        List<ClauseGapFinding> gaps = List.of(
                new ClauseGapFinding("8.1", "Maîtrise", MockAuditCriticality.MAJOR, 0d, 4, 0,
                        "Aucune preuve.", questions),
                new ClauseGapFinding("4.1", "Contexte", MockAuditCriticality.OBSERVATION, 1d, 2, 2,
                        "Couverte.", List.of()));
        List<RemediationAction> plan = List.of(
                new RemediationAction("8.1", MockAuditCriticality.MAJOR, "high", "AUDIT", "Lever NC."));
        return MockAuditRun.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "iso-9001", "ISO 9001:2015", 16.67d, questions, gaps, plan, "ollama",
                UUID.randomUUID(), Instant.parse("2026-06-20T09:00:00Z"));
    }

    @Test
    void roundTrip_preservesAllFields() {
        MockAuditRun original = sample();
        UUID id = UUID.randomUUID();
        original.assignId(id);

        MockAuditRunJpaEntity entity = MockAuditRunMapper.toEntity(original, json);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getStandardCode()).isEqualTo("iso-9001");
        assertThat(entity.getMajorCount()).isEqualTo(1);
        assertThat(entity.getQuestionsJson()).contains("8.1");
        assertThat(entity.getGapsJson()).contains("Aucune preuve.");
        assertThat(entity.getRemediationJson()).contains("AUDIT");

        MockAuditRun back = MockAuditRunMapper.toDomain(entity, json);

        assertThat(back.getId()).isEqualTo(id);
        assertThat(back.getStandardCode()).isEqualTo("iso-9001");
        assertThat(back.getStandardName()).isEqualTo("ISO 9001:2015");
        assertThat(back.getReadiness()).isEqualTo(16.67d);
        assertThat(back.getMajorCount()).isEqualTo(1);
        assertThat(back.getMinorCount()).isZero();
        assertThat(back.getObservationCount()).isEqualTo(1);
        assertThat(back.getQuestions()).singleElement()
                .satisfies(q -> assertThat(q.question()).isEqualTo("Comment maîtrisez-vous ?"));
        assertThat(back.getGaps()).hasSize(2);
        assertThat(back.getGaps().get(0).questions()).hasSize(1);
        assertThat(back.getRemediationPlan()).singleElement()
                .satisfies(a -> {
                    assertThat(a.targetModule()).isEqualTo("AUDIT");
                    assertThat(a.criticality()).isEqualTo(MockAuditCriticality.MAJOR);
                });
        assertThat(back.getAiProvider()).isEqualTo("ollama");
        assertThat(back.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    @Test
    void toDomain_corruptJson_throwsIllegalState() {
        MockAuditRunJpaEntity e = MockAuditRunMapper.toEntity(sample(), json);
        e.setQuestionsJson("{not valid json");
        assertThatThrownBy(() -> MockAuditRunMapper.toDomain(e, json))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deserialize");
    }

    @Test
    void toEntity_serializationFailure_throwsIllegalState() {
        // Un ObjectMapper qui échoue à l'écriture force la branche catch de write().
        ObjectMapper failing = new ObjectMapper() {
            @Override public String writeValueAsString(Object value) {
                throw new RuntimeException("boom");
            }
        };
        assertThatThrownBy(() -> MockAuditRunMapper.toEntity(sample(), failing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }
}
