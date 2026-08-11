package com.openlab.qualitos.quality.nonconformity.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les réglages d'un dispositif qui EFFACE se vérifient au démarrage, pas au
 * premier passage : une valeur dangereuse découverte en production l'est une
 * fois les octets perdus.
 */
class OrphanSweepPropertiesTest {

    @Test
    void isOffByDefault() {
        assertThat(new OrphanSweepProperties().isEnabled()).isFalse();
    }

    @Test
    void defaultGraceIsADay() {
        assertThat(new OrphanSweepProperties().getGracePeriod()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void tooShortGracePeriod_isRefusedAtStartup() {
        // Quelques minutes suffiraient à effacer une pièce entre son dépôt et la
        // validation de sa transaction.
        OrphanSweepProperties props = new OrphanSweepProperties();
        assertThatThrownBy(() -> props.setGracePeriod(Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grace-period");
    }

    @Test
    void nullGracePeriod_isRefusedRatherThanFallingBack() {
        OrphanSweepProperties props = new OrphanSweepProperties();
        assertThatThrownBy(() -> props.setGracePeriod(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oneHourGracePeriod_isTheAcceptedFloor() {
        OrphanSweepProperties props = new OrphanSweepProperties();
        props.setGracePeriod(Duration.ofHours(1));
        assertThat(props.getGracePeriod()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void nonPositiveBatchSize_isRefused() {
        OrphanSweepProperties props = new OrphanSweepProperties();
        assertThatThrownBy(() -> props.setBatchSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batch-size");
    }

    @Test
    void batchSize_isCapped() {
        OrphanSweepProperties props = new OrphanSweepProperties();
        props.setBatchSize(99_999);
        assertThat(props.getBatchSize()).isEqualTo(5000);
    }
}
