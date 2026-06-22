package com.openlab.qualitos.quality.academy.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonConverterTest {

    private final StringListJsonConverter strings = new StringListJsonConverter();
    private final IntListJsonConverter ints = new IntListJsonConverter();

    @Test
    void stringList_roundTrips_withCommasInValues() {
        List<String> in = List.of("Oui, vraiment", "Non", "Peut-être");
        String db = strings.convertToDatabaseColumn(in);
        assertThat(strings.convertToEntityAttribute(db)).isEqualTo(in);
    }

    @Test
    void stringList_nullAndBlank_giveEmptyList() {
        assertThat(strings.convertToEntityAttribute(null)).isEmpty();
        assertThat(strings.convertToEntityAttribute("  ")).isEmpty();
        assertThat(strings.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    void stringList_corruptJson_throws() {
        assertThatThrownBy(() -> strings.convertToEntityAttribute("{not-json"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void intList_roundTrips() {
        List<Integer> in = List.of(0, 2, 1, 3);
        String db = ints.convertToDatabaseColumn(in);
        assertThat(ints.convertToEntityAttribute(db)).isEqualTo(in);
    }

    @Test
    void intList_nullGivesEmpty() {
        assertThat(ints.convertToEntityAttribute(null)).isEmpty();
        assertThat(ints.convertToDatabaseColumn(null)).isEqualTo("[]");
    }
}
