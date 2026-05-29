"""Tests for the ONNX inference backend + the fallback factory.

We never depend on a real onnxruntime install: a fake `onnxruntime` module is
injected into `sys.modules` so the ONNX code path is exercised deterministically.
No `.onnx` file is shipped — the model file is a touch'd empty file and the
session is fully mocked.
"""

from __future__ import annotations

import sys
import types

import numpy as np
import pytest

from app.domain.analyzer import DeterministicStubBackend
from app.infrastructure import backend_factory
from app.infrastructure.backend_factory import build_backend


# --------------------------------------------------------------------------
# Fake onnxruntime injected into sys.modules
# --------------------------------------------------------------------------

def _install_fake_onnxruntime(monkeypatch, output_vector):
    """Register a fake `onnxruntime` whose InferenceSession returns
    `output_vector` (shape 1x5, values in 0..1)."""

    class _FakeInput:
        name = "input"

    class _FakeSession:
        def __init__(self, model_path, providers=None):
            self.model_path = model_path
            self.providers = providers

        def get_inputs(self):
            return [_FakeInput()]

        def run(self, output_names, feeds):
            # Sanity: the adapter must pass a single NCHW float32 tensor.
            tensor = feeds["input"]
            assert tensor.ndim == 4, "expected NCHW tensor"
            assert tensor.shape[0] == 1 and tensor.shape[1] == 3
            return [np.asarray([output_vector], dtype=np.float32)]

    fake = types.ModuleType("onnxruntime")
    fake.InferenceSession = _FakeSession
    monkeypatch.setitem(sys.modules, "onnxruntime", fake)
    return _FakeSession


@pytest.fixture
def fake_model_file(tmp_path):
    """An empty file standing in for a real .onnx model (session is mocked)."""
    p = tmp_path / "vision5s.onnx"
    p.write_bytes(b"")  # contents irrelevant — InferenceSession is faked
    return str(p)


# --------------------------------------------------------------------------
# (a) Without a model -> stub fallback
# --------------------------------------------------------------------------

def test_factory_no_model_path_uses_stub(monkeypatch):
    monkeypatch.delenv(backend_factory.ENV_MODEL_PATH, raising=False)
    backend = build_backend()
    assert isinstance(backend, DeterministicStubBackend)


def test_factory_missing_model_file_uses_stub(monkeypatch, tmp_path):
    monkeypatch.setenv(
        backend_factory.ENV_MODEL_PATH, str(tmp_path / "does-not-exist.onnx")
    )
    backend = build_backend()
    assert isinstance(backend, DeterministicStubBackend)


def test_factory_onnxruntime_unavailable_uses_stub(monkeypatch, fake_model_file):
    """Model file exists but onnxruntime cannot be imported -> stub fallback."""
    monkeypatch.setenv(backend_factory.ENV_MODEL_PATH, fake_model_file)
    # Force the import to fail.
    monkeypatch.setitem(sys.modules, "onnxruntime", None)
    backend = build_backend()
    assert isinstance(backend, DeterministicStubBackend)


# --------------------------------------------------------------------------
# (b) With a mocked model -> ONNX path is taken, structured score produced
# --------------------------------------------------------------------------

def test_factory_selects_onnx_when_available(monkeypatch, fake_model_file):
    _install_fake_onnxruntime(monkeypatch, [0.9, 0.8, 0.7, 0.6, 0.5])
    monkeypatch.setenv(backend_factory.ENV_MODEL_PATH, fake_model_file)

    from app.infrastructure.onnx_backend import OnnxInferenceBackend

    backend = build_backend()
    assert isinstance(backend, OnnxInferenceBackend)


def test_onnx_backend_produces_structured_score(monkeypatch, fake_model_file, jpeg_bytes):
    _install_fake_onnxruntime(monkeypatch, [0.9, 0.8, 0.7, 0.6, 0.5])

    from app.infrastructure.onnx_backend import OnnxInferenceBackend

    backend = OnnxInferenceBackend(fake_model_file, input_size=64)
    result = backend.analyze(jpeg_bytes, 320, 240)

    assert len(result.image_sha256) == 64
    assert result.width == 320 and result.height == 240
    # 0..1 model output scaled to 0..100.
    assert result.score.seiri == 90
    assert result.score.seiton == 80
    assert result.score.seiso == 70
    assert result.score.seiketsu == 60
    assert result.score.shitsuke == 50
    assert result.score.overall == round((90 + 80 + 70 + 60 + 50) / 5)


