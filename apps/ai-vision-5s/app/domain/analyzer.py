"""Deterministic stub 5S analyzer.

V1 ships a SHA-256-of-bytes-based stub so:
- tests are reproducible without a GPU/PyTorch dep,
- the public API contract is locked,
- the real YOLOv8 model can plug in behind the same Protocol later.

Real CV model (YOLOv8 fine-tuned on 5S labels) is added in P5 via a
`InferenceBackend` swap — no API change.
"""

from __future__ import annotations

import hashlib
from typing import Protocol

from .models import AnalysisResult, Finding, FiveSScore, Pillar, Severity


class InferenceBackend(Protocol):
    def analyze(self, image_bytes: bytes, width: int, height: int) -> AnalysisResult: ...


class DeterministicStubBackend:
    """Computes scores from the SHA-256 digest of the image bytes.

    No randomness — same image always yields the same scores. Good enough for:
    - smoke tests of the REST contract,
    - load-tests without a GPU,
    - early frontend development.
    """

    def analyze(self, image_bytes: bytes, width: int, height: int) -> AnalysisResult:
        digest = hashlib.sha256(image_bytes).digest()
        # Use 5 different bytes from the digest as deterministic 0..100 scores.
        seiri = 30 + (digest[0] % 71)
        seiton = 30 + (digest[1] % 71)
        seiso = 30 + (digest[2] % 71)
        seiketsu = 30 + (digest[3] % 71)
        shitsuke = 30 + (digest[4] % 71)

        score = FiveSScore(seiri, seiton, seiso, seiketsu, shitsuke)
        findings = self._findings_for_low_scores(score)

        return AnalysisResult(
            image_sha256=hashlib.sha256(image_bytes).hexdigest(),
            width=width,
            height=height,
            score=score,
            findings=findings,
        )

    @staticmethod
    def _findings_for_low_scores(score: FiveSScore) -> list[Finding]:
        findings: list[Finding] = []
        rules = [
            (Pillar.SEIRI, score.seiri, "Unused items detected in workspace"),
            (Pillar.SEITON, score.seiton, "Tools not stored in designated locations"),
            (Pillar.SEISO, score.seiso, "Cleanliness below standard"),
            (Pillar.SEIKETSU, score.seiketsu, "Standardization markings missing or faded"),
            (Pillar.SHITSUKE, score.shitsuke, "Audit checklist evidence missing"),
        ]
        for pillar, value, desc in rules:
            if value < 60:
                findings.append(Finding(
                    pillar=pillar,
                    description=desc,
                    severity=Severity.CRITICAL if value < 45 else Severity.WARNING,
                    confidence=round(1 - value / 100, 2),
                ))
        return findings
