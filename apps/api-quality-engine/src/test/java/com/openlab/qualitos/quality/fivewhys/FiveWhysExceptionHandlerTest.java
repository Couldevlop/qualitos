package com.openlab.qualitos.quality.fivewhys;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ces deux bancs vivaient dans {@code GlobalExceptionHandlerTest}. Ils ont suivi
 * leurs gestionnaires quand le socle a cessé de porter les refus des modules
 * (ADR 0128) : un banc qui mesure le refus des 5 pourquoi appartient aux 5 pourquoi.
 */
class FiveWhysExceptionHandlerTest {

    private final FiveWhysExceptionHandler handler = new FiveWhysExceptionHandler();

    @Test
    void fiveWhysNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleFiveWhysNotFound(
                new FiveWhysNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Five Whys Analysis Not Found");
    }

    @Test
    void fiveWhysRuleBroken_mapsTo409_andSaysWhy() {
        // Une règle de méthode refusée n'est pas une panne : le client doit lire un
        // conflit d'état, et la raison, pour la corriger sans deviner.
        ProblemDetail pd = handler.handleFiveWhysState(
                new FiveWhysStateException("Conclure avant 3 pourquoi, c'est nommer un symptôme"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Invalid Five Whys State");
        assertThat(pd.getDetail()).contains("symptôme");
    }
}
