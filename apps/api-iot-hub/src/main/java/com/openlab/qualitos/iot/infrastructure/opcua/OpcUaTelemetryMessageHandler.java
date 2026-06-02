package com.openlab.qualitos.iot.infrastructure.opcua;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure, OPC-UA-stack-agnostic mapper from a monitored-item update (nodeId + value) to a
 * telemetry ingestion through the existing {@link IngestTelemetryUseCase}.
 *
 * <p>This class contains NO Eclipse Milo types so it can be unit-tested in isolation
 * (CLAUDE.md hexagonal conventions). The Milo client adapter
 * ({@link OpcUaIngestionConnector}) extracts the primitive {@code (nodeId, value, statusGood,
 * sourceTime)} from each {@code DataValue} and forwards them here.
 *
 * <h2>Wire contract (CLAUDE.md §9.4)</h2>
 * Each subscribed OPC-UA node is declared in configuration with a
 * {@link OpcUaProperties.NodeMapping} that binds {@code nodeId → (deviceCode, metric, unit)}.
 * The incoming value is the numeric reading of that node.
 *
 * <h2>Tenant safety (CLAUDE.md §18.2 rule 2 — CRITICAL)</h2>
 * The {@code tenantId} is NEVER read from the OPC-UA payload, the nodeId, or the
 * configuration. The node maps to a device <em>code</em>; the device is resolved via
 * {@link DeviceRepository#findUniqueByCode(String)} and the authoritative tenant is taken
 * from {@link Device#tenantId()}.
 *
 * <h2>Robustness</h2>
 * Unmapped node, bad OPC-UA status, non-numeric / null value, unknown or ambiguous device, or
 * any unexpected error are logged and swallowed — {@link #handle} never throws, so a single
 * bad item update can never tear down the subscription.
 */
public final class OpcUaTelemetryMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(OpcUaTelemetryMessageHandler.class);

  private final DeviceRepository deviceRepository;
  private final IngestTelemetryUseCase ingestUseCase;

  /** nodeId → mapping, resolved once at construction from the configured node list. */
  private final Map<String, OpcUaProperties.NodeMapping> mappingsByNodeId;

  public OpcUaTelemetryMessageHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      List<OpcUaProperties.NodeMapping> nodeMappings) {
    this.deviceRepository = Objects.requireNonNull(deviceRepository, "deviceRepository");
    this.ingestUseCase = Objects.requireNonNull(ingestUseCase, "ingestUseCase");
    this.mappingsByNodeId = new HashMap<>();
    if (nodeMappings != null) {
      for (OpcUaProperties.NodeMapping m : nodeMappings) {
        if (m != null && m.getNodeId() != null && !m.getNodeId().isBlank()) {
          this.mappingsByNodeId.put(m.getNodeId(), m);
        }
      }
    }
  }

  /** Configured node ids — used by the connector to build the monitored items. */
  public java.util.Set<String> configuredNodeIds() {
    return java.util.Collections.unmodifiableSet(mappingsByNodeId.keySet());
  }

  /**
   * Map and ingest one OPC-UA monitored-item update. Never throws — all failures are logged
   * and ignored.
   *
   * @param nodeId      the OPC-UA NodeId in string form (e.g. {@code ns=2;s=Fridge01.Temperature})
   * @param value       the numeric reading (already extracted from the {@code DataValue}); may be null
   * @param statusGood  whether the OPC-UA status code of the {@code DataValue} was Good
   * @param sourceTime  the OPC-UA source timestamp; may be null (server time used downstream)
   * @return {@code true} when a telemetry point was ingested, {@code false} when ignored.
   */
  public boolean handle(String nodeId, Double value, boolean statusGood, Instant sourceTime) {
    try {
      if (nodeId == null || nodeId.isBlank()) {
        LOG.warn("OPC-UA update dropped — null/blank nodeId");
        return false;
      }

      OpcUaProperties.NodeMapping mapping = mappingsByNodeId.get(nodeId);
      if (mapping == null) {
        LOG.warn("OPC-UA update dropped — node '{}' is not mapped to any device/metric", nodeId);
        return false;
      }

      if (!statusGood) {
        LOG.warn("OPC-UA update dropped — bad status for node '{}' (device='{}')",
            nodeId, mapping.getDeviceCode());
        return false;
      }

      if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
        LOG.warn("OPC-UA update dropped — missing/non-numeric value for node '{}' (device='{}')",
            nodeId, mapping.getDeviceCode());
        return false;
      }

      String deviceCode = mapping.getDeviceCode();
      String metric = mapping.getMetric();
      if (isBlank(deviceCode)) {
        LOG.warn("OPC-UA update dropped — node '{}' has no device code in mapping", nodeId);
        return false;
      }
      if (isBlank(metric)) {
        LOG.warn("OPC-UA update dropped — node '{}' has no metric in mapping (device='{}')",
            nodeId, deviceCode);
        return false;
      }

      // ---- TENANT RESOLUTION (never from the wire) -------------------------------
      Optional<Device> resolved = deviceRepository.findUniqueByCode(deviceCode);
      if (resolved.isEmpty()) {
        // Unknown OR ambiguous code: fail-closed, ignore, keep the subscription alive.
        LOG.warn("OPC-UA update dropped — unknown/ambiguous device code '{}' (node='{}')",
            deviceCode, nodeId);
        return false;
      }
      Device device = resolved.get();

      ingestUseCase.ingest(
          device.tenantId(), device.id(), metric, value, mapping.getUnit(), sourceTime);
      LOG.debug("Ingested OPC-UA telemetry node='{}' device='{}' tenant='{}' metric='{}' value={}",
          nodeId, deviceCode, device.tenantId(), metric, value);
      return true;

    } catch (RuntimeException ex) {
      // Last-resort guard: a single bad update must never tear down the subscription.
      LOG.warn("OPC-UA update dropped — unexpected error on node '{}': {}", nodeId, ex.getMessage(), ex);
      return false;
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
