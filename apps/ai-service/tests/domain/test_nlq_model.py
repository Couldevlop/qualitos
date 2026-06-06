"""Domain invariants for NLQ entities."""
from __future__ import annotations

import pytest

from domain.model.nlq import ChartSpec, GeneratedSql, NlqAnswer, NlqQuestion
from domain.model.tenant import TenantContext


def test_generated_sql_requires_tenant_filter():
    from uuid import UUID

    with pytest.raises(ValueError, match="tenant filter"):
        GeneratedSql(
            sql="SELECT 1",
            parameters={"tenant_id": str(UUID(int=0))},
            tables_used=(),
            functions_used=(),
            tenant_filter_applied=False,
        )


def test_chart_spec_rejects_unknown_type():
    with pytest.raises(ValueError):
        ChartSpec(chart_type="sankey-weird", title="t", series=())


def test_nlq_question_length_cap(tenant_id):
    from uuid import uuid4

    tenant = TenantContext(tenant_id=tenant_id, issuer="test")
    with pytest.raises(ValueError):
        NlqQuestion(
            question="x" * 2000,
            tenant=tenant,
            user_id=uuid4(),
            correlation_id="c",
        )


def test_nlq_answer_confidence_bounds():
    sql = GeneratedSql(
        sql="SELECT 1",
        parameters={},
        tables_used=(),
        functions_used=(),
        tenant_filter_applied=True,
    )
    chart = ChartSpec(chart_type="kpi", title="t", series=({"value": 1, "label": "x"},))
    with pytest.raises(ValueError):
        NlqAnswer(
            question="q",
            sql=sql,
            rows=(),
            chart=chart,
            narrative="n",
            confidence=1.4,
            row_count=0,
        )
