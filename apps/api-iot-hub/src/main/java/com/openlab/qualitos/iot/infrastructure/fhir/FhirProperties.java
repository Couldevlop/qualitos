package com.openlab.qualitos.iot.infrastructure.fhir;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du connecteur d'ingestion HL7 FHIR R5 (CLAUDE.md §9.4 — Santé).
 *
 * <p>Contrairement aux connecteurs sortants (MQTT, OPC-UA) qui dialent vers un
 * broker/serveur, l'ingestion FHIR est un endpoint REST entrant : aucun système
 * externe n'est requis. Le connecteur est donc actif par défaut, désactivable
 * par {@code qualitos.iot.fhir.enabled=false}.
 */
@ConfigurationProperties(prefix = "qualitos.iot.fhir")
public class FhirProperties {

  /** Active l'endpoint d'ingestion FHIR ({@code POST /api/v1/iot/fhir}). */
  private boolean enabled = true;

  /**
   * Nombre maximal d'entrées traitées dans un Bundle (OWASP A04 / LLM-DoS-like :
   * borne dure contre les payloads pathologiques). Les entrées au-delà sont ignorées
   * et signalées dans la réponse.
   */
  private int maxBundleEntries = 200;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxBundleEntries() {
    return maxBundleEntries;
  }

  public void setMaxBundleEntries(int maxBundleEntries) {
    this.maxBundleEntries = maxBundleEntries;
  }
}
