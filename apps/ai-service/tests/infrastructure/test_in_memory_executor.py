"""InMemoryReadOnlyExecutor."""
from __future__ import annotations

from uuid import UUID

from domain.model.nlq import GeneratedSql
from infrastructure.nlq import InMemoryReadOnlyExecutor


def test_returns_fixture_rows():
    executor = InMemoryReadOnlyExecutor({"pdca_cycles": [{"id": "x"}]})
    sql = GeneratedSql(
        sql="SELECT id FROM pdca_cycles WHERE tenant_id = :tenant_id",
        parameters={"tenant_id": str(UUID(int=1))},
        tables_used=("pdca_cycles",),
        functions_used=(),
        tenant_filter_applied=True,
    )
    out = executor.execute(sql)
    assert out == [{"id": "x"}]


def test_returns_empty_for_unknown_table():
    executor = InMemoryReadOnlyExecutor({"pdca_cycles": []})
    sql = GeneratedSql(
        sql="SELECT id FROM audits WHERE tenant_id = :tenant_id",
        parameters={"tenant_id": str(UUID(int=1))},
        tables_used=("audits",),
        functions_used=(),
        tenant_filter_applied=True,
    )
    assert executor.execute(sql) == []
