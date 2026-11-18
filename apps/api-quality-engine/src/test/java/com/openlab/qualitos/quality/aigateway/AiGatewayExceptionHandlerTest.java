package com.openlab.qualitos.quality.aigateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce banc vivait dans {@code GlobalExceptionHandlerTest}. Il a suivi son gestionnaire
 * quand le socle a cessé de porter les refus des modules.
 */
class AiGatewayExceptionHandlerTest {

    private final AiGatewayExceptionHandler handler = new AiGatewayExceptionHandler();

    /**
     * Une question qu'on ne sait pas traduire en requête sûre n'est pas une panne de
     * passerelle : c'est le CONTENU de la demande qui est refusé. 422, donc, et non
     * 502 — sans quoi l'utilisateur croit le service en panne et réessaie à l'identique.
     */
    @Test
    void nlqUntranslatable_mapsTo422_notBadGateway() {
        ProblemDetail pd = handler.handleNlqUntranslatable(
                new NlqUntranslatableException("Question non traduisible en requête sûre", null));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(pd.getType().toString()).endsWith("/nlq-untranslatable");
    }
}
