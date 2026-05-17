"""Ollama HTTP adapter (default on-prem LLM)."""
from __future__ import annotations

import time

import httpx

from domain.model.completion import (
    CompletionRequest,
    CompletionResponse,
    Confidence,
    ProviderName,
)
from domain.model.errors import ProviderUnavailableError
from ._allowlist import assert_host_allowed
from infrastructure.ai_provider_base import AIProviderBase

_DEFAULT_TIMEOUT_S = 30.0


class OllamaProvider(AIProviderBase):
    """Calls a local Ollama server (default: llama3.1:8b)."""

    def __init__(
        self,
        base_url: str = "http://ollama:11434",
        model: str = "llama3.1:8b",
        timeout_s: float = _DEFAULT_TIMEOUT_S,
        client: httpx.Client | None = None,
    ) -> None:
        assert_host_allowed(base_url)
        self._base_url = base_url.rstrip("/")
        self._model = model
        self._timeout = timeout_s
        self._client = client or httpx.Client(timeout=timeout_s)

    def name(self) -> ProviderName:
        return ProviderName.OLLAMA

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        url = f"{self._base_url}/api/generate"
        payload = {
            "model": self._model,
            "prompt": f"{request.system_prompt}\n\n{request.user_prompt}",
            "stream": False,
            "options": {
                "num_predict": request.max_tokens,
                "temperature": request.temperature,
            },
        }
        started = time.monotonic()
        try:
            resp = self._client.post(url, json=payload)
            resp.raise_for_status()
        except (httpx.HTTPError, httpx.TimeoutException) as exc:
            raise ProviderUnavailableError(f"Ollama unavailable: {exc}") from exc
        elapsed = int((time.monotonic() - started) * 1000)
        data = resp.json()
        return CompletionResponse(
            text=data.get("response", "").strip(),
            provider=ProviderName.OLLAMA,
            confidence=Confidence(value=0.7, method="heuristic"),
            tokens_used=data.get("eval_count", 0),
            latency_ms=elapsed,
        )
