"""Tests for the deterministic fallback backend + orchestrator wiring."""

from __future__ import annotations

import pytest

from edge_inference.domain.buffer import StoreAndForwardBuffer
from edge_inference.domain.inference import (
    InferenceOrchestrator,
    ThresholdConfig,
    ThresholdFallbackBackend,
)
from edge_inference.domain.models import Severity, TelemetrySample


def _sample(value: float, metric: str = "temperature", ts: int = 1) -> TelemetrySample:
    return TelemetrySample(
        device_id="FRIDGE-01", metric=metric, value=value, timestamp_ms=ts
    )


def test_threshold_config_validation():
    with pytest.raises(ValueError):
        ThresholdConfig(low=10, high=2)
    with pytest.raises(ValueError):
        ThresholdConfig(low=0, high=8, warning_margin=-0.1)


def test_within_band_is_normal_and_deterministic():
    backend = ThresholdFallbackBackend({"temperature": ThresholdConfig(2, 8)})
    r1 = backend.infer(_sample(5.0))
    r2 = backend.infer(_sample(5.0))
    assert not r1.anomaly
    assert r1.severity is Severity.OK
    assert r1.score == 0.0
    assert r1 == r2  # deterministic
    assert r1.backend == "fallback"


def test_above_high_grades_score_and_severity():
    backend = ThresholdFallbackBackend(
        {"temperature": ThresholdConfig(2, 8, warning_margin=0.25)}
    )
    # band width = 6. value 9 -> excess 1 -> score 1/6 ~ 0.1667 < 0.25 -> WARNING
    warn = backend.infer(_sample(9.0))
    assert warn.anomaly
    assert warn.reason == "above-high"
    assert warn.severity is Severity.WARNING
    assert warn.score == pytest.approx(0.1667, abs=1e-3)

    # value 20 -> excess 12 -> score capped at 1.0 -> CRITICAL
    crit = backend.infer(_sample(20.0))
    assert crit.severity is Severity.CRITICAL
    assert crit.score == 1.0


def test_below_low_is_flagged():
    backend = ThresholdFallbackBackend({"temperature": ThresholdConfig(2, 8)})
    r = backend.infer(_sample(-1.0))
    assert r.anomaly
    assert r.reason == "below-low"


def test_unknown_metric_without_default_is_normal_but_explained():
    backend = ThresholdFallbackBackend({"temperature": ThresholdConfig(2, 8)})
    r = backend.infer(_sample(999.0, metric="pressure"))
    assert not r.anomaly
    assert r.reason == "no-threshold-configured"


def test_unknown_metric_uses_default_config():
    backend = ThresholdFallbackBackend(
        {}, default=ThresholdConfig(0, 100, warning_margin=0.1)
    )
    r = backend.infer(_sample(150.0, metric="whatever"))
    assert r.anomaly
    assert r.severity is Severity.CRITICAL


def test_orchestrator_enqueues_only_anomalies():
    buf = StoreAndForwardBuffer(capacity=10)
    backend = ThresholdFallbackBackend({"temperature": ThresholdConfig(2, 8)})
    orch = InferenceOrchestrator(backend, buf)

    normal = orch.process(_sample(5.0, ts=1))
    assert not normal.anomaly
    assert buf.is_empty()

    anomaly = orch.process(_sample(50.0, ts=2))
    assert anomaly.anomaly
    assert len(buf) == 1
    queued = buf.peek()
    assert queued.event_type == "iot.anomaly.detected"
    assert queued.device_id == "FRIDGE-01"
    assert queued.severity is Severity.CRITICAL
    assert orch.backend_name == "fallback"


def test_orchestrator_batch_and_custom_event_type():
    buf = StoreAndForwardBuffer(capacity=10)
    backend = ThresholdFallbackBackend({"temperature": ThresholdConfig(2, 8)})
    orch = InferenceOrchestrator(backend, buf, event_type="edge.fridge.excursion")

    results = orch.process_batch(
        [_sample(5.0, ts=1), _sample(12.0, ts=2), _sample(6.0, ts=3)]
    )
    assert [r.anomaly for r in results] == [False, True, False]
    assert len(buf) == 1
    assert buf.peek().event_type == "edge.fridge.excursion"


def test_multivariate_sample_model_input():
    s = TelemetrySample(
        device_id="COBOT-1",
        metric="vibration",
        value=0.0,
        timestamp_ms=1,
        features=(0.1, 0.2, 0.3),
    )
    assert s.model_input() == (0.1, 0.2, 0.3)
    # univariate falls back to [value]
    assert _sample(4.2).model_input() == (4.2,)
