package com.openlab.qualitos.iot.infrastructure.fhir;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Mapper pur : ressource FHIR R5 {@code Observation} (JSON) → point de télémétrie
 * QualitOS. Aucune dépendance Spring / HTTP — testable en isolation (conventions
 * hexagonales, comme {@code MqttTelemetryMessageHandler}).
 *
 * <h2>Contrat de mapping (CLAUDE.md §9.4 — Santé)</h2>
 * <ul>
 *   <li><b>deviceCode</b> ← {@code Observation.device.identifier.value}, sinon
 *       {@code Observation.device.reference} (forme {@code Device/<code>}), sinon
 *       {@code Observation.device.display}.</li>
 *   <li><b>metric</b> ← {@code Observation.code.coding[0].code} (LOINC/SNOMED…),
 *       sinon {@code Observation.code.text}.</li>
 *   <li><b>value</b> ← {@code Observation.valueQuantity.value} (numérique requis).</li>
 *   <li><b>unit</b> ← {@code valueQuantity.unit}, sinon {@code valueQuantity.code} (UCUM).</li>
 *   <li><b>recordedAt</b> ← {@code effectiveDateTime}, sinon {@code issued}
 *       (ISO-8601, offset toléré) ; {@code null} → horodatage serveur en aval.</li>
 * </ul>
 *
 * <h2>Confidentialité (§22.9)</h2>
 * Les champs patient ({@code subject}, {@code performer}, …) sont volontairement
 * IGNORÉS : seule la mesure physique de l'équipement est extraite. Les raisons de
 * rejet ne citent jamais de données personnelles.
 */
public final class FhirObservationMapper {

  /** Point de télémétrie extrait d'une Observation valide. */
  public record MappedObservation(
      String deviceCode, String metric, Double value, String unit, Instant recordedAt) {}

  /** Résultat du mapping : observation OU raison de rejet (jamais les deux). */
  public record MappingResult(MappedObservation observation, String dropReason) {

    public static MappingResult ok(MappedObservation observation) {
      return new MappingResult(observation, null);
    }

    public static MappingResult dropped(String reason) {
      return new MappingResult(null, reason);
    }

    public boolean isDropped() {
      return dropReason != null;
    }
  }

  /** Mappe une ressource Observation. Ne lève jamais d'exception. */
  public MappingResult map(JsonNode observation) {
    if (observation == null || !observation.isObject()) {
      return MappingResult.dropped("resource is not a JSON object");
    }
    if (!"Observation".equals(text(observation, "resourceType"))) {
      return MappingResult.dropped("resourceType is not 'Observation'");
    }

    String deviceCode = extractDeviceCode(observation.get("device"));
    if (deviceCode == null) {
      return MappingResult.dropped("no device identifier (device.identifier.value / "
          + "device.reference 'Device/<code>' / device.display)");
    }

    String metric = extractMetric(observation.get("code"));
    if (metric == null) {
      return MappingResult.dropped("no metric (code.coding[0].code or code.text)");
    }

    JsonNode quantity = observation.get("valueQuantity");
    JsonNode valueNode = quantity == null ? null : quantity.get("value");
    if (valueNode == null || !valueNode.isNumber()) {
      return MappingResult.dropped("missing/non-numeric valueQuantity.value");
    }

    String unit = text(quantity, "unit");
    if (unit == null) {
      unit = text(quantity, "code");
    }

    Instant recordedAt = parseInstant(text(observation, "effectiveDateTime"));
    if (recordedAt == null) {
      recordedAt = parseInstant(text(observation, "issued"));
    }

    return MappingResult.ok(new MappedObservation(
        deviceCode, metric, valueNode.asDouble(), unit, recordedAt));
  }

  // ---------------------------------------------------------------------------

  private static String extractDeviceCode(JsonNode device) {
    if (device == null || !device.isObject()) return null;

    JsonNode identifier = device.get("identifier");
    if (identifier != null && identifier.isObject()) {
      String value = text(identifier, "value");
      if (value != null) return value;
    }

    String reference = text(device, "reference");
    if (reference != null && reference.startsWith("Device/")) {
      String code = reference.substring("Device/".length());
      if (!code.isBlank()) return code;
    }

    return text(device, "display");
  }

  private static String extractMetric(JsonNode code) {
    if (code == null || !code.isObject()) return null;

    JsonNode codings = code.get("coding");
    if (codings != null && codings.isArray() && !codings.isEmpty()) {
      String c = text(codings.get(0), "code");
      if (c != null) return c;
    }

    return text(code, "text");
  }

  /** ISO-8601 strict (Instant) puis avec offset (OffsetDateTime) — sinon null. */
  private static Instant parseInstant(String raw) {
    if (raw == null) return null;
    try {
      return Instant.parse(raw);
    } catch (DateTimeParseException ignored) {
      // FHIR autorise les offsets locaux (2026-06-04T10:00:00+02:00).
    }
    try {
      return OffsetDateTime.parse(raw).toInstant();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static String text(JsonNode node, String field) {
    if (node == null) return null;
    JsonNode v = node.get(field);
    if (v == null || v.isNull() || !v.isTextual()) return null;
    String s = v.asText();
    return s.isBlank() ? null : s;
  }
}