def test_onnx_backend_emits_findings_for_low_scores(monkeypatch, fake_model_file, jpeg_bytes):
    # All pillars below 0.60 -> below 60 -> a finding per pillar.
    _install_fake_onnxruntime(monkeypatch, [0.40, 0.30, 0.20, 0.10, 0.55])

    from app.infrastructure.onnx_backend import OnnxInferenceBackend

    backend = OnnxInferenceBackend(fake_model_file, input_size=32)
    result = backend.analyze(jpeg_bytes, 100, 100)

    assert len(result.findings) == 5
    # < 45 -> CRITICAL, else WARNING.
    by_pillar = {f.pillar.value: f for f in result.findings}
    assert by_pillar["seiri"].severity.value == "critical"   # 40
    assert by_pillar["shitsuke"].severity.value == "warning"  # 55
    for f in result.findings:
        assert 0.0 <= f.confidence <= 1.0


# --------------------------------------------------------------------------
# (c) Score stays within bounds even for adversarial model output
# --------------------------------------------------------------------------

def test_onnx_score_clamped_within_bounds(monkeypatch, fake_model_file, jpeg_bytes):
    # Out-of-range / NaN values must be clamped to 0..1 then scaled.
    _install_fake_onnxruntime(
        monkeypatch, [1.5, -0.3, float("nan"), 0.999, 2.0]
    )

    from app.infrastructure.onnx_backend import OnnxInferenceBackend

    backend = OnnxInferenceBackend(fake_model_file, input_size=32)
    result = backend.analyze(jpeg_bytes, 50, 50)

    for value in result.score.as_dict().values():
        assert 0 <= value <= 100
    assert result.score.seiri == 100   # clamped from 1.5
    assert result.score.seiton == 0    # clamped from -0.3
    assert result.score.seiso == 0     # NaN -> 0
    assert result.score.shitsuke == 100  # clamped from 2.0


def test_onnx_backend_rejects_short_output(monkeypatch, fake_model_file, jpeg_bytes):
    from app.infrastructure.onnx_backend import (
        OnnxBackendUnavailable,
        OnnxInferenceBackend,
    )

    _install_fake_onnxruntime(monkeypatch, [0.9, 0.8])  # only 2 values

    backend = OnnxInferenceBackend(fake_model_file, input_size=32)
    with pytest.raises(OnnxBackendUnavailable):
        backend.analyze(jpeg_bytes, 10, 10)


def test_onnx_backend_unavailable_when_model_missing(monkeypatch, tmp_path):
    from app.infrastructure.onnx_backend import (
        OnnxBackendUnavailable,
        OnnxInferenceBackend,
    )

    _install_fake_onnxruntime(monkeypatch, [0.5, 0.5, 0.5, 0.5, 0.5])
    with pytest.raises(OnnxBackendUnavailable):
        OnnxInferenceBackend(str(tmp_path / "nope.onnx"))


def test_onnx_backend_session_construction_failure(monkeypatch, fake_model_file):
    """InferenceSession raising (e.g. corrupt model) -> OnnxBackendUnavailable,
    which the factory turns into a stub fallback."""
    class _BoomSession:
        def __init__(self, *a, **k):
            raise RuntimeError("corrupt model")

    fake = types.ModuleType("onnxruntime")
    fake.InferenceSession = _BoomSession
    monkeypatch.setitem(sys.modules, "onnxruntime", fake)
    monkeypatch.setenv(backend_factory.ENV_MODEL_PATH, fake_model_file)

    # Direct construction raises.
    from app.infrastructure.onnx_backend import (
        OnnxBackendUnavailable,
        OnnxInferenceBackend,
    )
    with pytest.raises(OnnxBackendUnavailable):
        OnnxInferenceBackend(fake_model_file)

    # Factory degrades gracefully to the stub.
    assert isinstance(build_backend(), DeterministicStubBackend)


def test_input_size_env_parsing(monkeypatch):
    monkeypatch.setenv(backend_factory.ENV_INPUT_SIZE, "320")
    assert backend_factory._input_size() == 320
    monkeypatch.setenv(backend_factory.ENV_INPUT_SIZE, "garbage")
    assert backend_factory._input_size() == 224
    monkeypatch.setenv(backend_factory.ENV_INPUT_SIZE, "-5")
    assert backend_factory._input_size() == 224
    monkeypatch.delenv(backend_factory.ENV_INPUT_SIZE, raising=False)
    assert backend_factory._input_size() == 224
