package com.openlab.qualitos.iot.infrastructure.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure, broker-agnostic mapper from an MQTT message (topic + JSON payload) to a
 * telemetry ingestion through the existing {@link IngestTelemetryUseCase}.
 *
 * <p>This class contains NO Eclipse Paho types so it can be unit-tested in isolation
 * (CLAUDE.md hexagonal conventions). The Paho client adapter
 * ({@link MqttIngestionConnector}) merely forwards raw {@code (topic, payloadBytes)}.
 *
 * <h2>Wire contract (CLAUDE.md §9.4)</h2>
 * <ul>
 *   <li><b>Topic:</b> {@code qualitos/<deviceCode>/<metric>} — segment 1 = device code,
 *       segment 2 = metric. A {@code deviceCode}/{@code metric} field in the payload may
 *       override the topic-derived value.</li>
 *   <li><b>Payload (JSON):</b> {@code {"value": 7.2, "unit": "C", "recordedAt": "2026-..."}}.
 *       {@code unit} and {@code recordedAt} are optional. {@code value} must be numeric.</li>
 * </ul>
 *
 * <h2>Tenant safety (CLAUDE.md §18.2 rule 2 — CRITICAL)</h2>
 * The {@code tenantId} is NEVER read from the topic or the payload. The message identifies a
 * <em>device</em> by code; the device is resolved via
 * {@link DeviceRepository#findUniqueByCode(String)} and the authoritative tenant is taken
 * from {@link Device#tenantId()}. Any {@code tenantId} present in the payload is ignored.
 *
 * <h2>Robustness</h2>
 * Unknown device, malformed JSON, missing/non-numeric value, or any unexpected error are
 * logged and swallowed — {@link #handle(String, byte[])} never throws, so a single bad
 * message can never kill the subscription.
 */
public final class MqttTelemetryMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MqttTelemetryMessageHandler.class);

  private static final String TOPIC_PREFIX = "qualitos/";

  private final DeviceRepository deviceRepository;
  private final IngestTelemetryUseCase ingestUseCase;
  private final ObjectMapper objectMapper;

  public MqttTelemetryMessageHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      ObjectMapper objectMapper) {
    this.deviceRepository = Objects.requireNonNull(deviceRepository, "deviceRepository");
    this.ingestUseCase = Objects.requireNonNull(ingestUseCase, "ingestUseCase");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  /**
   * Map and ingest one MQTT message. Never throws — all failures are logged and ignored.
   *
   * @return {@code true} when a telemetry point was ingested, {@code false} when the
   *         message was ignored (unknown device, malformed, etc.). Mainly useful for tests.
   */
  public boolean handle(String topic, byte[] payload) {
    try {
      String body = payload == null ? "" : new String(payload, StandardCharsets.UTF_8);
      JsonNode root;
      try {
        root = objectMapper.readTree(body);
      } catch (Exception parseEx) {
        LOG.warn("MQTT message dropped — malformed JSON on topic '{}': {}", topic, parseEx.getMessage());
        return false;
      }
      if (root == null || !root.isObject()) {
        LOG.warn("MQTT message dropped — payload is not a JSON object on topic '{}'", topic);
        return false;
      }

      String[] segments = parseSegments(topic);
      String deviceCode = textOrNull(root, "deviceCode");
      if (deviceCode == null) {
        deviceCode = segments.length > 0 ? segments[0] : null;
      }
      String metric = textOrNull(root, "metric");
      if (metric == null) {
        metric = segments.length > 1 ? segments[1] : null;
      }

      if (isBlank(deviceCode)) {
        LOG.warn("MQTT message dropped — no device code in topic '{}' or payload", topic);
        return false;
      }
      if (isBlank(metric)) {
        LOG.warn("MQTT message dropped — no metric in topic '{}' or payload (device='{}')", topic, deviceCode);
        return false;
      }

      JsonNode valueNode = root.get("value");
      if (valueNode == null || !valueNode.isNumber()) {
        LOG.warn("MQTT message dropped — missing/non-numeric 'value' (device='{}', metric='{}')",
            deviceCode, metric);
        return false;
      }
      double value = valueNode.asDouble();
      String unit = textOrNull(root, "unit");
      Instant recordedAt = parseRecordedAt(root.get("recordedAt"));

      // ---- TENANT RESOLUTION (never from the wire) -------------------------------
      Optional<Device> resolved = deviceRepository.findUniqueByCode(deviceCode);
      if (resolved.isEmpty()) {
        // Unknown OR ambiguous code: fail-closed, ignore, keep the subscription alive.
        LOG.warn("MQTT message dropped — unknown/ambiguous device code '{}' (topic='{}')",
            deviceCode, topic);
        return false;
      }
      Device device = resolved.get();

      ingestUseCase.ingest(
          device.tenantId(), device.id(), metric, value, unit, recordedAt);
      LOG.debug("Ingested MQTT telemetry device='{}' tenant='{}' metric='{}' value={}",
          deviceCode, device.tenantId(), metric, value);
      return true;

    } catch (RuntimeException ex) {
      // Last-resort guard: a single bad message must never tear down the subscription.
      LOG.warn("MQTT message dropped — unexpected error on topic '{}': {}", topic, ex.getMessage(), ex);
      return false;
    }
  }

  private static String[] parseSegments(String topic) {
    if (topic == null) return new String[0];
    String t = topic.startsWith(TOPIC_PREFIX) ? topic.substring(TOPIC_PREFIX.length()) : topic;
    if (t.isEmpty()) return new String[0];
    return t.split("/");
  }

  private static String textOrNull(JsonNode node, String field) {
    JsonNode v = node.get(field);
    if (v == null || v.isNull() || !v.isTextual()) return null;
    String s = v.asText();
    return s.isBlank() ? null : s;
  }

  private static Instant parseRecordedAt(JsonNode node) {
    if (node == null || node.isNull() || !node.isTextual()) return null;
    try {
      return Instant.parse(node.asText());
    } catch (DateTimeParseException e) {
      // Non-fatal: fall back to server-side timestamp (handled downstream as null).
      LOG.debug("Invalid recordedAt '{}' — using server time", node.asText());
      return null;
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
