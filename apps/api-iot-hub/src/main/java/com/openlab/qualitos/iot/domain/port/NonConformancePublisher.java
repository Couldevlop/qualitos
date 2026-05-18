package com.openlab.qualitos.iot.domain.port;

import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;

/**
 * Output port — bridge to api-quality-engine to open a Non-Conformance when
 * a threshold is breached. Default infrastructure adapter is a Feign HTTP
 * client (allow-listed via egress config: A10 SSRF).
 */
public interface NonConformancePublisher {
  void notifyBreach(ThresholdBreachEvent event);
}
