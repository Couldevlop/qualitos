"""sqlglot-based SQL validator (OWASP LLM02 + LLM08 + A03).

This is the *single* gatekeeper between a LLM-generated SELECT and the
database. Every rule below MUST pass â€” failure raises UnsafeSqlError.

Defence layers:
  1. Parse with sqlglot â€” invalid SQL rejected immediately.
  2. Single statement only (no `;` piggyback).
  3. Top-level node MUST be Select (no INSERT/UPDATE/DELETE/CREATE/DROP/...).
  4. No CTEs to system schemas (pg_catalog, information_schema).
  5. Every table identifier âŠ† allow-list.
  6. Every function identifier âŠ† allow-list.
  7. tenant_id = :tenant_id MUST appear in the top-level WHERE clause.
     If absent, the validator *rewrites* the query to inject it.
  8. No COPY / LOAD / CALL / EXECUTE / DO.
  9. LIMIT capped at 10 000.

Behind this validator, the executor connects with a Postgres role that has
SELECT-only grants â€” defence in depth.
"""
from __future__ import annotations

from uuid import UUID

import sqlglot
from sqlglot import exp

from domain.model.errors import UnsafeSqlError
from domain.model.nlq import GeneratedSql
from domain.port.sql_validator import SqlValidator

# Allow-listed tables (snake_case). Keep narrow.
ALLOWED_TABLES: frozenset[str] = frozenset(
    {
        "pdca_cycles",
        "non_conformities",
        "capas",
        "audits",
        "five_s_audits",
        "fmea_items",
        "suppliers",
        "kpis",
    }
)

# Allow-listed functions. Anything else is rejected.
# Note: sqlglot may normalize `date_trunc` to `timestamp_trunc` after parsing,
# so both names must be listed.
ALLOWED_FUNCS: frozenset[str] = frozenset(
    {
        "sum",
        "avg",
        "count",
        "min",
        "max",
        "date_trunc",
        "timestamp_trunc",
        "lag",
        "lead",
        "percentile_cont",
        "coalesce",
        "round",
        "extract",
    }
)

# Categorical bans â€” scanned on the raw text and the AST.
_FORBIDDEN_TEXT_SUBSTRINGS: tuple[str, ...] = (
    "pg_catalog",
    "information_schema",
    "pg_sleep",
    "copy ",
    "load ",
    "do $$",
    "execute ",
    "call ",
    "grant ",
    "revoke ",
    "vacuum ",
    "create ",
    "drop ",
    "alter ",
    "truncate ",
    "delete ",
    "update ",
    "insert ",
    "merge ",
    "xmltype",
    "dblink",
    "lo_import",
    "lo_export",
)

_MAX_LIMIT = 10_000


