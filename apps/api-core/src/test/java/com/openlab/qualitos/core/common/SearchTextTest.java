package com.openlab.qualitos.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchText")
class SearchTextTest {

    @Test
    @DisplayName("retire accents et majuscules, resserre les espaces")
    void normalizes() {
        assertThat(SearchText.normalize("  Élodie   MÜLLER-Çà ")).isEqualTo("elodie muller-ca");
        assertThat(SearchText.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("neutralise les jokers LIKE et le caractère d'échappement")
    void escapesWildcards() {
        assertThat(SearchText.containsPattern("")).isEqualTo("%");
        assertThat(SearchText.containsPattern("a%b_c!d")).isEqualTo("%a!%b!_c!!d%");
    }
}
