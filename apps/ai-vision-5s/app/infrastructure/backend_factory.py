"""Inference backend selection (composition root for the analyzer).

Selection rule (transparent fallback, no API break):

1. If ``VISION5S_ONNX_MODEL_PATH`` points to an existing file AND the ONNX
   backend can be constructed (onnxruntime / numpy / Pillow importable, model
   loadable) -> :class:`OnnxInferenceBackend`.
2. Otherwise -> :class:`DeterministicStubBackend`.

The selected backend is logged at startup so operators can tell, from the logs
alone, whether the service is running real inference or the deterministic stub.
"""

from __future__ import annotations

import logging
import os

from app.domain.analyzer import DeterministicStubBackend, InferenceBackend

_LOG = logging.getLogger(__name__)

ENV_MODEL_PATH = "VISION5S_ONNX_MODEL_PATH"
ENV_INPUT_SIZE = "VISION5S_ONNX_INPUT_SIZE"
_DEFAULT_INPUT_SIZE = 224


def _input_size() -> int:
    raw = os.getenv(ENV_INPUT_SIZE)
    if not raw:
        return _DEFAULT_INPUT_SIZE
    try:
        size = int(raw)
    except ValueError:
        _LOG.warning(
            "backend.factory invalid %s=%r, using default %d",
            ENV_INPUT_SIZE, raw, _DEFAULT_INPUT_SIZE,
        )
        return _DEFAULT_INPUT_SIZE
    return size if size > 0 else _DEFAULT_INPUT_SIZE


def build_backend() -> InferenceBackend:
    """Return the best available inference backend (ONNX or stub fallback)."""
    model_path = os.getenv(ENV_MODEL_PATH, "").strip()

    if not model_path:
        _LOG.info(
            "backend.factory selected=stub reason=no-model-path (%s unset)",
            ENV_MODEL_PATH,
        )
        return DeterministicStubBackend()

    if not os.path.isfile(model_path):
        _LOG.warning(
            "backend.factory selected=stub reason=model-missing path=%s",
            model_path,
        )
        return DeterministicStubBackend()

    # Import the adapter lazily so the stub path never imports onnxruntime
    # transitively. Any construction failure (missing libs, bad model) is a
    # non-fatal fallback to the deterministic stub.
    try:
        from app.infrastructure.onnx_backend import (
            OnnxBackendUnavailable,
            OnnxInferenceBackend,
        )

        backend = OnnxInferenceBackend(model_path, input_size=_input_size())
    except Exception as exc:  # OnnxBackendUnavailable or import error
        # OnnxBackendUnavailable is the expected case; broaden to Exception so
        # an unexpected adapter error still degrades gracefully to the stub.
        name = type(exc).__name__
        _LOG.warning(
            "backend.factory selected=stub reason=onnx-unavailable (%s: %s)",
            name, exc,
        )
        return DeterministicStubBackend()

    _LOG.info("backend.factory selected=onnx path=%s", model_path)
    return backend