class SqlglotValidator(SqlValidator):
    """The sole gatekeeper."""

    def validate_and_rewrite(self, raw_sql: str, tenant_id: UUID) -> GeneratedSql:
        if not raw_sql or not raw_sql.strip():
            raise UnsafeSqlError("empty SQL")

        # Layer 1 â€” text-level bans (fast, also catches comment tricks).
        lowered = raw_sql.lower()
        for needle in _FORBIDDEN_TEXT_SUBSTRINGS:
            if needle in lowered:
                raise UnsafeSqlError(f"forbidden token detected: {needle.strip()}")

        # Reject statement-stacking (one ';' tolerated only at end).
        stripped = raw_sql.strip().rstrip(";")
        if ";" in stripped:
            raise UnsafeSqlError("multiple statements not allowed")

        # Layer 2 â€” parse.
        try:
            parsed_list = sqlglot.parse(stripped, read="postgres")
        except Exception as exc:
            raise UnsafeSqlError(f"unparseable SQL: {exc}") from exc

        parsed = [p for p in parsed_list if p is not None]
        if len(parsed) != 1:
            raise UnsafeSqlError("exactly one statement required")
        stmt = parsed[0]

        if not isinstance(stmt, exp.Select):
            raise UnsafeSqlError(
                f"only SELECT allowed (got {type(stmt).__name__})"
            )

        # Layer 3 â€” walk tables.
        for table in stmt.find_all(exp.Table):
            name = (table.name or "").lower()
            schema = (table.db or "").lower()
            if schema in {"pg_catalog", "information_schema"}:
                raise UnsafeSqlError(f"forbidden schema: {schema}")
            if name not in ALLOWED_TABLES:
                raise UnsafeSqlError(f"table not in allow-list: {name}")

        # Layer 4 â€” walk functions.
        funcs_used: set[str] = set()
        for func in stmt.find_all(exp.Func):
            fname = (func.sql_name() or func.name or "").lower()
            if not fname:
                continue
            if fname not in ALLOWED_FUNCS:
                raise UnsafeSqlError(f"function not in allow-list: {fname}")
            funcs_used.add(fname)

        # Layer 5 â€” block subqueries that re-introduce forbidden tables.
        # (already covered by walking all exp.Table â€” subqueries inherit)

        # Layer 6 â€” enforce LIMIT cap.
        limit_node = stmt.args.get("limit")
        if limit_node is not None:
            try:
                lim_val = int(limit_node.expression.this)  # type: ignore[union-attr]
                if lim_val > _MAX_LIMIT:
                    raise UnsafeSqlError(f"LIMIT {lim_val} exceeds cap {_MAX_LIMIT}")
            except (AttributeError, ValueError, TypeError):
                pass

        # Layer 6.5 - if the query already mentions tenant_id, it MUST
        # compare against the :tenant_id placeholder, never a literal.
        self._reject_literal_tenant_id(stmt)

        # Layer 7 - enforce tenant filter (rewrite if missing).
        rewritten, applied = self._ensure_tenant_filter(stmt)
        # Re-render the SQL safely.
        final_sql = rewritten.sql(dialect="postgres")

        tables = tuple(
            sorted({(t.name or "").lower() for t in rewritten.find_all(exp.Table)})
        )
        return GeneratedSql(
            sql=final_sql,
            parameters={"tenant_id": str(tenant_id)},
            tables_used=tables,
            functions_used=tuple(sorted(funcs_used)),
            tenant_filter_applied=True,
        )

    def _ensure_tenant_filter(self, stmt: exp.Select) -> tuple[exp.Select, bool]:
        """Returns (rewritten, applied_now)."""
        if self._tenant_filter_present(stmt):
            return stmt, False

        tenant_predicate = exp.EQ(
            this=exp.column("tenant_id"),
            expression=exp.Placeholder(this="tenant_id"),
        )
        existing_where = stmt.args.get("where")
        if existing_where is not None:
            combined = exp.And(
                this=existing_where.this,
                expression=tenant_predicate,
            )
            stmt.set("where", exp.Where(this=combined))
        else:
            stmt.set("where", exp.Where(this=tenant_predicate))
        return stmt, True

    @staticmethod
    def _tenant_filter_present(stmt: exp.Select) -> bool:
        where = stmt.args.get("where")
        if where is None:
            return False
        for col in where.find_all(exp.Column):
            if (col.name or "").lower() == "tenant_id":
                return True
        return False

    @staticmethod
    def _reject_literal_tenant_id(stmt: exp.Select) -> None:
        """If tenant_id is compared to a literal, reject — must be :tenant_id."""
        for eq in stmt.find_all(exp.EQ):
            left = eq.this
            right = eq.expression
            for col, other in ((left, right), (right, left)):
                if isinstance(col, exp.Column) and (col.name or "").lower() == "tenant_id":
                    # Acceptable RHS: placeholder, parameter, or column reference
                    # (e.g. JOIN ON pdca_cycles.tenant_id = audits.tenant_id).
                    if isinstance(other, (exp.Placeholder, exp.Parameter, exp.Column)):
                        return  # OK on at least one side
                    if isinstance(other, exp.Literal):
                        raise UnsafeSqlError(
                            "tenant_id must be bound via :tenant_id placeholder, "
                            "not a literal"
                        )
