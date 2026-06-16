package com.openlab.qualitos.iot.infrastructure.modbus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModbusPropertiesTest {

  @Test
  void defaults_areSafeAndConnectorDisabled() {
    ModbusProperties p = new ModbusProperties();
    assertThat(p.isEnabled()).isFalse(); // pas d'opt-in → aucune surface exposée
    assertThat(p.getMaxReadings()).isEqualTo(100);
  }

  @Test
  void settersRoundTrip() {
    ModbusProperties p = new ModbusProperties();
    p.setEnabled(true);
    p.setMaxReadings(10);

    assertThat(p.isEnabled()).isTrue();
    assertThat(p.getMaxReadings()).isEqualTo(10);
  }
}
