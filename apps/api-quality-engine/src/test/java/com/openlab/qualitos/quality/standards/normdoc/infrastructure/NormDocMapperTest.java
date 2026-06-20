package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormDocMapperTest {

    private final ObjectMapper json = new ObjectMapper();

    private NormativeDocument doc() {
        return NormativeDocument.draftFromAi(UUID.randomUUID(), UUID.randomUUID(), "iso-9001",
                NormDocKind.MANUAL, "Manuel Qualité",
                List.of(new NormDocSection("ctx", "Contexte", List.of("4.1"), "Corps"),
                        new NormDocSection("lead", "Leadership", List.of(), "Corps 2")),
                "ollama", UUID.randomUUID(), Instant.parse("2026-06-20T08:00:00Z"));
    }

    @Test
    void roundTrip_preservesFields() {
        NormativeDocument src = doc();
        src.assignId(UUID.randomUUID());

        NormDocJpaEntity e = NormDocMapper.toEntity(src, null, json);
        assertThat(e.getSectionsJson()).contains("ctx").contains("4.1");

        NormativeDocument back = NormDocMapper.toDomain(e, json);
        assertThat(back.getStandardCode()).isEqualTo("iso-9001");
        assertThat(back.getKind()).isEqualTo(NormDocKind.MANUAL);
        assertThat(back.getSections()).hasSize(2);
        assertThat(back.getSections().get(0).getClauses()).containsExactly("4.1");
        assertThat(back.getStatus()).isEqualTo(NormDocStatus.BROUILLON_IA);
    }

    @Test
    void toEntity_reusesExistingTarget() {
        NormativeDocument src = doc();
        src.assignId(UUID.randomUUID());
        NormDocJpaEntity target = new NormDocJpaEntity();
        target.setId(src.getId());
        NormDocJpaEntity out = NormDocMapper.toEntity(src, target, json);
        assertThat(out).isSameAs(target);
        assertThat(out.getTitle()).isEqualTo("Manuel Qualité");
    }

    @Test
    void toEntity_nullIdNotSet() {
        NormativeDocument src = doc(); // id null
        NormDocJpaEntity e = NormDocMapper.toEntity(src, null, json);
        assertThat(e.getId()).isNull();
    }

    @Test
    void writeSections_serializationFailure_throws() throws Exception {
        ObjectMapper broken = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(broken.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        NormativeDocument src = doc();
        assertThatThrownBy(() -> NormDocMapper.toEntity(src, null, broken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }

    @Test
    void readSections_invalidJson_throws() {
        NormativeDocument src = doc();
        NormDocJpaEntity e = NormDocMapper.toEntity(src, null, json);
        e.setSectionsJson("{not-json");
        assertThatThrownBy(() -> NormDocMapper.toDomain(e, json))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deserialize");
    }
}
