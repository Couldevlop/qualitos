package com.openlab.qualitos.iot.infrastructure.modbus;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du connecteur d'ingestion Modbus TCP/RTU (CLAUDE.md §9.4 — équipements
 * legacy / PLC).
 *
 * <p>Bound depuis {@code qualitos.iot.modbus.*}. Le connecteur est désactivé par défaut
 * ({@link #enabled} = false) : ni le handler ni le contrôleur ne sont créés, donc aucune
 * surface n'est exposée dans un déploiement qui n'y consent pas explicitement (OWASP A05).
 */
@ConfigurationProperties(prefix = "qualitos.iot.modbus")
public class ModbusProperties {

  /** Interrupteur maître. À {@code false}, aucun bean Modbus n'est créé. */
  private boolean enabled = false;

  /**
   * Nombre maximal de mesures traitées par lecture (OWASP A04, borne dure anti-DoS).
   * Les mesures au-delà sont ignorées et signalées dans la réponse.
   */
  private int maxReadings = 100;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxReadings() {
    return maxReadings;
  }

  public void setMaxReadings(int maxReadings) {
    this.maxReadings = maxReadings;
  }
}
