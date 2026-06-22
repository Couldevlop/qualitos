package com.openlab.qualitos.quality.academy.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class XapiStatementBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final XapiStatementBuilder builder = new XapiStatementBuilder(mapper);
    private final UUID actor = UUID.randomUUID();

    @Test
    void buildsPassedAndCompletedStatements_whenPassed() throws Exception {
        String json = builder.completionStatements(actor, "iso-9001", "ISO 9001",
                88, true, Instant.parse("2026-06-22T10:00:00Z"));
        JsonNode arr = mapper.readTree(json);

        assertThat(arr.isArray()).isTrue();
        assertThat(arr).hasSize(2);
        assertThat(arr.get(0).path("verb").path("id").asText()).isEqualTo(XapiStatementBuilder.VERB_PASSED);
        assertThat(arr.get(1).path("verb").path("id").asText()).isEqualTo(XapiStatementBuilder.VERB_COMPLETED);

        JsonNode first = arr.get(0);
        assertThat(first.path("actor").path("account").path("name").asText()).isEqualTo(actor.toString());
        assertThat(first.path("object").path("id").asText()).endsWith("/course/iso-9001");
        assertThat(first.path("result").path("success").asBoolean()).isTrue();
        assertThat(first.path("result").path("score").path("raw").asInt()).isEqualTo(88);
        assertThat(first.path("result").path("score").path("scaled").asDouble()).isEqualTo(0.88);
    }

    @Test
    void buildsFailedStatement_whenNotPassed() throws Exception {
        String json = builder.completionStatements(actor, "c1", "Cours", 40, false, Instant.now());
        JsonNode arr = mapper.readTree(json);
        assertThat(arr.get(0).path("verb").path("id").asText()).isEqualTo(XapiStatementBuilder.VERB_FAILED);
        assertThat(arr.get(0).path("result").path("success").asBoolean()).isFalse();
    }

    @Test
    void scoreScaledIsClampedBetween0And1() throws Exception {
        String json = builder.completionStatements(actor, "c1", "Cours", 100, true, Instant.now());
        JsonNode arr = mapper.readTree(json);
        assertThat(arr.get(0).path("result").path("score").path("scaled").asDouble()).isEqualTo(1.0);
    }
}
