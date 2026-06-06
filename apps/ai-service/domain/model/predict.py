"""Domain models for the predictive endpoints (CLAUDE.md §6.5, §12.1).

Three explainable predictions — every result carries the *why* (drivers,
slope, residuals) alongside the number (CLAUDE.md §12.3 « l'IA suggère,
l'humain décide ») :

- ``KpiForecast``       : will a KPI reach its target by the horizon?
- ``SupplierRiskScore`` : transparent weighted risk scoring of a supplier.
- ``NcClusteringResult``: similarity clusters over non-conformity texts.

V1 is a deliberate **statistical baseline** (OLS trend + residual intervals,
logistic weighted scoring, TF-IDF cosine clustering) : deterministic, cheap on
CPU, fully explainable. Heavier learners (LSTM/XGBoost/HDBSCAN+embeddings)
plug behind the same models when a GPU budget exists — same pattern as the
NLQ models (ADR 0014 §7.3).
"""
from __future__ import annotations

from dataclasses import dataclass, field


# ---- KPI forecast ---------------------------------------------------------------

@dataclass(frozen=True)
class KpiForecastPoint:
    step: int          # 1..horizon (periods after the last observation)
    value: float       # point forecast
    low: float         # 95 % prediction interval
    high: float


@dataclass(frozen=True)
class KpiForecast:
    n: int
    slope: float               # trend per period (explainability)
    intercept: float
    residual_sigma: float
    r2: float                  # goodness of fit of the trend (0..1)
    horizon: int
    target: float
    direction: str             # "at_least" | "at_most"
    probability: float         # P(target reached at the horizon), 0..1
    confidence: str            # "low" | "medium" | "high" (data quantity + fit)
    points: list[KpiForecastPoint] = field(default_factory=list)


# ---- Supplier risk --------------------------------------------------------------

@dataclass(frozen=True)
class RiskDriver:
    """One feature's contribution to the score — the explanation unit."""

    feature: str
    value: float           # raw input value
    weight: float          # model weight (documented, auditable)
    contribution: float    # weight × normalized value (signed)


@dataclass(frozen=True)
class SupplierRiskScore:
    score: float           # 0..100 (100 = highest risk)
    level: str             # "low" | "medium" | "high" | "critical"
    drivers: list[RiskDriver] = field(default_factory=list)


# ---- Non-conformity clustering ---------------------------------------------------

@dataclass(frozen=True)
class NcCluster:
    cluster_id: int
    indices: list[int]     # positions in the submitted list
    size: int
    top_terms: list[str]   # most representative terms (explainability)


@dataclass(frozen=True)
class NcClusteringResult:
    n: int
    clusters: list[NcCluster] = field(default_factory=list)
    noise_indices: list[int] = field(default_factory=list)

    @property
    def clustered_ratio(self) -> float:
        return 0.0 if self.n == 0 else 1.0 - len(self.noise_indices) / self.n
