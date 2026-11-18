package com.openlab.qualitos.quality.standards;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce banc vivait dans {@code GlobalExceptionHandlerTest}. Il a suivi son gestionnaire
 * quand le socle a cessé de porter les refus des modules.
 */
class StandardsExceptionHandlerTest {

    private final StandardsExceptionHandler handler = new StandardsExceptionHandler();

    @Test
    void roadmapStageNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleRoadmapStageNotFound(
                new RoadmapStageNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).contains("Roadmap Stage");
    }
}
