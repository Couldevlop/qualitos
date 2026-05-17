"""Natural-Language-Query domain objects (text-to-SQL).

CRITICAL: see infrastructure/nlq/sqlglot_validator.py — every generated SQL is
validated server-side before execution. The domain entity GeneratedSql only
carries the validated SQL string + safety flags.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any
from uuid import UUID

from .tenant import TenantContext


@dataclass(frozen=True, slots=True)
class NlqQuestion:
    """An NL question scoped to a tenant."""

    question: str
    tenant: TenantContext
    user_id: UUID
    correlation_id: str

    def __post_init__(self) -> None:
        if not self.question or not self.question.strip():
            raise ValueError("question required")
        if len(self.question) > 1000:
            raise ValueError("question too long (>1000 chars)")


@dataclass(frozen=True, slots=True)
class GeneratedSql:
    """A validated, tenant-scoped, READ-ONLY SQL query.

    Invariant: tenant_id is guaranteed to appear in the WHERE clause.
    Invariant: this is a SELECT only — no INSERT/UPDATE/DELETE/DDL/COPY.
    Invariant: only allow-listed tables and functions are referenced.
    """

    sql: str
    parameters: dict[str, Any]
    tables_used: tuple[str, ...]
    functions_used: tuple[str, ...]
    tenant_filter_applied: bool

    def __post_init__(self) -> None:
        if not self.sql.strip():
            raise ValueError("sql required")
        if not self.tenant_filter_applied:
            raise ValueError("tenant filter MUST be applied before instantiation")


@dataclass(frozen=True, slots=True)
class ChartSpec:
    """A minimal ECharts option spec the frontend can render directly."""

    chart_type: str  # 'bar', 'line', 'pie', 'table', 'kpi'
    title: str
    series: tuple[dict[str, Any], ...]
    x_axis: tuple[str, ...] = field(default_factory=tuple)

    def __post_init__(self) -> None:
        if self.chart_type not in {"bar", "line", "pie", "table", "kpi"}:
            raise ValueError(f"unsupported chart_type: {self.chart_type}")


@dataclass(frozen=True, slots=True)
class NlqAnswer:
    """End-to-end NLQ answer: SQL + data + chart + narrative explanation."""

    question: str
    sql: GeneratedSql
    rows: tuple[dict[str, Any], ...]
    chart: ChartSpec
    narrative: str
    confidence: float
    row_count: int

    def __post_init__(self) -> None:
        if not 0.0 <= self.confidence <= 1.0:
            raise ValueError("confidence must be in [0, 1]")
