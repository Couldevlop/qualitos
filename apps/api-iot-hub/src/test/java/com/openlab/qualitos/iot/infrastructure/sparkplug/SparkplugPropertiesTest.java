package com.openlab.qualitos.iot.infrastructure.sparkplug;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Garantit les défauts sûrs (désactivé) et la mutabilité des propriétés Sparkplug. */
class SparkplugPropertiesTest {

  @Test
  void defaultsAreSafe() {
    SparkplugProperties p = new SparkplugProperties();
    assertThat(p.isEnabled()).isFalse();   // OWASP A05 : surface nulle par défaut
    assertThat(p.getMaxMetrics()).isEqualTo(500);
  }

  @Test
  void settersBind() {
    SparkplugProperties p = new SparkplugProperties();
    p.setEnabled(true);
    p.setMaxMetrics(42);
    assertThat(p.isEnabled()).isTrue();
    assertThat(p.getMaxMetrics()).isEqualTo(42);
  }
}
