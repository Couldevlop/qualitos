"""Use case: generate a complete, multi-section normative document (§8.8).

Flow (par section, pour borner le contexte LLM et tracer les clauses) :
  1) PromptInjectionFilter contrôle le guidance/profil (LLM01).
  2) PiiFilter caviarde les entrées et la sortie (LLM06 + A09).
  3) AIProvider rédige chaque section (LLM04 — timeout interne au provider).
  4) PromptAuditLogger journalise les prompts/réponses caviardés (A09).

Le résultat est un ``NormDocDraft`` complet (brouillon IA). La publication est
soumise à validation humaine côté engine (CLAUDE.md §18.2 #5 — l'IA suggère,
l'humain décide). Ce service ne fait que produire le brouillon.
"""
from __future__ import annotations

import time
from dataclasses import dataclass

from domain.model.completion import CompletionRequest, ProviderName
from domain.model.errors import PromptInjectionError
from domain.model.normdoc import (
    GeneratedSection,
    NormDocDraft,
    NormDocKind,
    NormDocSpec,
)
from domain.model.tenant import UserContext
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import (
    PromptAuditEntry,
    PromptAuditLogger,
)
from domain.port.prompt_injection_filter import PromptInjectionFilter

_KIND_LABEL: dict[NormDocKind, str] = {
    NormDocKind.MANUAL: "Manuel Qualité",
    NormDocKind.POLICY: "Politique Qualité",
    NormDocKind.PROCEDURE: "Procédure documentée",
}

_SYSTEM_PROMPT = (
    "Tu es un expert qualité QualitOS. Rédige en {language} le contenu d'UNE "
    "section d'un {kind} conforme à la norme {standard}. Style : Markdown "
    "structuré (sous-titres, listes, tableaux si utile), concis et directement "
    "exploitable. Marque tout élément à personnaliser par [[à compléter]]. "
    "N'invente AUCUNE exigence hors de la norme citée. Ne répète pas le titre "
    "de la section. Réponds uniquement avec le corps de la section."
)


@dataclass(frozen=True, slots=True)
class GenerateNormDocRequest:
    spec: NormDocSpec
    provider: ProviderName = ProviderName.OLLAMA
    max_tokens_per_section: int = 512
    temperature: float = 0.2


@dataclass(frozen=True, slots=True)
class GenerateNormDocResult:
    draft: NormDocDraft
    pii_findings: tuple[str, ...]


class GenerateNormDocUseCase:
    """Orchestre la génération section par section d'un document normatif."""

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
        self, user: UserContext, req: GenerateNormDocRequest
    ) -> GenerateNormDocResult:
        spec = req.spec
        provider = self._providers.get(req.provider)
        if provider is None:
            raise ValueError(f"provider not configured: {req.provider}")

        kind_label = _KIND_LABEL[spec.kind]
        profile = spec.tenant_profile

        # 1) Contexte tenant caviardé une fois (réutilisé par section).
        context_block = self._build_context_block(spec)
        context_scan = self._pii.redact(context_block)
        # Le profil étant non sensible, on garde le contexte caviardé tel quel.
        sanitized_context = context_scan.redacted_text

        system_prompt = _SYSTEM_PROMPT.format(
            language=profile.language,
            kind=kind_label,
            standard=f"{spec.standard_name} ({spec.standard_code})",
        )

        generated: list[GeneratedSection] = []
        findings: set[str] = set(context_scan.findings)
        total_tokens = 0
        last_provider = req.provider.value
        started = time.monotonic()

        for section in spec.sections:
            user_prompt = self._build_section_prompt(
                sanitized_context, kind_label, section
            )

            # 1bis) Garde anti-injection sur le guidance (entrée semi-libre).
            injection = self._injection.scan(user_prompt)
            if injection.suspicious:
                raise PromptInjectionError(
                    f"Prompt-injection suspected in section {section.key!r} "
                    f"(score={injection.score:.2f})"
                )

            completion = provider.complete(
                CompletionRequest(
                    system_prompt=system_prompt,
                    user_prompt=user_prompt,
                    provider=req.provider,
                    max_tokens=req.max_tokens_per_section,
                    temperature=req.temperature,
                )
            )
            scan_out = self._pii.redact(completion.text)
            findings.update(scan_out.findings)
            total_tokens += completion.tokens_used
            last_provider = completion.provider.value

            generated.append(
                GeneratedSection(
                    key=section.key,
                    title=section.title,
                    clauses=section.clauses,
                    body_markdown=scan_out.redacted_text.strip(),
                )
            )

            self._audit.log(
                PromptAuditEntry(
                    tenant_id=user.tenant.tenant_id,
                    user_id=user.user_id,
                    correlation_id=user.correlation_id,
                    operation="standards.generate-document",
                    provider=req.provider.value,
                    redacted_prompt=user_prompt[:1000],
                    redacted_response=scan_out.redacted_text[:1000],
                    latency_ms=completion.latency_ms,
                    tokens_used=completion.tokens_used,
                )
            )

        elapsed_ms = int((time.monotonic() - started) * 1000)
        draft = NormDocDraft(
            kind=spec.kind,
            standard_code=spec.standard_code,
            standard_name=spec.standard_name,
            title=f"{kind_label} — {profile.organization_name} ({spec.standard_code})",
            sections=tuple(generated),
            provider=last_provider,
            tokens_used=total_tokens,
            latency_ms=elapsed_ms,
        )
        return GenerateNormDocResult(draft=draft, pii_findings=tuple(sorted(findings)))

    @staticmethod
    def _build_context_block(spec: NormDocSpec) -> str:
        profile = spec.tenant_profile
        lines = [
            f"Organisation : {profile.organization_name}",
            f"Secteur : {profile.industry}",
            f"Taille : {profile.size}",
        ]
        if profile.known_processes:
            lines.append("Processus connus : " + ", ".join(profile.known_processes))
        return "\n".join(lines)

    @staticmethod
    def _build_section_prompt(context: str, kind_label: str, section) -> str:
        parts = [
            f"Contexte de l'organisation :\n{context}",
            f"Document : {kind_label}.",
            f"Section à rédiger : {section.title}.",
        ]
        if section.clauses:
            parts.append("Clauses couvertes : " + ", ".join(section.clauses) + ".")
        if section.guidance:
            parts.append("Consigne : " + section.guidance)
        parts.append("Rédige le corps de cette section.")
        return "\n".join(parts)
