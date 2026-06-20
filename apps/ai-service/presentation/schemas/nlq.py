"""NLQ API schemas."""
from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName
from presentation.provider_defaults import DEFAULT_PROVIDER


class NlqRequestSchema(BaseModel):
    question: str = Field(..., min_length=1, max_length=1000)
    provider: ProviderName = DEFAULT_PROVIDER
    max_rows: int = Field(default=500, ge=1, le=10_000)


class ChartSpecSchema(BaseModel):
    chart_type: str
    title: str
    x_axis: list[str] = Field(default_factory=list)
    series: list[dict[str, Any]] = Field(default_factory=list)


class GeneratedSqlSchema(BaseModel):
    sql: str
    parameters: dict[str, Any]
    tables_used: list[str]
    functions_used: list[str]
    tenant_filter_applied: bool


class NlqResponseSchema(BaseModel):
    question: str
    sql: GeneratedSqlSchema
    rows: list[dict[str, Any]]
    chart: ChartSpecSchema
    narrative: str
    confidence: float
    row_count: int
