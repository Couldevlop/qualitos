"""SQL validator port â€” sqlglot adapter lives in infrastructure/nlq/."""
from __future__ import annotations

from abc import ABC, abstractmethod
from uuid import UUID

from domain.model.nlq import GeneratedSql


class SqlValidator(ABC):
    """Validates and rewrites generated SQL to enforce all safety invariants.

    Hard rules enforced (OWASP LLM02 + LLM08 + A03):
      * SELECT only (no INSERT/UPDATE/DELETE/DDL/COPY/LOAD/CALL/EXECUTE)
      * FROM tables âŠ† allow-list
      * tenant_id = :tenant_id MUST be in WHERE (rewriter injects it if missing)
      * no subquery to pg_catalog / information_schema
      * functions âŠ† allow-list (sum, avg, count, min, max, date_trunc, lag, lead, percentile_cont)
      * statement count = 1 (no piggybacked queries)
    """

    @abstractmethod
    def validate_and_rewrite(self, raw_sql: str, tenant_id: UUID) -> GeneratedSql:
        """Parse, validate, rewrite. Raises UnsafeSqlError on violation."""
