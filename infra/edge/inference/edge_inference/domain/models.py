"""Domain value objects for the edge inference component.

All frozen dataclasses — immutable, hashable where it matters, and trivially
serialisable to JSON for the store-and-forward buffer. No I/O, no framework.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class Severity(str, Enum):
    """Severity of a detected anomaly, aligned with the platform NC criticities."""

    OK = "ok"
    WARNING = "warning"
    CRITICAL = "critical"


@dataclass(frozen=True, slots=True)
class TelemetrySample:
    """A single measurement coming off a sensor/equipment at the edge.

    ``device_id`` identifies the physical source (the IoT Hub resolves the
    tenant from the device registry — never from the payload, §18.2 rule 2).
    ``metric`` is the measured quantity (e.g. ``temperature``, ``vibration``).
    ``features`` carries the model input vector (numeric). For a univariate
    sensor it is simply ``[value]``; for multivariate models it holds several
    aligned features.
    """

    device_id: str
    metric: str
    value: float
    timestamp_ms: int
    features: tuple[float, ...] = field(default_factory=tuple)
    unit: str | None = None

    def model_input(self) -> tuple[float, ...]:
        """Return the feature vector fed to the model.

        Falls back to the scalar ``value`` when no explicit feature vector was
        provided, so univariate sensors need no extra wiring.
        """
        return self.features if self.features else (self.value,)


@dataclass(frozen=True, slots=True)
class InferenceResult:
    """Outcome of running one sample through the orchestrator."""

    device_id: str
    metric: str
    score: float
    """Anomaly score in ``0.0`` (normal) .. ``1.0`` (certain anomaly)."""
    severity: Severity
    anomaly: bool
    backend: str
    """Which backend produced the verdict: ``"onnx"`` or ``"fallback"``."""
    reason: str
    timestamp_ms: int


@dataclass(frozen=True, slots=True)
class HubEvent:
    """An anomaly event queued for the IoT Hub.

    This is *only* the contract + payload. No network call happens in this
    package — the event is placed on the store-and-forward buffer; the gateway's
    outbound mTLS MQTT bridge is responsible for the actual delivery (§9.8).
    """

    event_type: str
    device_id: str
    metric: str
    score: float
    severity: Severity
    backend: str
    reason: str
    timestamp_ms: int

    @classmethod
    def from_result(
        cls, result: InferenceResult, event_type: str = "iot.anomaly.detected"
    ) -> HubEvent:
        return cls(
            event_type=event_type,
            device_id=result.device_id,
            metric=result.metric,
            score=result.score,
            severity=result.severity,
            backend=result.backend,
            reason=result.reason,
            timestamp_ms=result.timestamp_ms,
        )
