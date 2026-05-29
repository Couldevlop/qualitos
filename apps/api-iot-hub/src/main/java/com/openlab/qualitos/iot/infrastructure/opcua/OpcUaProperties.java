package com.openlab.qualitos.iot.infrastructure.opcua;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the OPC-UA ingestion connector (CLAUDE.md §9.4 — industrial flagship
 * protocol, IEC 62541).
 *
 * <p>Bound from {@code qualitos.iot.opcua.*}. The connector is disabled by default
 * ({@link #enabled} = false) so that no OPC-UA server is required in CI or in any context
 * that does not explicitly opt in.
 */
@ConfigurationProperties(prefix = "qualitos.iot.opcua")
public class OpcUaProperties {

  /** Master switch. When false the connector beans are not created. */
  private boolean enabled = false;

  /** Server endpoint, e.g. {@code opc.tcp://plc:4840}. */
  private String endpointUrl = "opc.tcp://localhost:4840";

  /** Application URI advertised by this client (must match the client certificate when secured). */
  private String applicationUri = "urn:qualitos:iot-hub:opcua-client";

  /** Application name advertised by this client. */
  private String applicationName = "QualitOS IoT Hub OPC-UA Client";

  /**
   * Security policy URI fragment. {@code None} for unsecured channels (anonymous, dev / behind
   * an Edge Gateway, §9.8). Other values map to the OPC-UA {@code SecurityPolicy} enum, e.g.
   * {@code Basic256Sha256}.
   */
  private String securityPolicy = "None";

  /** Optional username (omit for anonymous identity). */
  private String username;

  /** Optional password (used only when {@link #username} is set). */
  private String password;

  /** Optional PKCS#12 keystore path for client certificate (mTLS / signed channels). */
  private String keystorePath;

  /** Optional keystore password. */
  private String keystorePassword;

  /** OPC-UA publishing interval in milliseconds for the subscription. */
  private double publishingIntervalMs = 1000.0;

  /** Connection request timeout in milliseconds. */
  private long requestTimeoutMs = 10_000L;

  /**
   * Nodes to subscribe to. Each entry maps an OPC-UA {@code nodeId} to a QualitOS
   * {@code (deviceCode, metric)}. The tenant is NEVER configured here — it is resolved at
   * ingestion time from the device record (CLAUDE.md §18.2 rule 2).
   */
  private List<NodeMapping> nodes = new ArrayList<>();

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public String getEndpointUrl() { return endpointUrl; }
  public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

  public String getApplicationUri() { return applicationUri; }
  public void setApplicationUri(String applicationUri) { this.applicationUri = applicationUri; }

  public String getApplicationName() { return applicationName; }
  public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

  public String getSecurityPolicy() { return securityPolicy; }
  public void setSecurityPolicy(String securityPolicy) { this.securityPolicy = securityPolicy; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getKeystorePath() { return keystorePath; }
  public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }

  public String getKeystorePassword() { return keystorePassword; }
  public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }

  public double getPublishingIntervalMs() { return publishingIntervalMs; }
  public void setPublishingIntervalMs(double publishingIntervalMs) { this.publishingIntervalMs = publishingIntervalMs; }

  public long getRequestTimeoutMs() { return requestTimeoutMs; }
  public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

  public List<NodeMapping> getNodes() { return nodes; }
  public void setNodes(List<NodeMapping> nodes) { this.nodes = nodes == null ? new ArrayList<>() : nodes; }

  /**
   * Declarative mapping of one OPC-UA node to a QualitOS device metric.
   *
   * <p>Example (YAML):
   * <pre>
   * qualitos.iot.opcua.nodes:
   *   - node-id: "ns=2;s=Fridge01.Temperature"
   *     device-code: "FRIDGE-01"
   *     metric: "temperature"
   *     unit: "C"
   * </pre>
   */
  public static class NodeMapping {
    /** OPC-UA NodeId in string form, e.g. {@code ns=2;s=Fridge01.Temperature} or {@code ns=2;i=1001}. */
    private String nodeId;
    /** QualitOS device code (resolved to a device → authoritative tenant at ingestion time). */
    private String deviceCode;
    /** Metric name recorded on the telemetry point, e.g. {@code temperature}. */
    private String metric;
    /** Optional unit recorded on the telemetry point, e.g. {@code C}, {@code bar}. */
    private String unit;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
  }
}
