package com.openlab.qualitos.iot.domain.model;

/**
 * Static threshold rule (V1). Predictive / multivariate rules will be added in
 * P5 (cf. CLAUDE.md §9.7 — autoencoders, LSTM-AE).
 */
public record Threshold(
    String metric,
    Double min,
    Double max,
    Severity severity
) {
  public enum Severity { INFO, WARNING, CRITICAL }

  public boolean isBreached(double value) {
    if (min != null && value < min) return true;
    if (max != null && value > max) return true;
    return false;
  }
}
