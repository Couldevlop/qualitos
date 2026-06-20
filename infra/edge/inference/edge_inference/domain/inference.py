"""Inference backends + orchestrator (domain layer).

Two backends implement the same :class:`InferenceBackend` Protocol:

- :class:`ThresholdFallbackBackend` — pure, deterministic, dependency-free. It
  flags an anomaly when a sample leaves a configured ``[low, high]`` band and
  derives a graded score from how far outside the band it sits. This is the
  "no model supplied" path and is always available.
- ``OnnxInferenceBackend`` (in ``infrastructure.onnx_backend``) — real model,
  lazily importing ``onnxruntime``; selected only when a model file is present.

The :class:`InferenceOrchestrator` ties a backend to the store-and-forward
buffer: every anomalous result is turned into a :class:`HubEvent` and enqueued
for later push. It performs **no** network I/O.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

from .buffer import StoreAndForwardBuffer
from .models import HubEvent, InferenceResult, Severity, TelemetrySample


class InferenceBackend(Protocol):
    name: str

    def infer(self, sample: TelemetrySample) -> InferenceResult: ...


@dataclass(frozen=True, slots=True)
class ThresholdConfig:
    """Per-metric thresholds for the deterministic fallback.

    ``low``/``high`` define the normal band. ``warning_margin`` is the fraction
    of the band width beyond which a breach escalates from WARNING to CRITICAL.
    """

    low: float
    high: float
    warning_margin: float = 0.25

    def __post_init__(self) -> None:
        if self.high <= self.low:
            raise ValueError("high must be greater than low")
        if self.warning_margin < 0:
            raise ValueError("warning_margin must be >= 0")

    @property
    def width(self) -> float:
        return self.high - self.low


class ThresholdFallbackBackend:
    """Deterministic, model-free anomaly detector.

    Same input always yields the same verdict (no randomness, no state). Good
    enough to keep the gateway protecting the line when no ONNX model has been
    provisioned yet, and as the graceful-degradation path when the model fails
    to load (mirrors the Vision 5S deterministic stub).
    """

    name = "fallback"

    def __init__(
        self, thresholds: dict[str, ThresholdConfig], *, default: ThresholdConfig | None = None
    ) -> None:
        self._thresholds = dict(thresholds)
        self._default = default

    def _config_for(self, metric: str) -> ThresholdConfig | None:
        return self._thresholds.get(metric, self._default)

    def infer(self, sample: TelemetrySample) -> InferenceResult:
        cfg = self._config_for(sample.metric)
        if cfg is None:
            # Unknown metric, no default -> cannot judge; treat as normal but
            # state it explicitly so it is auditable.
            return InferenceResult(
                device_id=sample.device_id,
                metric=sample.metric,
                score=0.0,
                severity=Severity.OK,
                anomaly=False,
                backend=self.name,
                reason="no-threshold-configured",
                timestamp_ms=sample.timestamp_ms,
            )

        value = sample.value
        if cfg.low <= value <= cfg.high:
            return InferenceResult(
                device_id=sample.device_id,
                metric=sample.metric,
                score=0.0,
                severity=Severity.OK,
                anomaly=False,
                backend=self.name,
                reason="within-band",
                timestamp_ms=sample.timestamp_ms,
            )

        # Outside the band: distance beyond the nearest bound, normalised by the
        # band width, capped at 1.0 -> a graded 0..1 anomaly score.
        if value < cfg.low:
            excess = cfg.low - value
            side = "below-low"
        else:
            excess = value - cfg.high
            side = "above-high"
        score = min(1.0, excess / cfg.width)
        severity = (
            Severity.CRITICAL
            if score >= cfg.warning_margin
            else Severity.WARNING
        )
        return InferenceResult(
            device_id=sample.device_id,
            metric=sample.metric,
            score=round(score, 4),
            severity=severity,
            anomaly=True,
            backend=self.name,
            reason=side,
            timestamp_ms=sample.timestamp_ms,
        )


class InferenceOrchestrator:
    """Runs samples through a backend and queues anomalies for the hub."""

    def __init__(
        self,
        backend: InferenceBackend,
        buffer: StoreAndForwardBuffer,
        *,
        event_type: str = "iot.anomaly.detected",
    ) -> None:
        self._backend = backend
        self._buffer = buffer
        self._event_type = event_type

    @property
    def backend_name(self) -> str:
        return self._backend.name

    def process(self, sample: TelemetrySample) -> InferenceResult:
        """Infer on one sample; enqueue a :class:`HubEvent` if anomalous."""
        result = self._backend.infer(sample)
        if result.anomaly:
            event = HubEvent.from_result(result, event_type=self._event_type)
            self._buffer.enqueue(event)
        return result

    def process_batch(self, samples: list[TelemetrySample]) -> list[InferenceResult]:
        return [self.process(s) for s in samples]
