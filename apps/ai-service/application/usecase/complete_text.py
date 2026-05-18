"""Use case: complete a text prompt safely.

Flow:
  1) PromptInjectionFilter checks user input (LLM01).
  2) PiiFilter redacts PII from both input and (later) output (LLM06).
  3) AIProvider executes the completion (LLM04 â€” timeout enforced inside).
  4) PiiFilter scrubs the response.
  5) PromptAuditLogger records the (redacted) prompt + response (A09).
"""
from __future__ import annotations

import time
from dataclasses import dataclass

from domain.model.completion import (
    CompletionRequest,
    CompletionResponse,
    ProviderName,
)
from domain.model.errors import (
    PiiViolationError,
    PromptInjectionError,
)
from domain.model.tenant import UserContext
from domain.port.ai_provider import AIProvider
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import (
    PromptAuditEntry,
    PromptAuditLogger,
)
from domain.port.prompt_injection_filter import PromptInjectionFilter


@dataclass(frozen=True, slots=True)
class CompleteTextRequest:
    system_prompt: str
    user_prompt: str
    provider: ProviderName
    max_tokens: int = 1024
    temperature: float = 0.2
    reject_on_pii: bool = False  # if True, throws instead of redacting input


@dataclass(frozen=True, slots=True)
class CompleteTextResult:
    response: CompletionResponse
    pii_findings: tuple[str, ...]
    injection_score: float


class CompleteTextUseCase:
    """Orchestrates a safe completion."""

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

    def execute(self, user: UserContext, req: CompleteTextRequest) -> CompleteTextResult:
        # 1) Prompt-injection guard (LLM01).
        injection = self._injection.scan(req.user_prompt)
        if injection.suspicious:
            raise PromptInjectionError(
                f"Prompt-injection suspected (score={injection.score:.2f}): "
                f"{injection.rationale}"
            )

        # 2) PII redaction on input (LLM06 + A09 logging hygiene).
        scan_in = self._pii.redact(req.user_prompt)
        if req.reject_on_pii and scan_in.had_pii:
            raise PiiViolationError(
                f"Input contains PII categories: {','.join(scan_in.findings)}"
            )
        sanitized_user_prompt = scan_in.redacted_text

        # 3) Provider lookup â€” only the allow-listed providers (A10).
        provider = self._providers.get(req.provider)
        if provider is None:
            raise ValueError(f"provider not configured: {req.provider}")

        completion_req = CompletionRequest(
            system_prompt=req.system_prompt,
            user_prompt=sanitized_user_prompt,
            provider=req.provider,
            max_tokens=req.max_tokens,
            temperature=req.temperature,
        )

        # 4) Completion + 5) output PII scrub.
        started = time.monotonic()
        raw = provider.complete(completion_req)
        elapsed_ms = int((time.monotonic() - started) * 1000)

        scan_out = self._pii.redact(raw.text)
        scrubbed = CompletionResponse(
            text=scan_out.redacted_text,
            provider=raw.provider,
            confidence=raw.confidence,
            citations=raw.citations,
            tokens_used=raw.tokens_used,
            latency_ms=raw.latency_ms or elapsed_ms,
        )

        # 6) Audit log (already redacted).
        self._audit.log(
            PromptAuditEntry(
                tenant_id=user.tenant.tenant_id,
                user_id=user.user_id,
                correlation_id=user.correlation_id,
                operation="complete",
                provider=req.provider.value,
                redacted_prompt=sanitized_user_prompt[:1000],
                redacted_response=scan_out.redacted_text[:1000],
                latency_ms=scrubbed.latency_ms,
                tokens_used=scrubbed.tokens_used,
            )
        )

        merged_findings = tuple(set(scan_in.findings) | set(scan_out.findings))
        return CompleteTextResult(
            response=scrubbed,
            pii_findings=merged_findings,
            injection_score=injection.score,
        )
