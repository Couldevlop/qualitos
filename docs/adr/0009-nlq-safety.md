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

## Update (2026-05-28) — schema realignment, end-to-end delivery

The safety model above is unchanged; the following supersede the implementation
details, and NLQ is now wired all the way to the SPA.

- **Real schema allow-list.** The table list in the original Decision was
  aspirational and never matched the database. `ALLOWED_TABLES` (and the
  generator's schema hint) now reflect the actual `qualitos_quality` schema:
  `{pdca_cycles, capa_cases, fives_audits, ishikawa_diagrams, fmea_items,
  suppliers, kpi_definitions, kpi_measurements}`. A non-conformity is a
  `capa_cases` row with `source_type = 'NON_CONFORMITY'`. Adding a table to NLQ
  remains a deliberate, security-reviewed change (per "Consequences").
- **Function allow-list extended** with safe, side-effect-free scalars:
  `cast`, `nullif`, `abs` (in addition to the original aggregations). Logical/
  comparison operators (`and`, `or`, `not`, `in`, `like`, `between`, `is`,
  `exists`) that sqlglot sometimes surfaces as `exp.Func` are skipped — they are
  not exfiltrating calls; safety rests on the TABLE allow-list + read-only role.
- **Validator is model-agnostic.** The gatekeeper is the contract regardless of
  which local Ollama model generates the SQL. A weaker model simply produces
  more *rejected* (422) or *non-executable* (503 ProblemDetail) queries — never
  an unsafe one. Verified live: hallucinated JOINs / invented columns fail
  cleanly with the tenant filter still enforced.
- **Engine relay + SPA.** The SPA never calls ai-service directly. New endpoint
  `POST /api/v1/ai/nlq/ask` in api-quality-engine (`NlqController` → `NlqService`
  → `AiGatewayClient`, the gateway of ADR 0014) relays to ai-service. Tenant is
  derived from the validated JWT (`TenantContext`), **never** from the body
  (CLAUDE.md §18.2 #2); `question` (≤500 chars) and `maxRows` (≤1000) are bounded
  to limit LLM-DoS (LLM04). Gateway failures surface as `502` ProblemDetail.
  SPA feature `features/nlq` (route `/nlq`) renders the AI narrative, results,
  confidence, the tenant-filter security badge, and the generated SQL (§12.3
  explicability).
- **Runtime PII filter.** Presidio is excluded from the runtime image (ADR 0013,
  spaCy auto-download breaks the distroless build); a `HeuristicPiiFilter`
  fallback runs instead. Re-enable Presidio in prod by embedding the spaCy model
  at build time.
