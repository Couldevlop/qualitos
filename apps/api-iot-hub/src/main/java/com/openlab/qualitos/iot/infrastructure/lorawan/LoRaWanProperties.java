package com.openlab.qualitos.iot.infrastructure.lorawan;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du connecteur d'ingestion LoRaWAN (CLAUDE.md §9.4 — TTN / ChirpStack).
 *
 * <p>Bound depuis {@code qualitos.iot.lorawan.*}. Le connecteur est désactivé par défaut
 * ({@link #enabled} = false) : ni le handler ni le contrôleur ne sont créés, donc aucune
 * surface n'est exposée dans un déploiement qui n'y consent pas explicitement (OWASP A05).
 */
@ConfigurationProperties(prefix = "qualitos.iot.lorawan")
public class LoRaWanProperties {

  /** Interrupteur maître. À {@code false}, aucun bean LoRaWAN n'est créé. */
  private boolean enabled = false;

  /**
   * Nom du champ JSON portant le code device dans l'uplink. TTN expose typiquement
   * {@code deviceName} (alias {@code end_device_ids.device_id} mis à plat par le formatter),
   * ChirpStack {@code deviceName} ou {@code devEUI}. Configurable car les NS diffèrent.
   */
  private String deviceIdField = "deviceName";

  /**
   * Nombre maximal de mesures traitées par uplink (OWASP A04, borne dure anti-DoS).
   * Les mesures au-delà sont ignorées et signalées dans la réponse.
   */
  private int maxMeasurements = 50;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getDeviceIdField() {
    return deviceIdField;
  }

  public void setDeviceIdField(String deviceIdField) {
    this.deviceIdField = deviceIdField;
  }

  public int getMaxMeasurements() {
    return maxMeasurements;
  }

  public void setMaxMeasurements(int maxMeasurements) {
    this.maxMeasurements = maxMeasurements;
  }
}
