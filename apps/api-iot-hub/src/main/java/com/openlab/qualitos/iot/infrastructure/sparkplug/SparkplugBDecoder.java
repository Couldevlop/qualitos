package com.openlab.qualitos.iot.infrastructure.sparkplug;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Décodeur pur d'un payload <b>Sparkplug B</b> (CLAUDE.md §9.4 — modèle de données
 * industriel unifié sur MQTT) reçu sous forme <b>JSON</b> (forme NDATA / DDATA déjà
 * décodée du protobuf). Aucune dépendance Spring / HTTP / MQTT — testable en isolation
 * (mêmes conventions hexagonales que {@code FhirObservationMapper} /
 * {@code MqttTelemetryMessageHandler}).
 *
 * <h2>Portée — décodage protobuf délégué (lot ultérieur)</h2>
 * Sparkplug B « vrai » sérialise ses payloads en <b>protobuf</b> (schéma Eclipse Tahu).
 * Pour rester SANS dépendance lourde et 100 % testable, ce décodeur accepte la forme
 * <em>déjà décodée en JSON</em> que produit une passerelle Edge (Tahu / EMQX Sparkplug
 * codec). Le décodage protobuf brut → JSON sera ajouté comme adaptateur séparé (Eclipse
 * Tahu) sans toucher à cette logique métier.
 *
 * <h2>Contrat de mapping</h2>
 * <ul>
 *   <li><b>deviceCode</b> ← {@code deviceId}, sinon {@code edgeNodeId/deviceId} composé
 *       ({@code edgeNodeId} + '/' + {@code deviceId} absent) ; à défaut {@code edgeNodeId}
 *       seul (cas NDATA d'un Edge Node sans device fils).</li>
 *   <li><b>metrics[]</b> ← {@code metrics[].name} (obligatoire) + {@code metrics[].value}
 *       (numérique requis) + {@code metrics[].unit} (optionnel).</li>
 *   <li><b>timestamp</b> ← {@code timestamp} (epoch millis numérique OU ISO-8601) ;
 *       chaque métrique peut porter son propre {@code timestamp} (priorité au métrique).
 *       {@code null} → horodatage serveur en aval.</li>
 * </ul>
 *
 * <p>Ne lève jamais d'exception : un champ invalide produit un {@link DecodeResult}
 * en échec OU une métrique ignorée (jamais d'exception qui tuerait le flux MQTT).
 */
public final class SparkplugBDecoder {

  /** Une mesure unitaire extraite d'une métrique Sparkplug. */
  public record DecodedMetric(String name, Double value, String unit, Instant recordedAt) {}

  /** Résultat du décodage : le code device + les métriques valides, OU une raison de rejet. */
  public record DecodeResult(
      String deviceCode, List<DecodedMetric> metrics, String dropReason) {

    public DecodeResult {
      metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }

    public static DecodeResult ok(String deviceCode, List<DecodedMetric> metrics) {
      return new DecodeResult(deviceCode, metrics, null);
    }

    public static DecodeResult dropped(String reason) {
      return new DecodeResult(null, List.of(), reason);
    }

    public boolean isDropped() {
      return dropReason != null;
    }
  }

  /** Décode un payload Sparkplug B (JSON). Ne lève jamais d'exception. */
  public DecodeResult decode(JsonNode root) {
    if (root == null || !root.isObject()) {
      return DecodeResult.dropped("payload is not a JSON object");
    }

    String deviceCode = extractDeviceCode(root);
    if (deviceCode == null) {
      return DecodeResult.dropped("no device code (deviceId / edgeNodeId)");
    }

    JsonNode metricsNode = root.get("metrics");
    if (metricsNode == null || !metricsNode.isArray() || metricsNode.isEmpty()) {
      return DecodeResult.dropped("no metrics array");
    }

    Instant payloadTs = parseTimestamp(root.get("timestamp"));

    List<DecodedMetric> decoded = new ArrayList<>();
    for (JsonNode m : metricsNode) {
      if (m == null || !m.isObject()) {
        continue;
      }
      String name = text(m, "name");
      if (name == null) {
        continue;
      }
      JsonNode valueNode = m.get("value");
      if (valueNode == null || !valueNode.isNumber()) {
        continue;
      }
      String unit = text(m, "unit");
      Instant metricTs = parseTimestamp(m.get("timestamp"));
      Instant recordedAt = metricTs != null ? metricTs : payloadTs;
      decoded.add(new DecodedMetric(name, valueNode.asDouble(), unit, recordedAt));
    }

    if (decoded.isEmpty()) {
      return DecodeResult.dropped("no valid metric (need name + numeric value)");
    }
    return DecodeResult.ok(deviceCode, decoded);
  }

  // ---------------------------------------------------------------------------

  private static String extractDeviceCode(JsonNode root) {
    String deviceId = text(root, "deviceId");
    if (deviceId != null) {
      return deviceId;
    }
    // Cas NDATA : pas de device fils → le code est l'Edge Node lui-même.
    return text(root, "edgeNodeId");
  }

  /** epoch millis (numérique) puis ISO-8601 (texte, avec offset toléré) — sinon null. */
  private static Instant parseTimestamp(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      long epochMillis = node.asLong();
      return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : null;
    }
    if (node.isTextual()) {
      String raw = node.asText();
      if (raw.isBlank()) {
        return null;
      }
      try {
        return Instant.parse(raw);
      } catch (DateTimeParseException ignored) {
        // Offset local toléré (2026-06-12T10:00:00+02:00).
      }
      try {
        return OffsetDateTime.parse(raw).toInstant();
      } catch (DateTimeParseException e) {
        return null;
      }
    }
    return null;
  }

  private static String text(JsonNode node, String field) {
    if (node == null) {
      return null;
    }
    JsonNode v = node.get(field);
    if (v == null || v.isNull() || !v.isTextual()) {
      return null;
    }
    String s = v.asText();
    return s.isBlank() ? null : s;
  }
}
