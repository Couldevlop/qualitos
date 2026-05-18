"""Pure-Python domain models for 5S vision analysis.

CLAUDE.md §3.2 — Seiri / Seiton / Seiso / Seiketsu / Shitsuke.
Score range 0-100 per pillar; overall score is the weighted average.

This module has NO dependency on FastAPI / aiohttp / pillow — anything that
talks to the outside world lives in `app.infrastructure`.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Mapping


class Pillar(str, Enum):
    SEIRI = "seiri"            # Sort
    SEITON = "seiton"          # Set in order
    SEISO = "seiso"            # Shine
    SEIKETSU = "seiketsu"      # Standardize
    SHITSUKE = "shitsuke"      # Sustain


class Severity(str, Enum):
    INFO = "info"
    WARNING = "warning"
    CRITICAL = "critical"


@dataclass(frozen=True)
class Finding:
    """A non-conformance detected on a photo."""
    pillar: Pillar
    description: str
    severity: Severity
    confidence: float  # 0.0..1.0
    bbox: tuple[int, int, int, int] | None = None  # x, y, w, h — optional


@dataclass(frozen=True)
class FiveSScore:
    """Per-pillar score + overall score (0..100)."""
    seiri: int
    seiton: int
    seiso: int
    seiketsu: int
    shitsuke: int

    @property
    def overall(self) -> int:
        # Equal weights across pillars (Lean default — can be overridden by industry pack).
        return round(
            (self.seiri + self.seiton + self.seiso + self.seiketsu + self.shitsuke) / 5
        )

    def as_dict(self) -> Mapping[str, int]:
        return {
            "seiri": self.seiri,
            "seiton": self.seiton,
            "seiso": self.seiso,
            "seiketsu": self.seiketsu,
            "shitsuke": self.shitsuke,
            "overall": self.overall,
        }


@dataclass(frozen=True)
class AnalysisResult:
    """Full analysis output for one image."""
    image_sha256: str
    width: int
    height: int
    score: FiveSScore
    findings: List[Finding] = field(default_factory=list)
