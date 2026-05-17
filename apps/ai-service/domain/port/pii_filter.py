"""PII redaction port — Presidio adapter lives in infrastructure/guardrails/."""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class PiiScanResult:
    redacted_text: str
    findings: tuple[str, ...]  # categories detected: PERSON, EMAIL, ...
    had_pii: bool


class PiiFilter(ABC):
    """Detects and redacts PII from text before logging or LLM calls."""

    @abstractmethod
    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        """Redact PII. Returns sanitized text + findings."""
