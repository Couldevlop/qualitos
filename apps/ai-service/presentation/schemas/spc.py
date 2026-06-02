"""Pydantic schemas for the SPC anomaly-detection endpoint."""
from __future__ import annotations

from pydantic import BaseModel, Field, model_validator

from domain.model.spc import SpcAnalysis


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
