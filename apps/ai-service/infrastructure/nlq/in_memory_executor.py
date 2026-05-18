"""In-memory NLQ executor used by tests and local dev.

Stores a list of dict 'rows' per table; responds with a deterministic
canned answer based on the SQL's first table.
"""
from __future__ import annotations

from typing import Any

from domain.model.nlq import GeneratedSql
from domain.port.sql_executor import ReadOnlySqlExecutor


class InMemoryReadOnlyExecutor(ReadOnlySqlExecutor):
    def __init__(self, fixtures: dict[str, list[dict[str, Any]]] | None = None) -> None:
        self._fixtures = fixtures or {}

    def execute(self, sql: GeneratedSql, max_rows: int = 1000) -> list[dict[str, Any]]:
        for table in sql.tables_used:
            if table in self._fixtures:
                return list(self._fixtures[table])[:max_rows]
        return []
