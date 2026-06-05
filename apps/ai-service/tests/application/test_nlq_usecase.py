"""NlqAskUseCase orchestration tests."""
from __future__ import annotations

import pytest

from application.usecase.nlq_ask import NlqAskRequest, NlqAskUseCase
from domain.model.completion import (
    CompletionRequest,
    CompletionResponse,
    Confidence,
    ProviderName,
)
from domain.model.errors import UnsafeSqlError
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter, PiiScanResult
from domain.port.prompt_audit_logger import PromptAuditLogger
from domain.port.prompt_injection_filter import (
    InjectionScanResult,
    PromptInjectionFilter,
)
from infrastructure.nlq import InMemoryReadOnlyExecutor, SqlglotValidator


class _SqlProvider(AIProvider):
    """Returns canned SQL — what an LLM would have produced."""

    def __init__(self, sql: str) -> None:
        self._sql = sql

    def name(self) -> ProviderName:
        return ProviderName.OLLAMA

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        return CompletionResponse(
            text=self._sql,
            provider=ProviderName.OLLAMA,
            confidence=Confidence(0.7, "heuristic"),
            tokens_used=5,
            latency_ms=2,
        )


class _NoopPii(PiiFilter):
    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        return PiiScanResult(redacted_text=text, findings=(), had_pii=False)


class _NoopInjection(PromptInjectionFilter):
    def scan(self, user_prompt: str) -> InjectionScanResult:
        return InjectionScanResult(suspicious=False, score=0.0, rationale="fake")


class _NoopAudit(PromptAuditLogger):
    def __init__(self) -> None:
        self.entries = []

    def log(self, entry):
        self.entries.append(entry)


def test_nlq_returns_chart_and_narrative(user_context):
    fixtures = {
        "pdca_cycles": [
            {"site": "A", "cycles": 12},
            {"site": "B", "cycles": 7},
        ]
    }
    provider = _SqlProvider(
        "SELECT site, count(*) AS cycles FROM pdca_cycles "
        "WHERE tenant_id = :tenant_id GROUP BY site"
    )
    uc = NlqAskUseCase(
        providers={ProviderName.OLLAMA: provider},
        sql_validator=SqlglotValidator(),
        sql_executor=InMemoryReadOnlyExecutor(fixtures),
        pii_filter=_NoopPii(),
        injection_filter=_NoopInjection(),
        audit_logger=_NoopAudit(),
    )
    answer = uc.execute(user_context, NlqAskRequest(question="How many cycles per site?"))
    assert answer.row_count == 2
    assert answer.chart.chart_type == "bar"
    assert answer.sql.tenant_filter_applied
    assert "narrative" in answer.narrative.lower() or "question" in answer.narrative.lower()


def test_nlq_rejects_malicious_llm_output(user_context):
    provider = _SqlProvider("DROP TABLE pdca_cycles")
    audit = _NoopAudit()
    uc = NlqAskUseCase(
        providers={ProviderName.OLLAMA: provider},
        sql_validator=SqlglotValidator(),
        sql_executor=InMemoryReadOnlyExecutor({}),
        pii_filter=_NoopPii(),
        injection_filter=_NoopInjection(),
        audit_logger=audit,
    )
    with pytest.raises(UnsafeSqlError):
        uc.execute(user_context, NlqAskRequest(question="drop the table"))
    # rejection audited (LLM07 / A09)
    assert any(e.operation == "nlq.ask.rejected" for e in audit.entries)


def test_nlq_auto_injects_tenant_filter(user_context):
    fixtures = {"capa_cases": [{"id": "x", "priority": "high"}]}
    provider = _SqlProvider("SELECT id, priority FROM capa_cases WHERE priority = 'high'")
    uc = NlqAskUseCase(
        providers={ProviderName.OLLAMA: provider},
        sql_validator=SqlglotValidator(),
        sql_executor=InMemoryReadOnlyExecutor(fixtures),
        pii_filter=_NoopPii(),
        injection_filter=_NoopInjection(),
        audit_logger=_NoopAudit(),
    )
    answer = uc.execute(user_context, NlqAskRequest(question="urgent capas"))
    assert "tenant_id" in answer.sql.sql.lower()
