"""Use cases for 5S vision."""

from __future__ import annotations

from app.domain.analyzer import InferenceBackend
from app.domain.models import AnalysisResult


class AnalyzeImageUseCase:
    """Orchestrates: validate -> run backend -> return result."""

    def __init__(self, backend: InferenceBackend) -> None:
        self._backend = backend

    def execute(
        self,
        image_bytes: bytes,
        width: int,
        height: int,
    ) -> AnalysisResult:
        if not image_bytes:
            raise ValueError("Empty image")
        if width <= 0 or height <= 0:
            raise ValueError("Invalid image dimensions")
        return self._backend.analyze(image_bytes, width, height)


class ScoreImageUseCase:
    """Light-weight wrapper that only returns the score (no findings)."""

    def __init__(self, backend: InferenceBackend) -> None:
        self._backend = backend

    def execute(self, image_bytes: bytes, width: int, height: int):
        result = self._backend.analyze(image_bytes, width, height)
        return result.score
