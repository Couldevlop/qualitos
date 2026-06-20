"""Backend selection (composition root) — ONNX-or-fallback.

Selection rule (transparent fallback, no contract break — mirrors the Vision 5S
``backend_factory``):

1. If ``EDGE_ONNX_MODEL_PATH`` points to an existing file AND the ONNX backend
   can be constructed (onnxruntime/numpy importable, model loadable)
   -> :class:`OnnxInferenceBackend`.
2. Otherwise -> :class:`ThresholdFallbackBackend` built from the supplied
   threshold config.

The selected backend is logged at startup so operators can tell, from the logs
alone, whether the node runs real inference or the deterministic fallback.
"""

from __future__ import annotations

import logging
import os

from edge_inference.domain.inference import (
    InferenceBackend,
    ThresholdConfig,
    ThresholdFallbackBackend,
)

_LOG = logging.getLogger(__name__)

ENV_MODEL_PATH = "EDGE_ONNX_MODEL_PATH"
ENV_ANOMALY_THRESHOLD = "EDGE_ONNX_ANOMALY_THRESHOLD"
_DEFAULT_THRESHOLD = 0.5


def _anomaly_threshold() -> float:
    raw = os.getenv(ENV_ANOMALY_THRESHOLD)
    if not raw:
        return _DEFAULT_THRESHOLD
    try:
        value = float(raw)
    except ValueError:
        _LOG.warning(
            "backend.factory invalid %s=%r, using default %.2f",
            ENV_ANOMALY_THRESHOLD, raw, _DEFAULT_THRESHOLD,
        )
        return _DEFAULT_THRESHOLD
    return value if 0.0 <= value <= 1.0 else _DEFAULT_THRESHOLD


def build_backend(
    thresholds: dict[str, ThresholdConfig],
    *,
    default: ThresholdConfig | None = None,
    model_path: str | None = None,
) -> InferenceBackend:
    """Return the best available backend (ONNX or deterministic fallback).

    ``model_path`` overrides the ``EDGE_ONNX_MODEL_PATH`` env var when given
    (useful in tests / explicit wiring).
    """
    fallback = ThresholdFallbackBackend(thresholds, default=default)
    path = (model_path if model_path is not None else os.getenv(ENV_MODEL_PATH, "")).strip()

    if not path:
        _LOG.info(
            "backend.factory selected=fallback reason=no-model-path (%s unset)",
            ENV_MODEL_PATH,
        )
        return fallback

    if not os.path.isfile(path):
        _LOG.warning(
            "backend.factory selected=fallback reason=model-missing path=%s", path
        )
        return fallback

    # Import the adapter lazily so the fallback path never imports onnxruntime
    # transitively. Any construction failure degrades gracefully.
    try:
        from edge_inference.infrastructure.onnx_backend import OnnxInferenceBackend

        backend = OnnxInferenceBackend(path, anomaly_threshold=_anomaly_threshold())
    except Exception as exc:  # OnnxBackendUnavailable or import error
        _LOG.warning(
            "backend.factory selected=fallback reason=onnx-unavailable (%s: %s)",
            type(exc).__name__, exc,
        )
        return fallback

    _LOG.info("backend.factory selected=onnx path=%s", path)
    return backend
