"""AI provider port â€” every LLM call goes through this interface."""
from __future__ import annotations

from abc import ABC, abstractmethod

from domain.model.completion import CompletionRequest, CompletionResponse, ProviderName


class AIProvider(ABC):
    """Port to an LLM backend (Ollama, Anthropic, Mistral).

    Adapters MUST:
      * never accept user-supplied URLs (LLM07 / A10 SSRF)
      * enforce timeouts (LLM04)
      * surface confidence + token usage to the response (LLM09)
    """

    @abstractmethod
    def name(self) -> ProviderName:
        """Return the provider identifier."""

    @abstractmethod
    def complete(self, request: CompletionRequest) -> CompletionResponse:
        """Generate a completion. Synchronous to keep the use-case simple."""

    def is_configured(self) -> bool:
        """Whether this provider has what it needs to answer (e.g. its API key).

        Lets an administrator see, before pinning a tenant on a provider, that the
        call would not fail on a missing secret. Says nothing about the secret
        itself, and does not probe the network: a local provider is configured by
        its URL alone.
        """
        return True
