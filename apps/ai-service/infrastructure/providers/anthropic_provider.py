"""Anthropic HTTP adapter (Claude family)."""
from __future__ import annotations

import os
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

_DEFAULT_URL = "https://api.anthropic.com/v1/messages"
_DEFAULT_TIMEOUT_S = 30.0


class AnthropicProvider(AIProviderBase):
    """Adapter for api.anthropic.com.

    API key read from ANTHROPIC_API_KEY env only â€” never accept it in request bodies.
    """

    def __init__(
        self,
        api_key: str | None = None,
        base_url: str = _DEFAULT_URL,
        model: str = "claude-opus-4-7",
        timeout_s: float = _DEFAULT_TIMEOUT_S,
        client: httpx.Client | None = None,
    ) -> None:
        assert_host_allowed(base_url)
        self._api_key = api_key or os.environ.get("ANTHROPIC_API_KEY", "")
        self._base_url = base_url
        self._model = model
        self._timeout = timeout_s
        self._client = client or httpx.Client(timeout=timeout_s)

    def name(self) -> ProviderName:
        return ProviderName.ANTHROPIC

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        if not self._api_key:
            raise ProviderUnavailableError("ANTHROPIC_API_KEY not configured")
        headers = {
            "x-api-key": self._api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
        }
        payload = {
            "model": self._model,
            "system": request.system_prompt,
            "messages": [{"role": "user", "content": request.user_prompt}],
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
        }
        started = time.monotonic()
        try:
            resp = self._client.post(self._base_url, headers=headers, json=payload)
            resp.raise_for_status()
        except (httpx.HTTPError, httpx.TimeoutException) as exc:
            raise ProviderUnavailableError(f"Anthropic unavailable: {exc}") from exc
        elapsed = int((time.monotonic() - started) * 1000)
        data = resp.json()
        content = data.get("content", [])
        text = "".join(b.get("text", "") for b in content if b.get("type") == "text")
        usage = data.get("usage", {})
        return CompletionResponse(
            text=text.strip(),
            provider=ProviderName.ANTHROPIC,
            confidence=Confidence(value=0.85, method="heuristic"),
            tokens_used=usage.get("input_tokens", 0) + usage.get("output_tokens", 0),
            latency_ms=elapsed,
        )
