package com.openlab.qualitos.iot.infrastructure.sparkplug;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du connecteur d'ingestion <b>Sparkplug B</b> (CLAUDE.md §9.4).
 *
 * <p>Bound depuis {@code qualitos.iot.sparkplug.*}. Désactivé par défaut
 * ({@link #enabled} = false) à l'image du connecteur MQTT : aucune surface n'est
 * exposée en CI ni en déploiement REST-only tant que le flag n'est pas activé
 * explicitement (OWASP A05). Sparkplug B circule sur MQTT ; l'endpoint REST de ce
 * module sert d'entrée pour une passerelle Edge ayant déjà décodé le protobuf en JSON.
 */
@ConfigurationProperties(prefix = "qualitos.iot.sparkplug")
public class SparkplugProperties {

  /** Interrupteur maître. Quand false, ni handler ni contrôleur ne sont créés. */
  private boolean enabled = false;

  /**
   * Nombre maximal de métriques traitées dans un payload (OWASP A04 — borne dure
   * contre les payloads pathologiques). Les métriques au-delà sont ignorées et
   * signalées dans la réponse.
   */
  private int maxMetrics = 500;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxMetrics() {
    return maxMetrics;
  }

  public void setMaxMetrics(int maxMetrics) {
    this.maxMetrics = maxMetrics;
  }
}
