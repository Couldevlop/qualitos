package com.openlab.qualitos.quality.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La règle tient en une ligne, mais c'est elle qui décide si une empreinte est
 * recalculable après un aller-retour en base. Elle est donc dite ici.
 */
class StorableInstantTest {

    @Test
    void aNanosecondInstantLosesWhatFollowsTheMicrosecond() {
        assertThat(StorableInstant.micros(Instant.parse("2026-08-27T18:18:04.123456789Z")))
                .isEqualTo(Instant.parse("2026-08-27T18:18:04.123456Z"));
    }

    @Test
    void itTruncatesInsteadOfRounding() {
        // .999999999 ne devient PAS la seconde suivante : arrondir ferait dépendre
        // l'écriture d'une règle qui appartient au moteur de base de données.
        assertThat(StorableInstant.micros(Instant.parse("2026-08-27T18:18:04.999999999Z")))
                .isEqualTo(Instant.parse("2026-08-27T18:18:04.999999Z"));
    }

    @Test
    void anInstantAlreadyAtTheMicrosecondComesBackUntouched() {
        Instant already = Instant.parse("2026-08-27T18:18:04.123456Z");
        assertThat(StorableInstant.micros(already)).isEqualTo(already);
    }

    @Test
    void aSecondPreciseInstantComesBackUntouched() {
        Instant already = Instant.parse("2026-08-27T18:18:04Z");
        assertThat(StorableInstant.micros(already)).isEqualTo(already);
    }

    @Test
    void nullStaysNull() {
        // Un horodatage absent ne doit pas devenir une exception : le hacheur sait
        // déjà traiter le vide, et c'est à lui de décider.
        assertThat(StorableInstant.micros(null)).isNull();
    }

    @Test
    void theClassIsNotMeantToBeInstantiated() throws Exception {
        Constructor<StorableInstant> constructor = StorableInstant.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
