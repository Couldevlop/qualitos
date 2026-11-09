"""Pydantic schemas for the SPC anomaly-detection endpoint."""
from __future__ import annotations

from pydantic import BaseModel, Field, model_validator

from domain.model.spc import SpcAnalysis, SpcBreachForecast


class SpcAnalyzeRequest(BaseModel):
    values: list[float] = Field(..., min_length=1, max_length=10000)
    # Optional known process baseline. Both must be set together; sigma > 0.
    center: float | None = None
    sigma: float | None = Field(default=None, gt=0)

    @model_validator(mode="after")
    def _baseline_is_consistent(self) -> "SpcAnalyzeRequest":
        if (self.center is None) != (self.sigma is None):
            raise ValueError("center and sigma must be provided together")
        return self


class SpcLimitsResponse(BaseModel):
    center_line: float
    sigma: float
    ucl: float
    lcl: float
    estimated: bool


class SpcViolationResponse(BaseModel):
    rule: str
    title: str
    description: str
    point_indices: list[int]
    severity: str


class SpcAnalyzeResponse(BaseModel):
    n: int
    out_of_control: bool
    limits: SpcLimitsResponse
    violations: list[SpcViolationResponse]

    @classmethod
    def from_domain(cls, a: SpcAnalysis) -> "SpcAnalyzeResponse":
        return cls(
            n=a.n,
            out_of_control=a.out_of_control,
            limits=SpcLimitsResponse(
                center_line=a.limits.center_line,
                sigma=a.limits.sigma,
                ucl=a.limits.ucl,
                lcl=a.limits.lcl,
                estimated=a.limits.estimated,
            ),
            violations=[
                SpcViolationResponse(
                    rule=v.rule,
                    title=v.title,
                    description=v.description,
                    point_indices=v.point_indices,
                    severity=v.severity,
                )
                for v in a.violations
            ],
        )


class SpcForecastRequest(BaseModel):
    """Quand le procede sortira-t-il de ses limites ?

    Memes bornes que l'analyse (anti-DoS) ; quatre points minimum, puisqu'un
    ajustement de Holt sur trois points ne dit rien d'une tendance.
    """

    values: list[float] = Field(..., min_length=4, max_length=10000)
    horizon: int = Field(default=6, ge=1, le=60)
    center: float | None = None
    sigma: float | None = Field(default=None, gt=0)
    seasonal_period: int | None = Field(default=None, ge=2, le=365)
    threshold: float = Field(default=0.5, gt=0.0, le=1.0)

    @model_validator(mode="after")
    def _baseline_is_consistent(self) -> "SpcForecastRequest":
        if (self.center is None) != (self.sigma is None):
            raise ValueError("center and sigma must be provided together")
        return self


class SpcBreachPointResponse(BaseModel):
    step: int
    value: float
    low: float
    high: float
    p_above_ucl: float
    p_below_lcl: float
    p_breach: float


class SpcForecastResponse(BaseModel):
    n: int
    horizon: int
    threshold: float
    will_breach: bool
    first_breach_step: int | None
    side: str | None
    trend: float
    residual_sigma: float
    r2: float
    confidence: str
    model: str
    limits: SpcLimitsResponse
    points: list[SpcBreachPointResponse]

    @classmethod
    def from_domain(cls, f: SpcBreachForecast) -> "SpcForecastResponse":
        return cls(
            n=f.n,
            horizon=f.horizon,
            threshold=f.threshold,
            will_breach=f.will_breach,
            first_breach_step=f.first_breach_step,
            side=f.side,
            trend=f.trend,
            residual_sigma=f.residual_sigma,
            r2=f.r2,
            confidence=f.confidence,
            model=f.model,
            limits=SpcLimitsResponse(
                center_line=f.limits.center_line,
                sigma=f.limits.sigma,
                ucl=f.limits.ucl,
                lcl=f.limits.lcl,
                estimated=f.limits.estimated,
            ),
            points=[
                SpcBreachPointResponse(
                    step=p.step,
                    value=p.value,
                    low=p.low,
                    high=p.high,
                    p_above_ucl=p.p_above_ucl,
                    p_below_lcl=p.p_below_lcl,
                    p_breach=p.p_breach,
                )
                for p in f.points
            ],
        )
