"""GenerateNormDocUseCase orchestration tests (§8.8)."""
from __future__ import annotations

from dataclasses import dataclass, field

import pytest

from application.usecase.generate_norm_doc import (
    GenerateNormDocRequest,
    GenerateNormDocUseCase,
)
from domain.model.completion import (
    CompletionRequest,
    CompletionResponse,
    Confidence,
    ProviderName,
)
from domain.model.errors import PromptInjectionError
from domain.model.normdoc import (
    NormDocKind,
    NormDocSpec,
    SectionSpec,
    TenantProfile,
)
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter, PiiScanResult
from domain.port.prompt_audit_logger import PromptAuditLogger
from domain.port.prompt_injection_filter import (
    InjectionScanResult,
    PromptInjectionFilter,
)


@dataclass
class FakeProvider(AIProvider):
    requests: list = field(default_factory=list)

    def name(self) -> ProviderName:
        return ProviderName.OLLAMA

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        self.requests.append(request)
        return CompletionResponse(
            text=f"corps[{request.user_prompt[-20:]}]",
            provider=ProviderName.OLLAMA,
            confidence=Confidence(0.6, "heuristic"),
            tokens_used=7,
            latency_ms=3,
        )


class FakePii(PiiFilter):
    def __init__(self, findings: tuple[str, ...] = ()) -> None:
        self._findings = findings

    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        return PiiScanResult(
            redacted_text=text.replace("alice@example.com", "<EMAIL>"),
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


def _spec(kind: NormDocKind = NormDocKind.MANUAL) -> NormDocSpec:
    return NormDocSpec(
        kind=kind,
        standard_code="iso-9001",
        standard_name="ISO 9001:2015",
        tenant_profile=TenantProfile(
            organization_name="ACME",
            industry="manufacturing",
            size="PME",
            language="fr",
            known_processes=("achats",),
        ),
        sections=(
            SectionSpec(key="ctx", title="Contexte", clauses=("4.1",), guidance="cadrer"),
            SectionSpec(key="lead", title="Leadership", clauses=("5.1",)),
        ),
    )


def _usecase(provider, pii=None, inj=None, audit=None) -> GenerateNormDocUseCase:
    return GenerateNormDocUseCase(
        providers={ProviderName.OLLAMA: provider},
        pii_filter=pii or FakePii(),
        injection_filter=inj or FakeInjection(),
        audit_logger=audit or FakeAudit(),
    )


def test_requires_at_least_one_provider():
    with pytest.raises(ValueError, match="at least one provider"):
        GenerateNormDocUseCase({}, FakePii(), FakeInjection(), FakeAudit())


def test_happy_path_generates_all_sections(user_context):
    provider = FakeProvider()
    audit = FakeAudit()
    uc = _usecase(provider, audit=audit)

    out = uc.execute(user_context, GenerateNormDocRequest(spec=_spec()))

    assert len(out.draft.sections) == 2
    assert out.draft.sections[0].key == "ctx"
    assert out.draft.sections[0].clauses == ("4.1",)
    assert out.draft.title.startswith("Manuel Qualité — ACME")
    # Une requête LLM par section.
    assert len(provider.requests) == 2
    # System prompt mentionne la langue + la norme + le type de document.
    sys_prompt = provider.requests[0].system_prompt
    assert "Manuel Qualité" in sys_prompt
    assert "ISO 9001:2015 (iso-9001)" in sys_prompt
    # Tokens cumulés et provider remontés.
    assert out.draft.tokens_used == 14
    assert out.draft.provider == "ollama"
    # Un audit par section.
    assert len(audit.entries) == 2
    assert audit.entries[0].operation == "standards.generate-document"


@pytest.mark.parametrize(
    ("kind", "label"),
    [
        (NormDocKind.MANUAL, "Manuel Qualité"),
        (NormDocKind.POLICY, "Politique Qualité"),
        (NormDocKind.PROCEDURE, "Procédure documentée"),
    ],
)
def test_kind_label_in_title(kind, label, user_context):
    uc = _usecase(FakeProvider())
    out = uc.execute(user_context, GenerateNormDocRequest(spec=_spec(kind)))
    assert out.draft.title.startswith(f"{label} — ACME")


def test_unknown_provider_raises(user_context):
    uc = _usecase(FakeProvider())
    with pytest.raises(ValueError, match="provider not configured"):
        uc.execute(
            user_context,
            GenerateNormDocRequest(spec=_spec(), provider=ProviderName.ANTHROPIC),
        )


def test_injection_in_section_blocks(user_context):
    provider = FakeProvider()
    uc = _usecase(provider, inj=FakeInjection(suspicious=True))
    with pytest.raises(PromptInjectionError, match="section 'ctx'"):
        uc.execute(user_context, GenerateNormDocRequest(spec=_spec()))
    # Aucun appel provider effectué avant le blocage.
    assert provider.requests == []


def test_pii_findings_merged_and_redacted(user_context):
    provider = FakeProvider()
    uc = _usecase(provider, pii=FakePii(findings=("EMAIL",)))
    out = uc.execute(user_context, GenerateNormDocRequest(spec=_spec()))
    assert out.pii_findings == ("EMAIL",)


def test_context_includes_known_processes(user_context):
    provider = FakeProvider()
    uc = _usecase(provider)
    uc.execute(user_context, GenerateNormDocRequest(spec=_spec()))
    assert "Processus connus : achats" in provider.requests[0].user_prompt


def test_context_without_known_processes(user_context):
    provider = FakeProvider()
    uc = _usecase(provider)
    spec = NormDocSpec(
        kind=NormDocKind.POLICY,
        standard_code="iso-27001",
        standard_name="ISO 27001",
        tenant_profile=TenantProfile(
            organization_name="ACME", industry="it", size="ETI"
        ),
        sections=(SectionSpec(key="s", title="Champ"),),
    )
    uc.execute(user_context, GenerateNormDocRequest(spec=spec))
    prompt = provider.requests[0].user_prompt
    assert "Processus connus" not in prompt
    # Une section sans clauses n'ajoute pas de ligne clauses.
    assert "Clauses couvertes" not in prompt


def test_section_prompt_carries_guidance_and_clauses(user_context):
    provider = FakeProvider()
    uc = _usecase(provider)
    uc.execute(user_context, GenerateNormDocRequest(spec=_spec()))
    first = provider.requests[0].user_prompt
    assert "Clauses couvertes : 4.1." in first
    assert "Consigne : cadrer" in first
    # La 2e section n'a pas de guidance.
    second = provider.requests[1].user_prompt
    assert "Consigne :" not in second
