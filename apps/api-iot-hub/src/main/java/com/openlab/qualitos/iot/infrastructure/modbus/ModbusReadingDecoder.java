package com.openlab.qualitos.iot.infrastructure.modbus;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Décodeur pur d'une lecture Modbus TCP/RTU déjà structurée en JSON {@link JsonNode} →
 * points de mesure QualitOS. Aucune dépendance Spring / HTTP : testable en isolation
 * (mêmes conventions hexagonales que {@code ModbusReadingHandler} et le handler MQTT).
 *
 * <h2>Format de lecture attendu (CLAUDE.md §9.4 — Modbus TCP/RTU)</h2>
 * Le décodage bas niveau du fil Modbus (lecture des registres bruts via une lib Modbus,
 * mots 16 bits, big/little-endian, regroupement float 32 bits…) est <b>délégué à la
 * passerelle Edge</b> (§9.5). QualitOS consomme une lecture déjà structurée :
 * <pre>{@code
 * {
 *   "deviceCode": "PLC-ATELIER-3",
 *   "readings": [
 *     { "register": 40001, "metric": "pressure",    "value": 4.2,  "unit": "bar"  },
 *     { "register": 40002, "metric": "temperature", "value": 55.0, "unit": "degC" }
 *   ],
 *   "time": "2026-06-16T08:30:00Z"   // ISO-8601 (offset toléré) OU epoch millisecondes ; optionnel
 * }
 * }</pre>
 *
 * <h2>Robustesse (jamais d'exception qui casse le lot)</h2>
 * Chaque lecture invalide (registre absent/non entier, valeur non numérique ou non finie,
 * metric vide, entrée non-objet) est <b>ignorée et comptée</b> dans
 * {@link DecodeResult#droppedReadings()}. Une lecture sans {@code readings} exploitable
 * renvoie une liste de mesures vide. {@link #decode(JsonNode)} ne lève jamais.
 *
 * <h2>Sécurité</h2>
 * Le décodeur n'extrait <em>aucun</em> tenant : seul le code device (résolu côté serveur)
 * fait autorité (§18.2 #2). Les valeurs venues du réseau ne sont jamais exécutées.
 */
public final class ModbusReadingDecoder {

  /** Une mesure unitaire issue d'un registre Modbus. */
  public record DecodedMeasurement(String metric, Double value, String unit, Instant recordedAt) {}

  /**
   * Résultat du décodage d'une lecture : code device (peut être {@code null} si absent),
   * mesures valides et nombre de lectures ignorées (registre/valeur invalide).
   */
  public record DecodeResult(String deviceCode, List<DecodedMeasurement> measurements, int droppedReadings) {

    public DecodeResult {
      measurements = measurements == null ? List.of() : List.copyOf(measurements);
    }

    public boolean hasDeviceCode() {
      return deviceCode != null && !deviceCode.isBlank();
    }
  }

  /**
   * Décode une lecture Modbus structurée. Ne lève jamais d'exception.
   *
   * @param reading corps JSON de la lecture (peut être {@code null} / non-objet)
   */
  public DecodeResult decode(JsonNode reading) {
    if (reading == null || !reading.isObject()) {
      return new DecodeResult(null, List.of(), 0);
    }

    String deviceCode = text(reading, "deviceCode");
    Instant recordedAt = parseInstant(reading.get("time"));

    JsonNode readings = reading.get("readings");
    if (readings == null || !readings.isArray() || readings.isEmpty()) {
      return new DecodeResult(deviceCode, List.of(), 0);
    }

    List<DecodedMeasurement> measurements = new ArrayList<>();
    int dropped = 0;

    for (JsonNode entry : readings) {
      // Chaque entrée doit être un objet portant un registre entier et une valeur numérique finie.
      if (entry == null || !entry.isObject()) {
        dropped++;
        continue;
      }

      JsonNode registerNode = entry.get("register");
      JsonNode valueNode = entry.get("value");

      boolean validRegister = registerNode != null && registerNode.isIntegralNumber();
      boolean validValue = valueNode != null && valueNode.isNumber() && Double.isFinite(valueNode.asDouble());

      if (!validRegister || !validValue) {
        dropped++;
        continue;
      }

      // Le nom de métrique est explicite quand fourni, sinon dérivé du n° de registre.
      String metric = text(entry, "metric");
      if (metric == null) {
        metric = "register_" + registerNode.asLong();
      }

      String unit = text(entry, "unit");
      measurements.add(new DecodedMeasurement(metric, valueNode.asDouble(), unit, recordedAt));
    }

    return new DecodeResult(deviceCode, measurements, dropped);
  }

  // ---------------------------------------------------------------------------

  /**
   * Horodatage : ISO-8601 strict (Instant) puis avec offset (OffsetDateTime), enfin epoch
   * millisecondes (numérique) — sinon {@code null} (horodatage serveur appliqué en aval).
   */
  private static Instant parseInstant(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    // epoch millisecondes : les passerelles industrielles émettent souvent un entier.
    if (node.isNumber()) {
      try {
        return Instant.ofEpochMilli(node.asLong());
      } catch (RuntimeException e) {
        return null;
      }
    }
    if (!node.isTextual()) {
      return null;
    }
    String raw = node.asText();
    if (raw.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(raw);
    } catch (DateTimeParseException ignored) {
      // Une passerelle peut émettre un offset local (2026-06-16T10:30:00+02:00).
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
