"""Use case: Natural Language Query (text-to-SQL).

Pipeline (defence-in-depth, every step is mandatory):

  1) PromptInjectionFilter on the question (LLM01)
  2) PII redaction (LLM06)
  3) LLM generates SQL (constrained system prompt, schema-aware)
  4) SqlValidator: parse with sqlglot, enforce SELECT-only, allow-listed tables,
     allow-listed functions, inject tenant_id filter if missing (LLM02 + LLM08)
  5) ReadOnlySqlExecutor runs with a separate Postgres role (SELECT only)
  6) ChartInferrer picks an ECharts spec
  7) NarrativeBuilder produces a deterministic explanation (LLM optional)
  8) PromptAuditLogger records everything redacted (A09)
"""
from __future__ import annotations

from dataclasses import dataclass

from domain.model.completion import CompletionRequest, ProviderName
from domain.model.errors import (
    PromptInjectionError,
    UnsafeSqlError,
)
from domain.model.nlq import NlqAnswer, NlqQuestion
from domain.model.tenant import UserContext
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import (
    PromptAuditEntry,
    PromptAuditLogger,
)
from domain.port.prompt_injection_filter import PromptInjectionFilter
from domain.port.sql_executor import ReadOnlySqlExecutor
from domain.port.sql_validator import SqlValidator
from domain.service.chart_inferrer import ChartInferrer
from domain.service.narrative_builder import NarrativeBuilder

# Schema hint kept narrow on purpose — only KPI-relevant tables, aligned on the
# real qualitos_quality schema. Enum values are given to guide WHERE clauses.
_SCHEMA_HINT = """
Tables you may query (snake_case columns; tenant_id is mandatory in WHERE):

  pdca_cycles(id, tenant_id, title, status, owner_id, created_at, completed_at)
     status in (PLAN, DO, CHECK, ACT, COMPLETED)
  capa_cases(id, tenant_id, title, type, criticity, status, source_type, due_date, resolved_at, closed_at, created_at)
     type in (CORRECTIVE, PREVENTIVE); criticity in (LOW, MEDIUM, HIGH, CRITICAL);
     status in (OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED);
     source_type in (NON_CONFORMITY, AUDIT, COMPLAINT, INTERNAL, IOT_ALERT, OTHER)
  fives_audits(id, tenant_id, zone, status, overall_score, scheduled_at, completed_at, created_at)
  ishikawa_diagrams(id, tenant_id, problem_statement, mode, status, created_at)
  fmea_items(id, tenant_id, rpn, severity, occurrence, detection, created_at)
  suppliers(id, tenant_id, name, supplier_type, status, score, created_at)
  kpi_definitions(id, tenant_id, code, name, category, unit, target_value, status)
  kpi_measurements(id, tenant_id, kpi_id, period_start, period_end, value)

Allowed aggregations: sum, avg, count, min, max, date_trunc, percentile_cont.
A non-conformity is a capa_cases row with source_type = 'NON_CONFORMITY'.
""".strip()

_SQL_SYSTEM_PROMPT = (
    "You are a strict PostgreSQL SQL generator. "
    "Output ONLY a single SELECT statement, no comments, no semicolons, no DDL. "
    "Always include `tenant_id = :tenant_id` in the WHERE clause. "
    "Use only the allowed tables and aggregations.\n\n" + _SCHEMA_HINT
)


@dataclass(frozen=True, slots=True)
class NlqAskRequest:
    question: str
    provider: ProviderName = ProviderName.OLLAMA
    max_rows: int = 1000


class NlqAskUseCase:
    """Generate, validate, execute, explain."""

    def __init__(
        self,
        providers: dict[ProviderName, AIProvider],
        sql_validator: SqlValidator,
        sql_executor: ReadOnlySqlExecutor,
        pii_filter: PiiFilter,
        injection_filter: PromptInjectionFilter,
        audit_logger: PromptAuditLogger,
    ) -> None:
        self._providers = providers
        self._validator = sql_validator
        self._executor = sql_executor
        self._pii = pii_filter
        self._injection = injection_filter
        self._audit = audit_logger

    def execute(self, user: UserContext, req: NlqAskRequest) -> NlqAnswer:
        injection = self._injection.scan(req.question)
        if injection.suspicious:
            raise PromptInjectionError(
                f"Injection suspected (score={injection.score:.2f})"
            )

        sanitized_q = self._pii.redact(req.question).redacted_text
        question = NlqQuestion(
            question=sanitized_q,
            tenant=user.tenant,
            user_id=user.user_id,
            correlation_id=user.correlation_id,
        )

        provider = self._providers[req.provider]
        completion = provider.complete(
            CompletionRequest(
                system_prompt=_SQL_SYSTEM_PROMPT,
                user_prompt=f"NL question: {sanitized_q}\n\nReturn one SELECT statement.",
                provider=req.provider,
                max_tokens=512,
                temperature=0.0,
            )
        )

        raw_sql = _strip_codefences(completion.text)

        # CRITICAL: validate + rewrite. Raises UnsafeSqlError on violation.
        try:
            validated = self._validator.validate_and_rewrite(
                raw_sql, user.tenant.tenant_id
            )
        except UnsafeSqlError:
            self._audit.log(
                PromptAuditEntry(
                    tenant_id=user.tenant.tenant_id,
                    user_id=user.user_id,
                    correlation_id=user.correlation_id,
                    operation="nlq.ask.rejected",
                    provider=req.provider.value,
                    redacted_prompt=sanitized_q[:500],
                    redacted_response=raw_sql[:500],
                    latency_ms=completion.latency_ms,
                    tokens_used=completion.tokens_used,
                )
            )
            raise

        rows = self._executor.execute(validated, max_rows=req.max_rows)
        chart = ChartInferrer.infer(rows, question.question)
        narrative = NarrativeBuilder.build(question.question, rows)

        # LLM-confidence is a coarse heuristic: 0 rows â†’ low, 1+ rows â†’ high.
        conf = 0.85 if rows else 0.35

        self._audit.log(
            PromptAuditEntry(
                tenant_id=user.tenant.tenant_id,
                user_id=user.user_id,
                correlation_id=user.correlation_id,
                operation="nlq.ask",
                provider=req.provider.value,
                redacted_prompt=sanitized_q[:500],
                redacted_response=validated.sql[:500],
                latency_ms=completion.latency_ms,
                tokens_used=completion.tokens_used,
            )
        )

        return NlqAnswer(
            question=question.question,
            sql=validated,
            rows=tuple(rows),
            chart=chart,
            narrative=narrative,
            confidence=conf,
            row_count=len(rows),
        )


def _strip_codefences(text: str) -> str:
    t = text.strip()
    if t.startswith("```"):
        lines = t.splitlines()
        # drop first ``` and any 'sql' tag
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        t = "\n".join(lines).strip()
    return t.rstrip(";").strip()
