"""Read-only SQL executor port."""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from domain.model.nlq import GeneratedSql


class ReadOnlySqlExecutor(ABC):
    """Executes a validated, read-only SQL query.

    Adapters MUST connect with a Postgres role that has only SELECT on a
    whitelisted schema. statement_timeout SHOULD be â‰¤ 5s.
    """

    @abstractmethod
    def execute(self, sql: GeneratedSql, max_rows: int = 1000) -> list[dict[str, Any]]:
        """Execute and return rows as dicts (column -> value)."""
