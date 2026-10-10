"""Postgres adapter using a SELECT-only role.

Connect with a role that has only SELECT on qualitos_quality schema.
statement_timeout is set to 5s at the session level.
"""
from __future__ import annotations

import os
from typing import Any

try:  # psycopg is optional at test time
    import psycopg
    from psycopg.rows import dict_row
except Exception:  # pragma: no cover - test envs may not have psycopg
    psycopg = None  # type: ignore[assignment]
    dict_row = None  # type: ignore[assignment]

from domain.model.errors import ProviderUnavailableError
from domain.model.nlq import GeneratedSql
from domain.port.sql_executor import ReadOnlySqlExecutor


class JdbcReadOnlyExecutor(ReadOnlySqlExecutor):
    """psycopg3 read-only executor.

    DSN should reference a dedicated user with only SELECT grants on the
    allow-listed tables. statement_timeout is enforced per session.
    """

    def __init__(
        self,
        dsn: str | None = None,
        statement_timeout_ms: int = 5000,
    ) -> None:
        self._dsn = dsn or os.environ.get(
            "NLQ_READONLY_DSN",
            "postgresql://qualitos_nlq_ro:nlq@postgres:5432/qualitos_quality",
        )
        self._stmt_timeout = statement_timeout_ms

    def execute(self, sql: GeneratedSql, max_rows: int = 1000) -> list[dict[str, Any]]:
        if psycopg is None:
            raise ProviderUnavailableError("psycopg not available")
        max_rows = max(1, min(max_rows, 10_000))
        tenant_id = tenant_setting(sql)
        try:
            with psycopg.connect(self._dsn, autocommit=False) as conn:
                with conn.cursor(row_factory=dict_row) as cur:
                    cur.execute(f"SET statement_timeout = {self._stmt_timeout}")
                    # Enforce read-only at the session level too.
                    cur.execute("SET TRANSACTION READ ONLY")
                    # Second rempart, tenu par la base (ADR 0086) : même si le
                    # SQL généré échappait au filtre tenant du validateur, les
                    # politiques de PostgreSQL ne rendraient que ce client.
                    # Local à la transaction : rien ne survit à la requête.
                    cur.execute("SELECT set_config('app.tenant_id', %s, true)", (tenant_id,))
                    cur.execute(sql.sql, sql.parameters)
                    rows = cur.fetchmany(max_rows)
                    return [dict(r) for r in rows]
        except Exception as exc:
            raise ProviderUnavailableError(f"NLQ executor failed: {exc}") from exc


def tenant_setting(sql: GeneratedSql) -> str:
    """Le client de la requête, tel que la base le reçoit (``app.tenant_id``).

    Refus net s'il manque : sans lui, les politiques de la base laisseraient
    voir tous les clients — exactement ce que ce rempart doit empêcher.
    """
    tenant_id = str((sql.parameters or {}).get("tenant_id") or "").strip()
    if not tenant_id:
        raise ProviderUnavailableError("NLQ : requête sans client, exécution refusée")
    return tenant_id
