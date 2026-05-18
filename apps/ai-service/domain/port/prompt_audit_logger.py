"""Prompt audit log port (OWASP A09 + LLM06)."""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True, slots=True)
class PromptAuditEntry:
    """An audit entry — already PII-redacted before reaching the logger."""

    tenant_id: UUID
    user_id: UUID
    correlation_id: str
    operation: str  # 'complete', 'rag.query', 'nlq.ask'
    provider: str
    redacted_prompt: str  # MUST already be Presidio-redacted
    redacted_response: str
    latency_ms: int
    tokens_used: int


class PromptAuditLogger(ABC):
    """Writes structured, append-only prompt audit logs."""

    @abstractmethod
    def log(self, entry: PromptAuditEntry) -> None:
        """Log the entry. MUST be non-blocking on best-effort backends."""
