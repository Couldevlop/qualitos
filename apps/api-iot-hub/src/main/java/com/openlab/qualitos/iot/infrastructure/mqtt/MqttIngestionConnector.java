package com.openlab.qualitos.iot.infrastructure.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Eclipse Paho MQTT v5 client adapter — connects to a broker, subscribes to the
 * configured topic filter and forwards every message to the pure
 * {@link MqttTelemetryMessageHandler}.
 *
 * <p>Activated ONLY when {@code qualitos.iot.mqtt.enabled=true} (see
 * {@link MqttConnectorConfig}). When disabled (the default) this bean is not created,
 * so no broker is needed in CI or existing test contexts.
 *
 * <p>This adapter holds zero business logic: parsing, device resolution and tenant
 * derivation all live in the unit-tested handler.
 */
public class MqttIngestionConnector implements MqttCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MqttIngestionConnector.class);

  private final com.openlab.qualitos.iot.infrastructure.mqtt.MqttProperties props;
  private final MqttTelemetryMessageHandler handler;

  private MqttClient client;

  public MqttIngestionConnector(
      com.openlab.qualitos.iot.infrastructure.mqtt.MqttProperties props,
      MqttTelemetryMessageHandler handler) {
    this.props = props;
    this.handler = handler;
  }

  @PostConstruct
  public void start() {
    try {
      client = new MqttClient(props.getBrokerUrl(), props.getClientId(), new MemoryPersistence());
      client.setCallback(this);

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setCleanStart(true);
      options.setAutomaticReconnect(props.isAutomaticReconnect());
      options.setConnectionTimeout(props.getConnectionTimeoutSeconds());
      options.setKeepAliveInterval(props.getKeepAliveIntervalSeconds());
      if (props.getUsername() != null && !props.getUsername().isBlank()) {
        options.setUserName(props.getUsername());
        if (props.getPassword() != null) {
          options.setPassword(props.getPassword().getBytes(StandardCharsets.UTF_8));
        }
      }
      // TLS: when ssl:// broker URL is used Paho negotiates TLS via the default
      // SSLSocketFactory. mTLS/device certs are terminated at the Edge Gateway (§9.8).

      client.connect(options);
      client.subscribe(props.getTopicFilter(), props.getQos());
      LOG.info("MQTT ingestion connector started — broker={} topic='{}' qos={}",
          props.getBrokerUrl(), props.getTopicFilter(), props.getQos());
    } catch (MqttException e) {
      // Do not crash the application context if the broker is unreachable at boot —
      // automaticReconnect will retry. Log and continue.
      LOG.error("MQTT connector failed to start (broker={}): {}",
          props.getBrokerUrl(), e.getMessage(), e);
    }
  }

  @PreDestroy
  public void stop() {
    if (client == null) return;
    try {
      if (client.isConnected()) {
        client.disconnect();
      }
      client.close();
      LOG.info("MQTT ingestion connector stopped");
    } catch (MqttException e) {
      LOG.warn("Error while stopping MQTT connector: {}", e.getMessage());
    }
  }

  // ---- MqttCallback ----------------------------------------------------------

  @Override
  public void messageArrived(String topic, MqttMessage message) {
    // Delegate to the pure handler — it never throws, protecting the Paho dispatch loop.
    handler.handle(topic, message == null ? null : message.getPayload());
  }

  @Override
  public void disconnected(MqttDisconnectResponse disconnectResponse) {
    LOG.warn("MQTT disconnected: {}",
        disconnectResponse == null ? "(unknown)" : disconnectResponse.getReasonString());
  }

  @Override
  public void mqttErrorOccurred(MqttException exception) {
    LOG.warn("MQTT error: {}", exception.getMessage());
  }

  @Override
  public void deliveryComplete(IMqttToken token) {
    // Inbound-only connector — nothing to do.
  }

  @Override
  public void connectComplete(boolean reconnect, String serverURI) {
    if (reconnect) {
      // Re-subscribe after an automatic reconnect (clean session loses subscriptions).
      try {
        client.subscribe(props.getTopicFilter(), props.getQos());
        LOG.info("MQTT reconnected to {} — re-subscribed to '{}'", serverURI, props.getTopicFilter());
      } catch (MqttException e) {
        LOG.warn("MQTT re-subscribe after reconnect failed: {}", e.getMessage());
      }
    }
  }

  @Override
  public void authPacketArrived(int reasonCode, MqttProperties properties) {
    // No enhanced-auth flow used.
  }
}
