"""MockAuditUseCase orchestration tests (Standards Hub §8.4 onglet 7)."""
from __future__ import annotations

from dataclasses import dataclass, field

import pytest

from application.usecase.mock_audit import (
    MockAuditUseCase,
    MockAuditUseCaseRequest,
)
from domain.model.completion import (
    CompletionRequest,
    CompletionResponse,
    Confidence,
    ProviderName,
)
from domain.model.errors import PromptInjectionError
from domain.model.mock_audit import (
    AuditClause,
    GapCriticality,
    MockAuditSpec,
    ObligationLevel,
    RiskLevel,
)
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter, PiiScanResult
from domain.port.prompt_audit_logger import PromptAuditLogger
from domain.port.prompt_injection_filter import (
    InjectionScanResult,
    PromptInjectionFilter,
)

_VALID_JSON = (
    '{"questions": ['
    '{"clause_code": "8.1", "question": "Comment maîtrisez-vous la production ?", "rationale": "clause critique"},'
    '{"clause_code": "7.1", "question": "Quelles ressources sont allouées ?"}'
    '], "findings": ['
    '{"clause_code": "8.1", "finding": "Aucune preuve documentée fournie."},'
    '{"clause_code": "7.1", "finding": "Couverture partielle."}'
    "]}"
)


@dataclass
class FakeProvider(AIProvider):
    text: str = _VALID_JSON
    requests: list = field(default_factory=list)

    def name(self) -> ProviderName:
        return ProviderName.OLLAMA

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        self.requests.append(request)
        return CompletionResponse(
            text=self.text,
            provider=ProviderName.OLLAMA,
            confidence=Confidence(0.6, "heuristic"),
            tokens_used=42,
            latency_ms=9,
        )


class FakePii(PiiFilter):
    def __init__(self, findings: tuple[str, ...] = ()) -> None:
        self._findings = findings

    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        return PiiScanResult(
            redacted_text=text,
            findings=self._findings,
            had_pii=bool(self._findings),
        )


class FakeInjection(PromptInjectionFilter):
    def __init__(self, suspicious: bool = False) -> None:
        self._suspicious = suspicious

    def scan(self, user_prompt: str) -> InjectionScanResult:
        return InjectionScanResult(
            suspicious=self._suspicious,
            score=0.95 if self._suspicious else 0.0,
            rationale="fake",
        )


class FakeAudit(PromptAuditLogger):
    def __init__(self) -> None:
        self.entries = []

    def log(self, entry):
        self.entries.append(entry)


def _spec() -> MockAuditSpec:
    return MockAuditSpec(
        standard_code="iso-9001",
        standard_name="ISO 9001:2015",
        industry="manufacturing",
        clauses=(
            AuditClause(
                clause_code="8.1", title="Maîtrise opérationnelle",
                obligation=ObligationLevel.MUST, risk=RiskLevel.CRITICAL,
                total_requirements=4, covered_requirements=0,
            ),
            AuditClause(
                clause_code="7.1", title="Ressources",
                obligation=ObligationLevel.MUST, risk=RiskLevel.MEDIUM,
                total_requirements=2, covered_requirements=1,
            ),
            AuditClause(
                clause_code="4.1", title="Contexte",
                obligation=ObligationLevel.SHOULD, risk=RiskLevel.LOW,
                total_requirements=2, covered_requirements=2,
            ),
        ),
    )


def _usecase(provider, pii=None, inj=None, audit=None) -> MockAuditUseCase:
    return MockAuditUseCase(
        providers={ProviderName.OLLAMA: provider},
        pii_filter=pii or FakePii(),
        injection_filter=inj or FakeInjection(),
        audit_logger=audit or FakeAudit(),
    )


def test_requires_at_least_one_provider():
    with pytest.raises(ValueError, match="at least one provider"):
        MockAuditUseCase({}, FakePii(), FakeInjection(), FakeAudit())


