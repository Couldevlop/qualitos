"""ONNX Runtime inference adapter for the Vision 5S service.

Hexagonal architecture: this is an *adapter* implementing the domain
`InferenceBackend` Protocol (see `app.domain.analyzer`). It depends on
`onnxruntime`, `numpy` and `Pillow` — all of which are **optional**. The imports
are deferred (lazy) so that:

- the pure domain layer stays framework-free,
- the deterministic stub fallback never requires these libraries,
- the service still boots when onnxruntime is not installed.

Model contract (V1 — fine-tuned 5S regression/classification head):
- input : a single RGB image tensor, NCHW, float32, normalized to 0..1,
  resized to `VISION5S_ONNX_INPUT_SIZE` (default 224x224).
- output: a 1x5 vector of per-pillar scores in 0..1 (Seiri, Seiton, Seiso,
  Seiketsu, Shitsuke). Values are clamped and scaled to the 0..100 domain range.

No trained model ships with the repo. The real inference path is exercised in
tests by mocking `onnxruntime.InferenceSession`.
"""

from __future__ import annotations

import hashlib
import logging
import os
from typing import TYPE_CHECKING

from app.domain.models import (
    AnalysisResult,
    Finding,
    FiveSScore,
    Pillar,
    Severity,
)

if TYPE_CHECKING:  # pragma: no cover - typing only
    import numpy as _np

_LOG = logging.getLogger(__name__)

_PILLAR_ORDER = (
    Pillar.SEIRI,
    Pillar.SEITON,
    Pillar.SEISO,
    Pillar.SEIKETSU,
    Pillar.SHITSUKE,
)

_FINDING_DESCRIPTIONS = {
    Pillar.SEIRI: "Unused items detected in workspace",
    Pillar.SEITON: "Tools not stored in designated locations",
    Pillar.SEISO: "Cleanliness below standard",
    Pillar.SEIKETSU: "Standardization markings missing or faded",
    Pillar.SHITSUKE: "Audit checklist evidence missing",
}


class OnnxBackendUnavailable(RuntimeError):
    """Raised when onnxruntime / numpy / Pillow are missing or the model
    cannot be loaded. The factory catches this and falls back to the stub."""


class OnnxInferenceBackend:
    """Real CV inference backend backed by an ONNX model.

    Implements the `app.domain.analyzer.InferenceBackend` Protocol so it is a
    drop-in replacement for `DeterministicStubBackend`.
    """

    def __init__(self, model_path: str, input_size: int = 224) -> None:
        self._model_path = model_path
        self._input_size = input_size
        self._ort = self._import_onnxruntime()
        self._np = self._import_numpy()
        self._session = self._load_session(model_path)
        self._input_name = self._session.get_inputs()[0].name
        _LOG.info(
            "onnx.backend.ready model=%s input=%s size=%d",
            model_path, self._input_name, input_size,
        )

    # -- construction helpers (lazy imports) -------------------------------

    @staticmethod
    def _import_onnxruntime():
        try:
            import onnxruntime as ort  # noqa: WPS433 (lazy import is intentional)
        except Exception as exc:  # pragma: no cover - import guard
            raise OnnxBackendUnavailable(
                "onnxruntime is not installed"
            ) from exc
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
            # CPU only by default; on-prem GPU tenants can extend providers.
            return self._ort.InferenceSession(
                model_path,
                providers=["CPUExecutionProvider"],
            )
        except Exception as exc:
            raise OnnxBackendUnavailable(
                f"Failed to load ONNX model: {model_path}"
            ) from exc

    # -- InferenceBackend Protocol -----------------------------------------

    def analyze(self, image_bytes: bytes, width: int, height: int) -> AnalysisResult:
        tensor = self._preprocess(image_bytes)
        raw = self._session.run(None, {self._input_name: tensor})
        score = self._to_score(raw)
        findings = self._findings_for_low_scores(score)
        return AnalysisResult(
            image_sha256=hashlib.sha256(image_bytes).hexdigest(),
            width=width,
            height=height,
            score=score,
            findings=findings,
        )

    # -- inference internals -----------------------------------------------

    def _preprocess(self, image_bytes: bytes):
        """Decode -> resize -> normalize -> NCHW float32 tensor.

        Pillow is imported lazily here; the image has already been sanitized
        (EXIF stripped, MIME validated) by `infrastructure.image_safety`.
        """
        import io  # noqa: WPS433

        try:
            from PIL import Image  # noqa: WPS433
        except Exception as exc:  # pragma: no cover - import guard
            raise OnnxBackendUnavailable("Pillow is not installed") from exc

        np = self._np
        size = self._input_size
        with Image.open(io.BytesIO(image_bytes)) as img:
            img = img.convert("RGB").resize((size, size))
            arr = np.asarray(img, dtype=np.float32) / 255.0  # HWC, 0..1
        # HWC -> CHW -> NCHW
        arr = np.transpose(arr, (2, 0, 1))
        arr = np.expand_dims(arr, axis=0)
        return np.ascontiguousarray(arr, dtype=np.float32)

    def _to_score(self, raw) -> FiveSScore:
        """Map the model output (1x5 in 0..1) to the 0..100 domain score."""
        np = self._np
        out = np.asarray(raw[0]).reshape(-1)
        if out.shape[0] < 5:
            raise OnnxBackendUnavailable(
                f"Model output has {out.shape[0]} values, expected >= 5"
            )
        values = []
        for i in range(5):
            v = float(out[i])
            # Clamp to 0..1 then scale, guarding against NaN / out-of-range.
            if v != v:  # NaN check
                v = 0.0
            v = max(0.0, min(1.0, v))
            values.append(int(round(v * 100)))
        return FiveSScore(*values)

    @staticmethod
    def _findings_for_low_scores(score: FiveSScore) -> list[Finding]:
        findings: list[Finding] = []
        per_pillar = {
            Pillar.SEIRI: score.seiri,
            Pillar.SEITON: score.seiton,
            Pillar.SEISO: score.seiso,
            Pillar.SEIKETSU: score.seiketsu,
            Pillar.SHITSUKE: score.shitsuke,
        }
        for pillar in _PILLAR_ORDER:
            value = per_pillar[pillar]
            if value < 60:
                findings.append(Finding(
                    pillar=pillar,
                    description=_FINDING_DESCRIPTIONS[pillar],
                    severity=Severity.CRITICAL if value < 45 else Severity.WARNING,
                    confidence=round(1 - value / 100, 2),
                ))
        return findings
