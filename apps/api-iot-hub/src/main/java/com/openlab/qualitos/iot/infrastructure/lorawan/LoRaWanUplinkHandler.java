package com.openlab.qualitos.iot.infrastructure.lorawan;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handler pur d'ingestion d'uplinks LoRaWAN (TTN / ChirpStack) — décode l'uplink puis
 * alimente {@link IngestTelemetryUseCase} (CLAUDE.md §9.4 — LoRaWAN).
 *
 * <p>Aucun type HTTP/Spring ici : le contrôleur ne fait que déléguer, ce qui rend toute
 * la logique testable en isolation (mêmes conventions que {@code FhirIngestionHandler}
 * et {@code MqttTelemetryMessageHandler}).
 *
 * <h2>Résolution TENANT (§18.2 #2 — CRITIQUE)</h2>
 * Un uplink LoRaWAN provient d'un Network Server en machine-to-machine, <b>sans</b> notre
 * JWT par device : on calque donc le chemin MQTT (et non FHIR). Le device est résolu par
 * son code via {@link DeviceRepository#findUniqueByCode(String)} (lookup tenant-agnostique)
 * et le tenant qui fait foi est {@link Device#tenantId()}. Toute collision cross-tenant
 * du code → {@link Optional#empty()} → rejet <b>fail-closed</b> (on ne devine jamais le
 * tenant, anti-IDOR A01). Aucun tenant n'est jamais lu de la charge utile.
 *
 * <h2>Robustesse</h2>
 * Ne lève jamais d'exception : uplink mal formé, device inconnu/ambigu, mesure pourrie ou
 * panne aval sont comptés et motivés dans l'{@link Outcome} (sans PII) ; une mesure en
 * échec n'interrompt jamais le reste de l'uplink.
 */
public final class LoRaWanUplinkHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LoRaWanUplinkHandler.class);

  private final DeviceRepository deviceRepository;
  private final IngestTelemetryUseCase ingestUseCase;
  private final LoRaWanUplinkDecoder decoder;
  private final String deviceIdField;
  private final int maxMeasurements;

  public LoRaWanUplinkHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      LoRaWanUplinkDecoder decoder,
      String deviceIdField,
      int maxMeasurements) {
    this.deviceRepository = Objects.requireNonNull(deviceRepository, "deviceRepository");
    this.ingestUseCase = Objects.requireNonNull(ingestUseCase, "ingestUseCase");
    this.decoder = Objects.requireNonNull(decoder, "decoder");
    this.deviceIdField = Objects.requireNonNull(deviceIdField, "deviceIdField");
    this.maxMeasurements = maxMeasurements;
  }

  /** Bilan d'ingestion — renvoyé tel quel au Network Server / intégrateur. */
  public record Outcome(int ingested, int dropped, List<String> issues) {

    public Outcome {
      issues = issues == null ? List.of() : List.copyOf(issues);
    }
  }

  /**
   * Décode et ingère un uplink LoRaWAN. Ne lève jamais d'exception.
   *
   * @param root corps JSON de l'uplink (tenant jamais lu d'ici)
   */
  public Outcome handle(JsonNode root) {
    if (root == null || !root.isObject()) {
      return new Outcome(0, 1, List.of("payload is not a JSON object"));
    }

    LoRaWanUplinkDecoder.DecodeResult decoded = decoder.decode(root, deviceIdField);

    if (!decoded.hasDeviceCode()) {
      return new Outcome(0, 1, List.of("no device code in field '" + sanitize(deviceIdField) + "'"));
    }

    List<String> issues = new ArrayList<>();
    if (decoded.droppedFields() > 0) {
      issues.add(decoded.droppedFields() + " non-numeric field(s) ignored");
    }

    List<LoRaWanUplinkDecoder.DecodedMeasurement> measurements = decoded.measurements();
    if (measurements.isEmpty()) {
      issues.add("no numeric measurement in 'decoded'");
      return new Outcome(0, 1, issues);
    }

    // ---- RÉSOLUTION TENANT (jamais depuis la charge utile) ---------------------
    Optional<Device> resolved = deviceRepository.findUniqueByCode(decoded.deviceCode());
    if (resolved.isEmpty()) {
      // Inconnu OU ambigu (collision cross-tenant) : fail-closed, rien n'est ingéré.
      issues.add("unknown/ambiguous device code '" + sanitize(decoded.deviceCode()) + "'");
      return new Outcome(0, measurements.size(), issues);
    }
    Device device = resolved.get();

    int ingested = 0;
    int dropped = 0;
    // Borne dure contre les uplinks pathologiques (OWASP A04, anti-DoS).
    int limit = Math.min(measurements.size(), maxMeasurements);
    if (measurements.size() > maxMeasurements) {
      dropped += measurements.size() - maxMeasurements;
      issues.add("uplink truncated: " + (measurements.size() - maxMeasurements)
          + " measurement(s) beyond the " + maxMeasurements + "-point limit were ignored");
    }

    for (int i = 0; i < limit; i++) {
      LoRaWanUplinkDecoder.DecodedMeasurement m = measurements.get(i);
      try {
        ingestUseCase.ingest(
            device.tenantId(), device.id(), m.metric(), m.value(), m.unit(), m.recordedAt());
        ingested++;
      } catch (RuntimeException ex) {
        // Une mesure en échec ne doit jamais interrompre le reste de l'uplink.
        dropped++;
        issues.add("measurement[" + i + "]: ingestion failed");
        LOG.warn("LoRaWAN measurement[{}] dropped — unexpected error: {}", i, ex.getMessage());
      }
    }

    LOG.debug("LoRaWAN ingestion done — tenant={} device={} ingested={} dropped={}",
        device.tenantId(), device.id(), ingested, dropped);
    return new Outcome(ingested, dropped, issues);
  }

  /** Tronque + neutralise les valeurs venues du réseau avant de les citer (A09 log injection). */
  private static String sanitize(String raw) {
    if (raw == null) {
      return "";
    }
    String cleaned = raw.replaceAll("[\\r\\n\\t]", " ");
    return cleaned.length() > 64 ? cleaned.substring(0, 64) + "…" : cleaned;
  }
}
