"""ONNX Runtime inference adapter for the edge component.

Hexagonal *adapter* implementing the domain
:class:`edge_inference.domain.inference.InferenceBackend` Protocol. It depends on
``onnxruntime`` and ``numpy`` — both **optional**. The imports are deferred
(lazy) so that:

- the pure domain layer stays framework-free,
- the deterministic fallback never requires these libraries,
- the component still runs when onnxruntime is not installed (the factory falls
  back to :class:`ThresholdFallbackBackend`).

Model contract (V1 — univariate/multivariate anomaly head):

- input : a single feature vector, shape ``[1, n_features]``, float32, where
  ``n_features`` matches ``TelemetrySample.model_input()``.
- output: a single anomaly score in ``0.0`` (normal) .. ``1.0`` (anomaly). A
  ``1xN`` output is reduced by taking ``max`` (one-vs-rest), so reconstruction-
  error autoencoders and classifier heads both fit.

No trained model ships with the repo. The real inference path is exercised in
tests by mocking ``onnxruntime.InferenceSession``.
"""

from __future__ import annotations

import logging
import os

from edge_inference.domain.models import InferenceResult, Severity, TelemetrySample

_LOG = logging.getLogger(__name__)


class OnnxBackendUnavailable(RuntimeError):
    """Raised when onnxruntime/numpy are missing or the model cannot be loaded.

    The factory catches this and degrades to the deterministic fallback.
    """


class OnnxInferenceBackend:
    """Real anomaly-detection backend backed by an ONNX model.

    Drop-in replacement for :class:`ThresholdFallbackBackend` (same Protocol).
    """

    name = "onnx"

    def __init__(self, model_path: str, *, anomaly_threshold: float = 0.5) -> None:
        self._model_path = model_path
        self._threshold = anomaly_threshold
        self._ort = self._import_onnxruntime()
        self._np = self._import_numpy()
        self._session = self._load_session(model_path)
        self._input_name = self._session.get_inputs()[0].name
        _LOG.info(
            "onnx.backend.ready model=%s input=%s threshold=%.3f",
            model_path, self._input_name, anomaly_threshold,
        )

    # -- construction helpers (lazy imports) -------------------------------

    @staticmethod
    def _import_onnxruntime():
        try:
            import onnxruntime as ort  # noqa: WPS433 (lazy import is intentional)
        except Exception as exc:  # pragma: no cover - import guard
            raise OnnxBackendUnavailable("onnxruntime is not installed") from exc
        return ort

    @staticmethod
    def _import_numpy():
        try:
            import numpy as np  # noqa: WPS433
        except Exception as exc:  # pragma: no cover - import guard
            raise OnnxBackendUnavailable("numpy is not installed") from exc
        return np

    def _load_session(self, model_path: str):
        if not os.path.isfile(model_path):
            raise OnnxBackendUnavailable(f"Model file not found: {model_path}")
        try:
            # CPU only by default; on-prem GPU/NPU edge nodes can extend providers.
            return self._ort.InferenceSession(
                model_path, providers=["CPUExecutionProvider"]
            )
        except Exception as exc:
            raise OnnxBackendUnavailable(
                f"Failed to load ONNX model: {model_path}"
            ) from exc

    # -- InferenceBackend Protocol -----------------------------------------

    def infer(self, sample: TelemetrySample) -> InferenceResult:
        np = self._np
        features = sample.model_input()
        tensor = np.asarray([features], dtype=np.float32)
        raw = self._session.run(None, {self._input_name: tensor})
        score = self._to_score(raw)
        anomaly = score >= self._threshold
        if not anomaly:
            severity = Severity.OK
        elif score >= (self._threshold + 1.0) / 2.0:
            severity = Severity.CRITICAL
        else:
            severity = Severity.WARNING
        return InferenceResult(
            device_id=sample.device_id,
            metric=sample.metric,
            score=round(score, 4),
            severity=severity,
            anomaly=anomaly,
            backend=self.name,
            reason="onnx-score",
            timestamp_ms=sample.timestamp_ms,
        )

    def _to_score(self, raw) -> float:
        np = self._np
        out = np.asarray(raw[0]).reshape(-1)
        if out.shape[0] == 0:
            raise OnnxBackendUnavailable("Model produced an empty output")
        value = float(np.max(out))
        if value != value:  # NaN guard
            value = 0.0
        return max(0.0, min(1.0, value))
