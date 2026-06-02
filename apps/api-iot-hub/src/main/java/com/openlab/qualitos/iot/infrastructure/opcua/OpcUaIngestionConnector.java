package com.openlab.qualitos.iot.infrastructure.opcua;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Eclipse Milo OPC-UA client adapter — connects to a server, creates a subscription and
 * registers a data-change listener on every configured node, forwarding each {@code DataValue}
 * to the pure {@link OpcUaTelemetryMessageHandler}.
 *
 * <p>Activated ONLY when {@code qualitos.iot.opcua.enabled=true} (see
 * {@link OpcUaConnectorConfig}). When disabled (the default) this bean is not created, so no
 * OPC-UA server is needed in CI or existing test contexts.
 *
 * <p>This adapter holds zero business logic: value extraction is mechanical, while node→device
 * mapping, tenant derivation and ingestion all live in the unit-tested handler.
 */
public class OpcUaIngestionConnector {

  private static final Logger LOG = LoggerFactory.getLogger(OpcUaIngestionConnector.class);

  private final OpcUaProperties props;
  private final OpcUaTelemetryMessageHandler handler;

  private OpcUaClient client;
  private ManagedSubscription subscription;

  public OpcUaIngestionConnector(OpcUaProperties props, OpcUaTelemetryMessageHandler handler) {
    this.props = props;
    this.handler = handler;
  }

  @PostConstruct
  public void start() {
    try {
      client = OpcUaClient.create(
          props.getEndpointUrl(),
          endpointSelector(),
          configureBuilder());
      client.connect().get(props.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);

      subscription = ManagedSubscription.create(client, props.getPublishingIntervalMs());

      int created = 0;
      for (String nodeIdStr : handler.configuredNodeIds()) {
        Optional<NodeId> nodeId = NodeId.parseSafe(nodeIdStr);
        if (nodeId.isEmpty()) {
          LOG.warn("OPC-UA node skipped — unparseable NodeId '{}'", nodeIdStr);
          continue;
        }
        try {
          ManagedDataItem item = subscription.createDataItem(nodeId.get());
          item.addDataValueListener(value -> onDataValue(nodeIdStr, value));
          created++;
        } catch (Exception itemEx) {
          // One bad node must not abort the whole subscription.
          LOG.warn("OPC-UA node '{}' could not be monitored: {}", nodeIdStr, itemEx.getMessage());
        }
      }
      LOG.info("OPC-UA ingestion connector started — endpoint={} security={} monitoredNodes={}/{}",
          props.getEndpointUrl(), props.getSecurityPolicy(), created, handler.configuredNodeIds().size());

    } catch (Exception e) {
      // Do not crash the application context if the server is unreachable at boot. Log and continue.
      LOG.error("OPC-UA connector failed to start (endpoint={}): {}",
          props.getEndpointUrl(), e.getMessage(), e);
    }
  }

  @PreDestroy
  public void stop() {
    try {
      if (subscription != null) {
        subscription.delete();
      }
    } catch (Exception e) {
      LOG.warn("Error while deleting OPC-UA subscription: {}", e.getMessage());
    }
    if (client != null) {
      try {
        client.disconnect().get(props.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        LOG.info("OPC-UA ingestion connector stopped");
      } catch (Exception e) {
        LOG.warn("Error while stopping OPC-UA connector: {}", e.getMessage());
      }
    }
  }

  // ---- DataValue → pure handler ----------------------------------------------

  private void onDataValue(String nodeIdStr, DataValue value) {
    // Translate Milo types into primitives; all decisions are made by the pure handler.
    boolean statusGood = value != null
        && value.getStatusCode() != null
        && value.getStatusCode().isGood();
    Double numeric = extractNumeric(value);
    Instant sourceTime = extractSourceTime(value);
    handler.handle(nodeIdStr, numeric, statusGood, sourceTime);
  }

  private static Double extractNumeric(DataValue value) {
    if (value == null) return null;
    Variant variant = value.getValue();
    Object o = variant == null ? null : variant.getValue();
    if (o instanceof Number n) {
      return n.doubleValue();
    }
    if (o instanceof Boolean b) {
      return b ? 1.0 : 0.0;
    }
    return null; // non-numeric → handler ignores it
  }

  private static Instant extractSourceTime(DataValue value) {
    if (value == null) return null;
    DateTime st = value.getSourceTime();
    try {
      return st == null ? null : st.getJavaInstant();
    } catch (RuntimeException e) {
      return null;
    }
  }

  // ---- client configuration --------------------------------------------------

  /** Selects the discovered endpoint whose security policy matches the configured one. */
  private Function<List<EndpointDescription>, Optional<EndpointDescription>> endpointSelector() {
    String wantedUri = securityPolicy().getUri();
    return endpoints -> endpoints.stream()
        .filter(e -> wantedUri.equals(e.getSecurityPolicyUri()))
        .findFirst()
        .or(() -> endpoints.stream().findFirst());
  }

  private Function<OpcUaClientConfigBuilder, org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig>
      configureBuilder() {
    final IdentityProvider identity = identityProvider();
    return builder -> builder
        .setApplicationName(org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
            .english(props.getApplicationName()))
        .setApplicationUri(props.getApplicationUri())
        .setIdentityProvider(identity)
        .setRequestTimeout(org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned
            .uint(props.getRequestTimeoutMs()))
        .build();
  }

  private IdentityProvider identityProvider() {
    if (props.getUsername() != null && !props.getUsername().isBlank()) {
      return new UsernameProvider(props.getUsername(),
          props.getPassword() == null ? "" : props.getPassword());
    }
    return new AnonymousProvider();
  }

  private SecurityPolicy securityPolicy() {
    String configured = props.getSecurityPolicy();
    if (configured == null || configured.isBlank()) {
      return SecurityPolicy.None;
    }
    for (SecurityPolicy p : SecurityPolicy.values()) {
      if (p.name().equalsIgnoreCase(configured)) {
        return p;
      }
    }
    LOG.warn("Unknown OPC-UA security policy '{}' — falling back to None", configured);
    return SecurityPolicy.None;
  }
}
