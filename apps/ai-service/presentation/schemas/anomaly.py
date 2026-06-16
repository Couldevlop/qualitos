"""Schémas Pydantic de l'endpoint de détection d'anomalies multivariées."""
from __future__ import annotations

from pydantic import BaseModel, Field, model_validator

from domain.model.anomaly import SUPPORTED_METHODS, AnomalyExplanation, AnomalyResult

# Bornes alignées sur le use case (anti-DoS, OWASP LLM04).
_MAX_SAMPLES = 50_000
_MAX_FEATURES = 200


class AnomalyDetectRequest(BaseModel):
    # Matrice échantillons × features. Chaque ligne = un vecteur de mesures.
    samples: list[list[float]] = Field(..., min_length=1, max_length=_MAX_SAMPLES)
    method: str = Field(default="isolation_forest")
    contamination: float = Field(default=0.1, gt=0.0, le=0.5)
    # Seuil explicite sur le score (sinon : quantile de contamination).
    threshold: float | None = None
    seed: int = Field(default=42, ge=0)
    # Hyperparamètres Isolation Forest.
    n_trees: int = Field(default=100, ge=1, le=1000)
    sample_size: int = Field(default=256, ge=1, le=_MAX_SAMPLES)
    # Reconstruction (ACP) : nombre de composantes (None = auto ~95 % variance).
    n_components: int | None = Field(default=None, ge=1, le=_MAX_FEATURES)

    @model_validator(mode="after")
    def _validate(self) -> "AnomalyDetectRequest":
        if self.method not in SUPPORTED_METHODS:
            raise ValueError(
                f"method must be one of {sorted(SUPPORTED_METHODS)}"
            )
        width = len(self.samples[0])
        if width == 0:
            raise ValueError("sample rows must have at least one feature")
        if width > _MAX_FEATURES:
            raise ValueError(f"too many features (max {_MAX_FEATURES})")
        if any(len(row) != width for row in self.samples):
            raise ValueError("all sample rows must have the same length")
        return self


class AnomalyExplainRequest(BaseModel):
    # Matrice échantillons × features + index de l'échantillon à expliquer.
    samples: list[list[float]] = Field(..., min_length=1, max_length=_MAX_SAMPLES)
    index: int = Field(..., ge=0)
    seed: int = Field(default=42, ge=0)
    n_trees: int = Field(default=100, ge=1, le=1000)
    sample_size: int = Field(default=256, ge=1, le=_MAX_SAMPLES)

    @model_validator(mode="after")
    def _validate(self) -> "AnomalyExplainRequest":
        width = len(self.samples[0])
        if width == 0:
            raise ValueError("sample rows must have at least one feature")
        if width > _MAX_FEATURES:
            raise ValueError(f"too many features (max {_MAX_FEATURES})")
        if any(len(row) != width for row in self.samples):
            raise ValueError("all sample rows must have the same length")
        if self.index >= len(self.samples):
            raise ValueError("index out of range")
        return self


class FeatureContributionResponse(BaseModel):
    feature: int
    value: float
    contribution: float


class AnomalyExplainResponse(BaseModel):
    index: int
    method: str
    score: float
    base_value: float
    contributions: list[FeatureContributionResponse]

    @classmethod
    def from_domain(cls, e: AnomalyExplanation) -> "AnomalyExplainResponse":
        return cls(
            index=e.index, method=e.method, score=e.score, base_value=e.base_value,
            contributions=[
                FeatureContributionResponse(
                    feature=c.feature, value=c.value, contribution=c.contribution)
                for c in e.contributions
            ],
        )


class AnomalyPointResponse(BaseModel):
    index: int
    score: float
    is_anomaly: bool
    top_feature: int | None = None


class AnomalyDetectResponse(BaseModel):
    n: int
    n_features: int
    method: str
    contamination: float
    threshold: float
    anomaly_count: int
    has_anomalies: bool
    points: list[AnomalyPointResponse]

    @classmethod
    def from_domain(cls, r: AnomalyResult) -> "AnomalyDetectResponse":
        return cls(
            n=r.n,
            n_features=r.n_features,
            method=r.method,
            contamination=r.contamination,
            threshold=r.threshold,
            anomaly_count=r.anomaly_count,
            has_anomalies=r.has_anomalies,
            points=[
                AnomalyPointResponse(
                    index=p.index,
                    score=p.score,
                    is_anomaly=p.is_anomaly,
                    top_feature=p.top_feature,
                )
                for p in r.points
            ],
        )
