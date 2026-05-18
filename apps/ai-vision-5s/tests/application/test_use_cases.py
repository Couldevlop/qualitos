import pytest

from app.application.use_cases import AnalyzeImageUseCase, ScoreImageUseCase
from app.domain.analyzer import DeterministicStubBackend


def test_analyze_rejects_empty():
    uc = AnalyzeImageUseCase(DeterministicStubBackend())
    with pytest.raises(ValueError):
        uc.execute(b"", 320, 240)


def test_analyze_rejects_invalid_dim():
    uc = AnalyzeImageUseCase(DeterministicStubBackend())
    with pytest.raises(ValueError):
        uc.execute(b"abc", 0, 240)


def test_analyze_happy(jpeg_bytes):
    uc = AnalyzeImageUseCase(DeterministicStubBackend())
    result = uc.execute(jpeg_bytes, 320, 240)
    assert result.width == 320
    assert result.height == 240
    assert len(result.image_sha256) == 64


def test_score_only(jpeg_bytes):
    uc = ScoreImageUseCase(DeterministicStubBackend())
    s = uc.execute(jpeg_bytes, 320, 240)
    assert 0 <= s.overall <= 100
