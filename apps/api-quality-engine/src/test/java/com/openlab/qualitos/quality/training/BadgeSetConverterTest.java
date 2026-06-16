package com.openlab.qualitos.quality.training;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Sérialisation CSV des badges (round-trip + tolérance aux valeurs inconnues). */
class BadgeSetConverterTest {

    private final BadgeSetConverter conv = new BadgeSetConverter();

    @Test
    void empty_roundTrips() {
        assertThat(conv.convertToDatabaseColumn(EnumSet.noneOf(Badge.class))).isEmpty();
        assertThat(conv.convertToDatabaseColumn(null)).isEmpty();
        assertThat(conv.convertToEntityAttribute("")).isEmpty();
        assertThat(conv.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void set_roundTrips_inEnumOrder() {
        Set<Badge> in = EnumSet.of(Badge.YELLOW_BELT, Badge.FIRST_STEPS);
        String csv = conv.convertToDatabaseColumn(in);
        // tri par ordre d'énum : FIRST_STEPS avant YELLOW_BELT
        assertThat(csv).isEqualTo("FIRST_STEPS,YELLOW_BELT");
        assertThat(conv.convertToEntityAttribute(csv)).isEqualTo(in);
    }

    @Test
    void unknownToken_isIgnored() {
        Set<Badge> out = conv.convertToEntityAttribute("FIRST_STEPS, GHOST_BADGE ,BLACK_BELT");
        assertThat(out).containsExactlyInAnyOrder(Badge.FIRST_STEPS, Badge.BLACK_BELT);
    }
}
