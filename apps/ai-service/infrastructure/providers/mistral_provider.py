"""Mistral HTTP adapter."""
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

_DEFAULT_URL = "https://api.mistral.ai/v1/chat/completions"
_DEFAULT_TIMEOUT_S = 30.0


class MistralProvider(AIProviderBase):
    """Adapter for api.mistral.ai."""

    def __init__(
        self,
        api_key: str | None = None,
        base_url: str = _DEFAULT_URL,
        model: str = "mistral-large-latest",
        timeout_s: float = _DEFAULT_TIMEOUT_S,
        client: httpx.Client | None = None,
    ) -> None:
        assert_host_allowed(base_url)
        self._api_key = api_key or os.environ.get("MISTRAL_API_KEY", "")
        self._base_url = base_url
        self._model = model
        self._timeout = timeout_s
        self._client = client or httpx.Client(timeout=timeout_s)

    def name(self) -> ProviderName:
        return ProviderName.MISTRAL

    def complete(self, request: CompletionRequest) -> CompletionResponse:
        if not self._api_key:
            raise ProviderUnavailableError("MISTRAL_API_KEY not configured")
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": request.system_prompt},
                {"role": "user", "content": request.user_prompt},
            ],
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
        }
        started = time.monotonic()
        try:
            resp = self._client.post(self._base_url, headers=headers, json=payload)
            resp.raise_for_status()
        except (httpx.HTTPError, httpx.TimeoutException) as exc:
            raise ProviderUnavailableError(f"Mistral unavailable: {exc}") from exc
        elapsed = int((time.monotonic() - started) * 1000)
        data = resp.json()
        choices = data.get("choices", [])
        text = (choices[0]["message"]["content"] if choices else "").strip()
        usage = data.get("usage", {})
        return CompletionResponse(
            text=text,
            provider=ProviderName.MISTRAL,
            confidence=Confidence(value=0.8, method="heuristic"),
            tokens_used=usage.get("total_tokens", 0),
            latency_ms=elapsed,
        )
