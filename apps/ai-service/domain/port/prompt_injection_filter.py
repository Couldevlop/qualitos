"""Prompt-injection filter port (OWASP LLM01)."""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class InjectionScanResult:
    """Result of a prompt-injection scan."""

    suspicious: bool
    score: float  # 0..1
    rationale: str

    def __post_init__(self) -> None:
        if not 0.0 <= self.score <= 1.0:
            raise ValueError("score must be in [0, 1]")


class PromptInjectionFilter(ABC):
    """Detects prompt-injection / jailbreak attempts in user input."""

    @abstractmethod
    def scan(self, user_prompt: str) -> InjectionScanResult:
        """Scan a user prompt for injection patterns."""
