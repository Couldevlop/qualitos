"""Le client passé à la base avant chaque requête NLQ (ADR 0086)."""
from __future__ import annotations

from contextlib import contextmanager

import pytest

from domain.model.errors import ProviderUnavailableError
from domain.model.nlq import GeneratedSql
from infrastructure.nlq import jdbc_readonly_executor as mod

TENANT = "00000000-0000-0000-0000-00000000000a"


def _sql(params):
    return GeneratedSql(
        sql="SELECT 1 FROM nonconformities WHERE tenant_id = %(tenant_id)s",
        parameters=params,
        tables_used=("nonconformities",),
        functions_used=(),
        tenant_filter_applied=True,
    )


class _Cursor:
    def __init__(self, log):
        self.log = log

    def execute(self, sql, params=None):
        self.log.append((sql, params))

    def fetchmany(self, n):
        return [{"un": 1}]

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


class _Conn:
    def __init__(self, log):
        self.log = log

    def cursor(self, row_factory=None):
        return _Cursor(self.log)

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


def test_le_client_est_pose_dans_la_transaction_avant_la_requete(monkeypatch):
    log = []

    class _Psycopg:
        @staticmethod
        def connect(dsn, autocommit):
            return _Conn(log)

    monkeypatch.setattr(mod, "psycopg", _Psycopg)
    monkeypatch.setattr(mod, "dict_row", object())
    rows = mod.JdbcReadOnlyExecutor(dsn="postgresql://x").execute(_sql({"tenant_id": TENANT}))
    assert rows == [{"un": 1}]
    statements = [s for s, _ in log]
    pose = statements.index("SELECT set_config('app.tenant_id', %s, true)")
    assert log[pose][1] == (TENANT,)
    assert pose > statements.index("SET TRANSACTION READ ONLY")
    assert pose < len(statements) - 1  # avant la requête générée


@pytest.mark.parametrize("params", [{}, {"tenant_id": ""}, {"tenant_id": "  "}, None])
def test_sans_client_rien_ne_part(params, monkeypatch):
    monkeypatch.setattr(mod, "psycopg", object())
    with pytest.raises(ProviderUnavailableError):
        mod.JdbcReadOnlyExecutor(dsn="postgresql://x").execute(_sql(params))
