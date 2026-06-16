package com.openlab.qualitos.iot.infrastructure.lorawan;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Décodeur pur d'un uplink LoRaWAN (TTN / ChirpStack) — JSON {@link JsonNode} → points
 * de mesure QualitOS. Aucune dépendance Spring / HTTP : testable en isolation (mêmes
 * conventions hexagonales que {@code FhirObservationMapper} et le handler MQTT).
 *
 * <h2>Format d'uplink attendu (CLAUDE.md §9.4 — LoRaWAN)</h2>
 * Les Network Servers (The Things Stack, ChirpStack) livrent déjà la charge utile
 * <em>décodée</em> par leur payload formatter ; QualitOS consomme ce JSON générique :
 * <pre>{@code
 * {
 *   "deviceName": "CAPTEUR-SOL-01",          // OU "devEUI": "...", champ configurable
 *   "decoded":   { "temperature": 4.2, "humidity": 55, "label": "ignored" },
 *   "units":     { "temperature": "degC", "humidity": "%RH" },
 *   "time":      "2026-06-16T08:30:00Z"      // ISO-8601, offset toléré ; optionnel
 * }
 * }</pre>
 *
 * <h2>Robustesse (jamais d'exception qui casse le lot)</h2>
 * Chaque mesure non numérique (texte, booléen, objet, null) est <b>ignorée et comptée</b>
 * dans {@link DecodeResult#droppedFields()} ; un uplink sans {@code decoded} exploitable
 * renvoie une liste de mesures vide. {@link #decode(JsonNode, String)} ne lève jamais.
 *
 * <h2>Sécurité</h2>
 * Le décodeur n'extrait <em>aucun</em> tenant : seul le code device (résolu côté serveur)
 * fait autorité (§18.2 #2). Les motifs cités sont neutralisés (A09, anti log-injection).
 */
public final class LoRaWanUplinkDecoder {

  /** Une mesure unitaire extraite du bloc {@code decoded}. */
  public record DecodedMeasurement(String metric, Double value, String unit, Instant recordedAt) {}

  /**
   * Résultat du décodage d'un uplink : code device (peut être {@code null} si absent),
   * mesures valides et nombre de champs ignorés (non numériques).
   */
  public record DecodeResult(String deviceCode, List<DecodedMeasurement> measurements, int droppedFields) {

    public DecodeResult {
      measurements = measurements == null ? List.of() : List.copyOf(measurements);
    }

    public boolean hasDeviceCode() {
      return deviceCode != null && !deviceCode.isBlank();
    }
  }

  /**
   * Décode un uplink LoRaWAN. Ne lève jamais d'exception.
   *
   * @param uplink        corps JSON de l'uplink (peut être {@code null} / non-objet)
   * @param deviceIdField nom du champ portant le code device (ex. {@code deviceName},
   *                      {@code devEUI}) — configurable car TTN et ChirpStack diffèrent
   */
  public DecodeResult decode(JsonNode uplink, String deviceIdField) {
    Objects.requireNonNull(deviceIdField, "deviceIdField");

    if (uplink == null || !uplink.isObject()) {
      return new DecodeResult(null, List.of(), 0);
    }

    String deviceCode = text(uplink, deviceIdField);
    Instant recordedAt = parseInstant(text(uplink, "time"));

    JsonNode decoded = uplink.get("decoded");
    if (decoded == null || !decoded.isObject() || decoded.isEmpty()) {
      return new DecodeResult(deviceCode, List.of(), 0);
    }

    JsonNode units = uplink.get("units");

    List<DecodedMeasurement> measurements = new ArrayList<>();
    int dropped = 0;

    for (Map.Entry<String, JsonNode> entry : decoded.properties()) {
      String metric = entry.getKey();
      JsonNode valueNode = entry.getValue();

      // Seules les valeurs numériques finies deviennent des points de télémétrie.
      if (metric == null || metric.isBlank()
          || valueNode == null || !valueNode.isNumber()
          || !Double.isFinite(valueNode.asDouble())) {
        dropped++;
        continue;
      }

      String unit = unitFor(units, metric);
      measurements.add(new DecodedMeasurement(metric, valueNode.asDouble(), unit, recordedAt));
    }

    return new DecodeResult(deviceCode, measurements, dropped);
  }

  // ---------------------------------------------------------------------------

  /** Unité déclarée pour {@code metric} dans le bloc {@code units}, sinon {@code null}. */
  private static String unitFor(JsonNode units, String metric) {
    if (units == null || !units.isObject()) {
      return null;
    }
    return text(units, metric);
  }

  /** ISO-8601 strict (Instant) puis avec offset (OffsetDateTime) — sinon {@code null}. */
  private static Instant parseInstant(String raw) {
    if (raw == null) {
      return null;
    }
    try {
      return Instant.parse(raw);
    } catch (DateTimeParseException ignored) {
      // Les NS LoRaWAN peuvent émettre un offset local (2026-06-16T10:30:00+02:00).
    }
    try {
      return OffsetDateTime.parse(raw).toInstant();
    } catch (DateTimeParseException e) {
      return null;
    }
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
