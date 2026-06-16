package com.openlab.qualitos.iot.infrastructure.sparkplug;

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
 * Handler pur d'ingestion <b>Sparkplug B</b> (CLAUDE.md §9.4) : décode un payload
 * NDATA/DDATA (JSON) puis alimente {@link IngestTelemetryUseCase} pour chaque métrique.
 *
 * <p>Aucun type HTTP/Spring/MQTT ici : le contrôleur (et, à terme, l'adaptateur MQTT
 * Tahu) ne fait que déléguer — toute la logique reste testable en isolation (mêmes
 * conventions que {@code MqttTelemetryMessageHandler} / {@code FhirIngestionHandler}).
 *
 * <h2>Résolution du tenant (§18.2 règle 2 — CRITIQUE)</h2>
 * Sparkplug B circule sur MQTT : comme le connecteur MQTT, le message identifie un
 * <em>équipement</em> par code, jamais un tenant. Le tenant est résolu via
 * {@link DeviceRepository#findUniqueByCode(String)} et provient de
 * {@link Device#tenantId()} — JAMAIS du payload. Une collision cross-tenant (code
 * ambigu) renvoie {@code empty} → <b>fail-closed</b> (anti-IDOR A01), aucune mesure
 * n'est attribuée au mauvais tenant.
 *
 * <h2>Robustesse</h2>
 * Ne lève jamais d'exception : un device inconnu/ambigu, un payload pourri ou une panne
 * aval sont comptés et motivés dans l'{@link Outcome} (sans donnée sensible) ; une
 * métrique en échec n'interrompt pas le reste du lot.
 */
public final class SparkplugIngestionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SparkplugIngestionHandler.class);

  private final DeviceRepository deviceRepository;
  private final IngestTelemetryUseCase ingestUseCase;
  private final SparkplugBDecoder decoder;
  private final int maxMetrics;

  public SparkplugIngestionHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      SparkplugBDecoder decoder,
      int maxMetrics) {
    this.deviceRepository = Objects.requireNonNull(deviceRepository, "deviceRepository");
    this.ingestUseCase = Objects.requireNonNull(ingestUseCase, "ingestUseCase");
    this.decoder = Objects.requireNonNull(decoder, "decoder");
    this.maxMetrics = maxMetrics;
  }

  /** Bilan d'ingestion — renvoyé tel quel à la passerelle/au système intégrateur. */
  public record Outcome(int ingested, int dropped, List<String> issues) {

    public Outcome {
      issues = issues == null ? List.of() : List.copyOf(issues);
    }
  }

  /**
   * Traite un payload Sparkplug B (JSON). Ne lève jamais d'exception.
   *
   * <p>Le tenant est résolu depuis le registre device, jamais lu du payload.
   */
  public Outcome handle(JsonNode root) {
    SparkplugBDecoder.DecodeResult result = decoder.decode(root);
    if (result.isDropped()) {
      return new Outcome(0, 1, List.of(result.dropReason()));
    }

    // ---- RÉSOLUTION DU TENANT (jamais depuis le payload) -----------------------
    Optional<Device> resolved = deviceRepository.findUniqueByCode(result.deviceCode());
    if (resolved.isEmpty()) {
      // Inconnu OU collision cross-tenant : fail-closed (anti-IDOR A01).
      return new Outcome(0, 1, List.of(
          "unknown/ambiguous device code '" + sanitize(result.deviceCode()) + "'"));
    }
    Device device = resolved.get();

    List<SparkplugBDecoder.DecodedMetric> metrics = result.metrics();
    int ingested = 0;
    int dropped = 0;
    List<String> issues = new ArrayList<>();

    int limit = Math.min(metrics.size(), maxMetrics);
    if (metrics.size() > maxMetrics) {
      issues.add("payload truncated: " + (metrics.size() - maxMetrics)
          + " metric(s) beyond the " + maxMetrics + "-metric limit were ignored");
    }

    for (int i = 0; i < limit; i++) {
      SparkplugBDecoder.DecodedMetric m = metrics.get(i);
      try {
        ingestUseCase.ingest(
            device.tenantId(), device.id(), m.name(), m.value(), m.unit(), m.recordedAt());
        ingested++;
      } catch (RuntimeException ex) {
        // Une métrique en échec ne doit jamais interrompre le reste du lot.
        dropped++;
        issues.add("metric[" + i + "]: ingestion failed");
        LOG.warn("Sparkplug metric[{}] dropped — unexpected error: {}", i, ex.getMessage());
      }
    }

    LOG.debug("Sparkplug ingestion done — device='{}' tenant='{}' ingested={} dropped={}",
        result.deviceCode(), device.tenantId(), ingested, dropped);
    return new Outcome(ingested, dropped, issues);
  }

  /** Tronque + neutralise les valeurs réseau avant de les citer (A09 log injection). */
  private static String sanitize(String raw) {
    if (raw == null) {
      return "";
    }
    String cleaned = raw.replaceAll("[\\r\\n\\t]", " ");
    return cleaned.length() > 64 ? cleaned.substring(0, 64) + "…" : cleaned;
  }
}
