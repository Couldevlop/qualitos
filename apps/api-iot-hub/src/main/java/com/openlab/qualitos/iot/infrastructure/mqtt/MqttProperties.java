package com.openlab.qualitos.iot.infrastructure.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the MQTT ingestion connector (CLAUDE.md §9.4).
 *
 * <p>Bound from {@code qualitos.iot.mqtt.*}. The connector is disabled by default
 * ({@link #enabled} = false) so that no broker is required in CI or in any context
 * that does not explicitly opt in.
 */
@ConfigurationProperties(prefix = "qualitos.iot.mqtt")
public class MqttProperties {

  /** Master switch. When false the connector bean is not created. */
  private boolean enabled = false;

  /** Broker URL, e.g. {@code tcp://emqx:1883} or {@code ssl://emqx:8883} for TLS. */
  private String brokerUrl = "tcp://localhost:1883";

  /** MQTT client identifier (must be stable for clean-session/persistent semantics). */
  private String clientId = "qualitos-iot-hub";

  /** Optional broker username (omit for anonymous brokers). */
  private String username;

  /** Optional broker password. */
  private String password;

  /**
   * Topic filter to subscribe to. Default mirrors the canonical schema
   * {@code qualitos/<deviceCode>/<metric>}.
   */
  private String topicFilter = "qualitos/+/+";

  /** QoS for the subscription (0,1,2). Default 1 = at-least-once. */
  private int qos = 1;

  /** Enable TLS (use an {@code ssl://} brokerUrl). Transport security only — mTLS device
   * auth is handled at the Edge Gateway layer (CLAUDE.md §9.8). */
  private boolean tlsEnabled = false;

  /** Connection / keep-alive tuning. */
  private int connectionTimeoutSeconds = 10;
  private int keepAliveIntervalSeconds = 30;
  private boolean automaticReconnect = true;

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public String getBrokerUrl() { return brokerUrl; }
  public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

  public String getClientId() { return clientId; }
  public void setClientId(String clientId) { this.clientId = clientId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getTopicFilter() { return topicFilter; }
  public void setTopicFilter(String topicFilter) { this.topicFilter = topicFilter; }

  public int getQos() { return qos; }
  public void setQos(int qos) { this.qos = qos; }

  public boolean isTlsEnabled() { return tlsEnabled; }
  public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }

  public int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
  public void setConnectionTimeoutSeconds(int v) { this.connectionTimeoutSeconds = v; }

  public int getKeepAliveIntervalSeconds() { return keepAliveIntervalSeconds; }
  public void setKeepAliveIntervalSeconds(int v) { this.keepAliveIntervalSeconds = v; }

  public boolean isAutomaticReconnect() { return automaticReconnect; }
  public void setAutomaticReconnect(boolean v) { this.automaticReconnect = v; }
}
