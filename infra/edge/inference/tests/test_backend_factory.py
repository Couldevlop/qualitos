"""Tests for ONNX-vs-fallback backend selection.

The pure selection logic (fallback paths) is fully tested without any heavy
dependency. The real ONNX construction is tested by *mocking*
``onnxruntime.InferenceSession`` so it runs even where onnxruntime is installed;
a final test uses ``importorskip`` to assert the lazy import contract.
"""

from __future__ import annotations

import sys
import types

import pytest

from edge_inference.domain.inference import ThresholdConfig
from edge_inference.infrastructure import backend_factory
from edge_inference.infrastructure.backend_factory import build_backend

THRESHOLDS = {"temperature": ThresholdConfig(2, 8)}


def test_no_model_path_selects_fallback(monkeypatch):
    monkeypatch.delenv(backend_factory.ENV_MODEL_PATH, raising=False)
    backend = build_backend(THRESHOLDS)
    assert backend.name == "fallback"


def test_missing_model_file_selects_fallback():
    backend = build_backend(THRESHOLDS, model_path="/does/not/exist.onnx")
    assert backend.name == "fallback"


def test_env_var_model_path_missing_file_falls_back(monkeypatch):
    monkeypatch.setenv(backend_factory.ENV_MODEL_PATH, "/nope/model.onnx")
    backend = build_backend(THRESHOLDS)
    assert backend.name == "fallback"


def test_onnx_construction_failure_degrades_to_fallback(tmp_path, monkeypatch):
    # A real, existing file -> passes the isfile() gate ...
    model = tmp_path / "model.onnx"
    model.write_bytes(b"not-a-real-onnx")

    # ... but the adapter raises on construction -> graceful fallback.
    import edge_inference.infrastructure.onnx_backend as onnx_mod

    def _boom(self, *a, **k):
        raise onnx_mod.OnnxBackendUnavailable("forced")

    monkeypatch.setattr(onnx_mod.OnnxInferenceBackend, "__init__", _boom)
    backend = build_backend(THRESHOLDS, model_path=str(model))
    assert backend.name == "fallback"


def test_onnx_selected_when_construction_succeeds(tmp_path, monkeypatch):
    """Real ONNX path with onnxruntime/numpy mocked so it always runs."""
    model = tmp_path / "model.onnx"
    model.write_bytes(b"x")

    # Fake numpy with just enough surface for the adapter.
    np = pytest.importorskip("numpy")  # numpy is light; require it for realism

    class _FakeSession:
        def __init__(self, *a, **k):
            pass

        def get_inputs(self):
            return [types.SimpleNamespace(name="input")]

        def run(self, _outputs, _feeds):
            return [np.asarray([0.9], dtype=np.float32)]

    fake_ort = types.ModuleType("onnxruntime")
    fake_ort.InferenceSession = _FakeSession
    monkeypatch.setitem(sys.modules, "onnxruntime", fake_ort)

    backend = build_backend(THRESHOLDS, model_path=str(model))
    assert backend.name == "onnx"

    from edge_inference.domain.models import TelemetrySample

    result = backend.infer(
        TelemetrySample(device_id="D1", metric="temperature", value=1.0, timestamp_ms=1)
    )
    assert result.backend == "onnx"
    assert result.anomaly is True
    assert result.score == pytest.approx(0.9)


def test_anomaly_threshold_env_parsing(monkeypatch):
    monkeypatch.setenv(backend_factory.ENV_ANOMALY_THRESHOLD, "0.7")
    assert backend_factory._anomaly_threshold() == pytest.approx(0.7)
    monkeypatch.setenv(backend_factory.ENV_ANOMALY_THRESHOLD, "bad")
    assert backend_factory._anomaly_threshold() == backend_factory._DEFAULT_THRESHOLD
    monkeypatch.setenv(backend_factory.ENV_ANOMALY_THRESHOLD, "5")  # out of range
    assert backend_factory._anomaly_threshold() == backend_factory._DEFAULT_THRESHOLD
    monkeypatch.delenv(backend_factory.ENV_ANOMALY_THRESHOLD, raising=False)
    assert backend_factory._anomaly_threshold() == backend_factory._DEFAULT_THRESHOLD


def test_onnx_backend_unavailable_without_onnxruntime(monkeypatch, tmp_path):
    """If onnxruntime is genuinely absent, the adapter raises the typed error."""
    model = tmp_path / "model.onnx"
    model.write_bytes(b"x")
    # Force the import to fail regardless of the local environment.
    monkeypatch.setitem(sys.modules, "onnxruntime", None)
    from edge_inference.infrastructure.onnx_backend import (
        OnnxBackendUnavailable,
        OnnxInferenceBackend,
    )

    with pytest.raises(OnnxBackendUnavailable):
        OnnxInferenceBackend(str(model))
