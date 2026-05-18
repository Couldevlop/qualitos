# ADR 0009 — Natural Language Query safety model (P5)

Status: Accepted (P5 sprint-1, 2026-05-17)

## Context

CLAUDE.md §7.3 and §12.1 list NLQ as a flagship dashboards feature: a user
asks "How many CAPAs in March in plant A?" and gets a chart + narrative.
Naively wiring an LLM to text-to-SQL is the textbook **LLM02 Insecure Output**
and **LLM08 Excessive Agency** vulnerability — a single jailbroken prompt
could `DROP TABLE pdca_cycles`, exfiltrate another tenant's data, or read
`pg_catalog`. This ADR locks the defence model.

## Decision: defence in depth

1. **PromptInjectionFilter** before any LLM call — pattern bank, score ≥ 0.6
   rejects (OWASP LLM01).
2. **Presidio PII redactor** scrubs user input *and* model output (LLM06).
3. **Constrained system prompt** — generator is told `SELECT only`, given a
   narrow allow-listed schema, told to use `tenant_id = :tenant_id`.
4. **SqlglotValidator** (mandatory gatekeeper) parses the generated SQL and
   enforces:
   - Single statement (no `;`-piggyback)
   - Top-level node is `exp.Select` — INSERT/UPDATE/DELETE/DDL rejected
   - Tables ⊆ allow-list `{pdca_cycles, non_conformities, capas, audits,
     five_s_audits, fmea_items, suppliers, kpis}`
   - Functions ⊆ allow-list (`sum, avg, count, min, max, date_trunc,
     timestamp_trunc, lag, lead, percentile_cont, coalesce, round, extract`)
   - `tenant_id` reference must compare to `:tenant_id` placeholder; literal
     comparisons are rejected (cross-tenant attempt)
   - If no tenant filter is present, validator **rewrites** the AST to inject
     `AND tenant_id = :tenant_id`
   - Forbidden text bans: `pg_catalog`, `information_schema`, `pg_sleep`,
     `copy`, `load`, `do $$`, `execute`, `call`, `dblink`, `lo_import`,
     `lo_export`, `xmltype`, all DDL/DML keywords except SELECT
   - LIMIT cap 10 000
5. **Read-only Postgres role** (`qualitos_nlq_ro`) with `SELECT` grants on
   the eight allow-listed tables. Per-session `SET TRANSACTION READ ONLY`
   and `statement_timeout = 5000ms`.
6. **PromptAuditLogger** — every NLQ call (including rejected ones) is
   recorded as a redacted JSON line (OWASP A09).

## Adversarial test suite

`tests/nlq_adversarial/test_sqlglot_validator_adversarial.py` ships **35
hostile cases**: statement stacking, DROP/DELETE/UPDATE/INSERT/CREATE/ALTER/
TRUNCATE/GRANT, COPY-from-file, LOAD, DO blocks, CALL, pg_sleep DoS,
information_schema enumeration, dblink, lo_import/export, UNION
pg_catalog, comment-trick, unknown tables, explicit cross-tenant id,
time-based blind SQLi, XMLTYPE/XXE, current_setting exfil, MERGE, VACUUM,
forbidden function (array_agg), EXECUTE, schema-qualified pg_catalog,
LIMIT abuse, empty SQL.

All 35 must raise `UnsafeSqlError`. Three benign cases must pass and have
the tenant filter applied/rewritten.

## Consequences

- A jailbroken LLM cannot escape — the validator is the contract, not the
  LLM. The schema allow-list is the trust boundary.
- New tables added to the catalog must be explicitly registered in
  `ALLOWED_TABLES` — making "expose a new dataset to NLQ" a deliberate
  decision reviewed by security.
- The validator's behaviour evolves with the threat model; new attacks
  drive new test cases first (TDD on the attack).

## Alternatives rejected

- "LLM-only filtering" — banned. LLMs cannot be trusted as a security
  boundary (LLM01 supremacy).
- "Run generated SQL in a sandboxed DB" — costly, still leaks the schema.
  A static AST validator + read-only role is cheaper and tighter.

## References

- CLAUDE.md §7.3, §11.2 (LLM02, LLM07, LLM08), §18.2.
- OWASP Top 10 for LLM Applications 2025.
- Files: `apps/ai-service/infrastructure/nlq/sqlglot_validator.py`,
  `apps/ai-service/application/usecase/nlq_ask.py`,
  `apps/ai-service/tests/nlq_adversarial/`.
