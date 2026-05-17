package com.openlab.qualitos.iot.infrastructure.external;

import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Bridges a {@link ThresholdBreachEvent} to api-quality-engine by:
 * <ol>
 *   <li>publishing a Spring application event for in-process listeners;</li>
 *   <li>calling api-quality-engine REST to open an NC — only if the target
 *   host is on the egress allow-list (OWASP A10 SSRF defense).</li>
 * </ol>
 */
@Component
public class QualityEngineNcPublisher implements NonConformancePublisher {

  private static final Logger LOG = LoggerFactory.getLogger(QualityEngineNcPublisher.class);

  private final ApplicationEventPublisher eventBus;
  private final RestTemplate restTemplate;
  private final String qualityEngineBaseUrl;
  private final Set<String> egressAllowList;

  public QualityEngineNcPublisher(
      ApplicationEventPublisher eventBus,
      RestTemplate restTemplate,
      @Value("${qualitos.iot.quality-engine.base-url:http://api-quality-engine:8082}") String qualityEngineBaseUrl,
      @Value("${qualitos.iot.quality-engine.allowed-hosts:api-quality-engine,localhost}") String allowed) {
    this.eventBus = eventBus;
    this.restTemplate = restTemplate;
    this.qualityEngineBaseUrl = qualityEngineBaseUrl;
    this.egressAllowList = Set.of(allowed.split(","));
  }

  @Override
  public void notifyBreach(ThresholdBreachEvent event) {
    // 1) in-process notification for any local listener (audit, metrics…)
    eventBus.publishEvent(event);

    // 2) egress allow-list check (OWASP A10)
    URI target;
    try {
      target = URI.create(qualityEngineBaseUrl + "/api/v1/nc/from-iot");
    } catch (IllegalArgumentException e) {
      LOG.warn("Invalid quality engine URL — dropping NC publish for device={}", event.deviceId(), e);
      return;
    }
    String host = target.getHost();
    if (host == null || !egressAllowList.contains(host)) {
      LOG.warn("Host not in egress allow-list (host={}) — dropping NC publish", host);
      return;
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    // Tenant context is carried in the JSON body (and validated by api-quality-engine
    // against its own JWT-derived tenantId — never trusted from outside).

    Map<String, Object> body = Map.of(
        "tenantId", event.tenantId().toString(),
        "deviceId", event.deviceId().toString(),
        "metric", event.metric(),
        "observedValue", event.observedValue(),
        "severity", event.rule().severity().name(),
        "occurredAt", event.occurredAt().toString());

    try {
      restTemplate.postForLocation(target, new HttpEntity<>(body, headers));
    } catch (RestClientException e) {
      LOG.warn("Failed to notify quality-engine for breach (device={}): {}",
          event.deviceId(), e.getMessage());
      // Resilience: in production we'd buffer to outbox table + retry — left for P5.
    }
  }
}
