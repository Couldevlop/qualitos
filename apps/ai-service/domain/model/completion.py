"""Text completion domain objects."""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any


class ProviderName(str, Enum):
    """Allowed providers — keep this list narrow (OWASP A10 SSRF / LLM05)."""

    OLLAMA = "ollama"
    ANTHROPIC = "anthropic"
    MISTRAL = "mistral"


@dataclass(frozen=True, slots=True)
class Citation:
    """A source pulled from RAG that the LLM relied on (LLM09 Overreliance)."""

    document_id: str
    score: float
    excerpt: str

    def __post_init__(self) -> None:
        if not 0.0 <= self.score <= 1.0:
            raise ValueError("score must be in [0, 1]")


@dataclass(frozen=True, slots=True)
class Confidence:
    """Calibrated confidence score, mandatory to surface to the user."""

    value: float
    method: str  # 'logprob', 'self-report', 'heuristic'

    def __post_init__(self) -> None:
        if not 0.0 <= self.value <= 1.0:
            raise ValueError("confidence value must be in [0, 1]")


@dataclass(frozen=True, slots=True)
class CompletionRequest:
    """A safe completion request — already PII-redacted and injection-checked."""

    system_prompt: str
    user_prompt: str
    provider: ProviderName
    max_tokens: int = 1024
    temperature: float = 0.2
    response_schema: dict[str, Any] | None = None  # optional structured output

    def __post_init__(self) -> None:
        if self.max_tokens < 1 or self.max_tokens > 8192:
            raise ValueError("max_tokens must be 1..8192")
        if not 0.0 <= self.temperature <= 2.0:
            raise ValueError("temperature must be 0..2")
        if not self.user_prompt or not self.user_prompt.strip():
            raise ValueError("user_prompt required")


@dataclass(frozen=True, slots=True)
class CompletionResponse:
    """An LLM completion plus mandatory explainability."""

    text: str
    provider: ProviderName
    confidence: Confidence
    citations: tuple[Citation, ...] = field(default_factory=tuple)
    tokens_used: int = 0
    latency_ms: int = 0
