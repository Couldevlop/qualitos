package com.openlab.qualitos.iot.domain.port;

import com.openlab.qualitos.iot.domain.model.Threshold;

import java.util.List;
import java.util.UUID;

/** Lookup port: which thresholds apply to a given device+metric. */
public interface ThresholdRegistry {
  List<Threshold> findFor(UUID tenantId, UUID deviceId, String metric);
}
