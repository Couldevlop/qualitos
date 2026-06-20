"""Use case: audit blanc IA avancé (Standards Hub §8.4 onglet 7).

Avant l'audit de certification officiel, l'IA simule un audit sur la norme
adoptée :
  1) sélectionne les clauses à risque (déterministe, domaine) ;
  2) demande au LLM, en sortie JSON structurée, de *générer 30-100 questions
     ciblées* sur ces clauses ET de *confronter* chaque clause aux preuves
     disponibles (constat d'écart) ;
  3) projette la sortie sur les clauses connues (anti-hallucination) ;
  4) restitue un rapport d'écarts (gap analysis) avec criticité déterministe.

Garde-fous OWASP LLM : anti-injection (LLM01), redaction PII (LLM06), audit
journalisé (A09), provider allow-listé (A10). Les questions et constats viennent
RÉELLEMENT du LLM ; la matière (clauses + état de preuve) vient de l'engine.
"""
from __future__ import annotations

import time
from dataclasses import dataclass

from domain.model.completion import CompletionRequest, ProviderName
from domain.model.errors import PromptInjectionError
from domain.model.mock_audit import (
    AuditClause,
    MockAuditReport,
    MockAuditSpec,
)
from domain.model.tenant import UserContext
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import (
    PromptAuditEntry,
    PromptAuditLogger,
)
from domain.port.prompt_injection_filter import PromptInjectionFilter
from domain.service import mock_audit_parser

_SYSTEM_PROMPT = (
    "Tu es un auditeur certifié QualitOS qui prépare un audit blanc de la norme "
    "{standard}. À partir des clauses À RISQUE fournies (avec leur état de "
    "couverture par les preuves du tenant), tu rédiges en {language} :\n"
    "1) des QUESTIONS d'audit ciblées (une ou plusieurs par clause, priorité aux "
    "clauses obligatoires faiblement couvertes) ;\n"
    "2) un CONSTAT d'écart par clause confrontant l'attendu aux preuves "
    "disponibles.\n"
    "Réponds UNIQUEMENT par un objet JSON valide, sans prose autour, de la forme : "
    '{{"questions":[{{"clause_code":"...","question":"...","rationale":"..."}}],'
    '"findings":[{{"clause_code":"...","finding":"..."}}]}}. '
    "N'utilise QUE les codes de clause fournis ; n'invente aucune clause ni preuve."
)


@dataclass(frozen=True, slots=True)
class MockAuditUseCaseRequest:
    spec: MockAuditSpec
    provider: ProviderName = ProviderName.OLLAMA
    max_tokens: int = 2048
    temperature: float = 0.2


@dataclass(frozen=True, slots=True)
class MockAuditUseCaseResult:
    report: MockAuditReport
    pii_findings: tuple[str, ...]


class MockAuditUseCase:
    """Orchestre la génération guidée d'un audit blanc (questions + écarts)."""

    # Au plus N clauses présentées au LLM (borne le contexte ; les plus à risque).
    _MAX_CLAUSES_IN_PROMPT = 40

    def __init__(
        self,
        providers: dict[ProviderName, AIProvider],
        pii_filter: PiiFilter,
        injection_filter: PromptInjectionFilter,
        audit_logger: PromptAuditLogger,
    ) -> None:
        if not providers:
            raise ValueError("at least one provider required")
        self._providers = providers
        self._pii = pii_filter
        self._injection = injection_filter
        self._audit = audit_logger

    def execute(
        self, user: UserContext, req: MockAuditUseCaseRequest
    ) -> MockAuditUseCaseResult:
        spec = req.spec
        provider = self._providers.get(req.provider)
        if provider is None:
            raise ValueError(f"provider not configured: {req.provider}")

        # 1) Clauses à risque, triées par priorité d'audit décroissante (domaine).
        ranked = sorted(spec.clauses, key=lambda c: c.risk_score(), reverse=True)
        prompt_clauses = ranked[: self._MAX_CLAUSES_IN_PROMPT]
        known = {c.clause_code: c for c in spec.clauses}

        system_prompt = _SYSTEM_PROMPT.format(
            standard=f"{spec.standard_name} ({spec.standard_code})",
            language=spec.language,
        )
        user_prompt = self._build_prompt(spec, prompt_clauses)

        # 2) Garde anti-injection sur l'entrée semi-libre (titres de clauses).
        injection = self._injection.scan(user_prompt)
        if injection.suspicious:
            raise PromptInjectionError(
                f"Prompt-injection suspected (score={injection.score:.2f})"
            )

        sanitized_prompt = self._pii.redact(user_prompt).redacted_text

        started = time.monotonic()
        completion = provider.complete(
            CompletionRequest(
                system_prompt=system_prompt,
                user_prompt=sanitized_prompt,
                provider=req.provider,
                max_tokens=req.max_tokens,
                temperature=req.temperature,
            )
        )
        elapsed_ms = int((time.monotonic() - started) * 1000)

        scan_out = self._pii.redact(completion.text)
        clean_text = scan_out.redacted_text

        # 3) Projection défensive sur les clauses connues (anti-hallucination).
        questions = mock_audit_parser.parse_questions(clean_text, spec, known)
        ai_findings = mock_audit_parser.parse_findings(clean_text, known)
        gaps = mock_audit_parser.build_gaps(spec, questions, ai_findings)
        major, minor, observation = mock_audit_parser.count_by_criticality(gaps)
        readiness = mock_audit_parser.readiness_score(spec)

        report = MockAuditReport(
            standard_code=spec.standard_code,
            standard_name=spec.standard_name,
            questions=questions,
            gaps=gaps,
            readiness=readiness,
            major_count=major,
            minor_count=minor,
            observation_count=observation,
            provider=completion.provider.value,
            tokens_used=completion.tokens_used,
            latency_ms=completion.latency_ms or elapsed_ms,
        )

        self._audit.log(
            PromptAuditEntry(
                tenant_id=user.tenant.tenant_id,
                user_id=user.user_id,
                correlation_id=user.correlation_id,
                operation="standards.mock-audit",
                provider=req.provider.value,
                redacted_prompt=sanitized_prompt[:1000],
                redacted_response=clean_text[:1000],
                latency_ms=report.latency_ms,
                tokens_used=report.tokens_used,
            )
        )

        return MockAuditUseCaseResult(
            report=report, pii_findings=tuple(sorted(set(scan_out.findings)))
        )

    @staticmethod
    def _build_prompt(
        spec: MockAuditSpec, clauses: list[AuditClause]
    ) -> str:
        lines = [
            f"Norme : {spec.standard_name} ({spec.standard_code}).",
            f"Secteur du tenant : {spec.industry}.",
            (
                f"Génère entre {spec.min_questions} et {spec.max_questions} "
                "questions d'audit ciblées."
            ),
            "Clauses à risque (code | titre | caractère | risque | preuves) :",
        ]
        for c in clauses:
            lines.append(
                f"- {c.clause_code} | {c.title} | {c.obligation.value} | "
                f"{c.risk.value} | preuves {c.covered_requirements}/"
                f"{c.total_requirements}"
            )
        lines.append(
            "Priorise les clauses obligatoires (must) à risque élevé et "
            "faiblement couvertes. Réponds uniquement par le JSON demandé."
        )
        return "\n".join(lines)
