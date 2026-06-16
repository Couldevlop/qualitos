package com.openlab.qualitos.iot.infrastructure.lorawan;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoRaWanPropertiesTest {

  @Test
  void defaults_areSafeAndConnectorDisabled() {
    LoRaWanProperties p = new LoRaWanProperties();
    assertThat(p.isEnabled()).isFalse(); // pas d'opt-in → aucune surface exposée
    assertThat(p.getDeviceIdField()).isEqualTo("deviceName");
    assertThat(p.getMaxMeasurements()).isEqualTo(50);
  }

  @Test
  void settersRoundTrip() {
    LoRaWanProperties p = new LoRaWanProperties();
    p.setEnabled(true);
    p.setDeviceIdField("devEUI");
    p.setMaxMeasurements(10);

    assertThat(p.isEnabled()).isTrue();
    assertThat(p.getDeviceIdField()).isEqualTo("devEUI");
    assertThat(p.getMaxMeasurements()).isEqualTo(10);
  }
}
