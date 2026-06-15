"""Pydantic schemas for the predictive endpoints (§6.5)."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.predict import KpiForecast, NcClusteringResult, SupplierRiskScore


# ---- KPI forecast ---------------------------------------------------------------

class KpiForecastRequest(BaseModel):
    values: list[float] = Field(..., min_length=4, max_length=10000)
    target: float
    horizon: int = Field(default=6, ge=1, le=60)
    direction: str = Field(default="at_least", pattern="^(at_least|at_most)$")
    # Période saisonnière optionnelle (ex. 7=hebdo, 12=mensuel). Utilisée si la série
    # couvre ≥ 2 périodes, sinon Holt linéaire. None = pas de saisonnalité.
    seasonal_period: int | None = Field(default=None, ge=2, le=365)


class KpiForecastPointResponse(BaseModel):
    step: int
    value: float
    low: float
    high: float


class KpiForecastResponse(BaseModel):
    n: int
    slope: float
    intercept: float
    residual_sigma: float
    r2: float
    horizon: int
    target: float
    direction: str
    probability: float
    confidence: str
    model: str
    seasonal_period: int
    points: list[KpiForecastPointResponse]

    @classmethod
    def from_domain(cls, f: KpiForecast) -> "KpiForecastResponse":
        return cls(
            n=f.n, slope=f.slope, intercept=f.intercept, residual_sigma=f.residual_sigma,
            r2=f.r2, horizon=f.horizon, target=f.target, direction=f.direction,
            probability=f.probability, confidence=f.confidence,
            model=f.model, seasonal_period=f.seasonal_period,
            points=[KpiForecastPointResponse(step=p.step, value=p.value, low=p.low, high=p.high)
                    for p in f.points],
        )


# ---- Supplier risk --------------------------------------------------------------

class SupplierRiskRequest(BaseModel):
    # Features optionnelles — voir domain.service.supplier_scoring pour la liste.
    features: dict[str, float] = Field(..., min_length=1, max_length=20)


class RiskDriverResponse(BaseModel):
    feature: str
    value: float
    weight: float
    contribution: float


class SupplierRiskResponse(BaseModel):
    score: float
    level: str
    drivers: list[RiskDriverResponse]

    @classmethod
    def from_domain(cls, s: SupplierRiskScore) -> "SupplierRiskResponse":
        return cls(
            score=s.score, level=s.level,
            drivers=[RiskDriverResponse(feature=d.feature, value=d.value,
                                        weight=d.weight, contribution=d.contribution)
                     for d in s.drivers],
        )


# ---- NC clustering --------------------------------------------------------------

class NcClusterRequest(BaseModel):
    texts: list[str] = Field(..., min_length=2, max_length=2000)
    threshold: float = Field(default=0.35, gt=0.0, lt=1.0)
    # Taille minimale du voisinage d'un point-cœur DBSCAN (densité).
    min_samples: int = Field(default=2, ge=2, le=100)


class NcClusterResponse(BaseModel):
    cluster_id: int
    indices: list[int]
    size: int
    top_terms: list[str]


class NcClusteringResponse(BaseModel):
    n: int
    clustered_ratio: float
    method: str
    clusters: list[NcClusterResponse]
    noise_indices: list[int]

    @classmethod
    def from_domain(cls, r: NcClusteringResult) -> "NcClusteringResponse":
        return cls(
            n=r.n, clustered_ratio=round(r.clustered_ratio, 3), method=r.method,
            clusters=[NcClusterResponse(cluster_id=c.cluster_id, indices=c.indices,
                                        size=c.size, top_terms=c.top_terms)
                      for c in r.clusters],
            noise_indices=r.noise_indices,
        )