def test_happy_path_produces_questions_and_gaps(user_context):
    provider = FakeProvider()
    audit = FakeAudit()
    uc = _usecase(provider, audit=audit)

    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    report = out.report

    # Les questions viennent du LLM, projetées sur les clauses connues.
    assert {q.clause_code for q in report.questions} == {"8.1", "7.1"}
    # Une seule requête LLM (génération globale, sortie JSON).
    assert len(provider.requests) == 1
    # Le prompt présente les clauses à risque + la borne de questions.
    user_prompt = provider.requests[0].user_prompt
    assert "8.1" in user_prompt
    assert "entre 30 et 100" in user_prompt
    # Gap analysis : 8.1 majeur, 7.1 mineur, 4.1 observation.
    assert report.major_count == 1
    assert report.minor_count == 1
    assert report.observation_count == 1
    # Constat IA repris pour 8.1.
    by_code = {g.clause_code: g for g in report.gaps}
    assert by_code["8.1"].finding == "Aucune preuve documentée fournie."
    assert by_code["8.1"].criticality is GapCriticality.MAJOR
    # Readiness = couverture MUST : 1/6 ≈ 16.67.
    assert report.readiness == pytest.approx(16.67, abs=0.01)
    assert report.provider == "ollama"
    assert report.tokens_used == 42
    # Audit journalisé.
    assert len(audit.entries) == 1
    assert audit.entries[0].operation == "standards.mock-audit"


def test_unknown_provider_raises(user_context):
    uc = _usecase(FakeProvider())
    with pytest.raises(ValueError, match="provider not configured"):
        uc.execute(
            user_context,
            MockAuditUseCaseRequest(spec=_spec(), provider=ProviderName.ANTHROPIC),
        )


def test_injection_blocks_before_llm(user_context):
    provider = FakeProvider()
    uc = _usecase(provider, inj=FakeInjection(suspicious=True))
    with pytest.raises(PromptInjectionError):
        uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    assert provider.requests == []


def test_garbage_llm_output_falls_back_to_deterministic(user_context):
    # Sortie LLM inexploitable → 0 question, mais constats déterministes par clause.
    provider = FakeProvider(text="désolé, je ne peux pas")
    uc = _usecase(provider)
    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    assert out.report.questions == ()
    assert len(out.report.gaps) == 3  # toutes les clauses ont un constat
    assert out.report.major_count == 1  # criticité déterministe préservée


def test_hallucinated_clause_codes_are_dropped(user_context):
    provider = FakeProvider(
        text='{"questions": [{"clause_code": "99.9", "question": "fantôme ?"}], "findings": []}'
    )
    uc = _usecase(provider)
    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    assert out.report.questions == ()  # clause inconnue ignorée


def test_pii_findings_surfaced(user_context):
    uc = _usecase(FakeProvider(), pii=FakePii(findings=("EMAIL",)))
    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    assert out.pii_findings == ("EMAIL",)


def test_latency_fallback_when_provider_reports_zero(user_context):
    provider = FakeProvider()

    def _zero_latency(request):
        provider.requests.append(request)
        return CompletionResponse(
            text=_VALID_JSON, provider=ProviderName.OLLAMA,
            confidence=Confidence(0.5, "heuristic"), tokens_used=1, latency_ms=0,
        )

    provider.complete = _zero_latency  # type: ignore[assignment]
    uc = _usecase(provider)
    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=_spec()))
    # latency_ms tombe sur le chrono mesuré (>= 0).
    assert out.report.latency_ms >= 0


def test_prompt_caps_clauses(user_context):
    # Plus de 40 clauses : le prompt ne présente que les plus à risque.
    clauses = tuple(
        AuditClause(
            clause_code=f"c.{i}", title=f"Clause {i}",
            obligation=ObligationLevel.MUST, risk=RiskLevel.LOW,
            total_requirements=1, covered_requirements=0,
        )
        for i in range(50)
    )
    spec = MockAuditSpec(
        standard_code="iso-9001", standard_name="ISO 9001",
        industry="it", clauses=clauses,
    )
    provider = FakeProvider(text='{"questions": [], "findings": []}')
    uc = _usecase(provider)
    out = uc.execute(user_context, MockAuditUseCaseRequest(spec=spec))
    # Le rapport couvre TOUTES les clauses (gap par clause), même hors prompt.
    assert len(out.report.gaps) == 50
    # Le prompt ne liste que 40 clauses (borne de contexte).
    assert provider.requests[0].user_prompt.count("\n- ") == 40
