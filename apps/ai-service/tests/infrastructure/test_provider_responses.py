"""Provider adapters — exercise success + error paths with mocked httpx."""
from __future__ import annotations

import json

import httpx
import pytest

from domain.model.completion import CompletionRequest, ProviderName
from domain.model.errors import ProviderUnavailableError
from infrastructure.providers import (
    AnthropicProvider,
    MistralProvider,
    OllamaProvider,
)


def _client(handler: httpx.MockTransport) -> httpx.Client:
    return httpx.Client(transport=handler, timeout=5.0)


def test_ollama_success():
    def handler(req: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200, json={"response": "ok answer", "eval_count": 42}
        )

    p = OllamaProvider(client=_client(httpx.MockTransport(handler)))
    out = p.complete(
        CompletionRequest(
            system_prompt="s",
            user_prompt="u",
            provider=ProviderName.OLLAMA,
        )
    )
    assert out.text == "ok answer"
    assert out.tokens_used == 42


def test_ollama_unavailable_raises():
    def handler(req: httpx.Request) -> httpx.Response:
        return httpx.Response(503, text="down")

    p = OllamaProvider(client=_client(httpx.MockTransport(handler)))
    with pytest.raises(ProviderUnavailableError):
        p.complete(
            CompletionRequest(
                system_prompt="s",
                user_prompt="u",
                provider=ProviderName.OLLAMA,
            )
        )


def test_anthropic_requires_api_key():
    p = AnthropicProvider(api_key="", client=_client(httpx.MockTransport(lambda r: httpx.Response(200))))
    with pytest.raises(ProviderUnavailableError):
        p.complete(
            CompletionRequest(
                system_prompt="s", user_prompt="u", provider=ProviderName.ANTHROPIC
            )
        )


def test_anthropic_success_parses_content():
    def handler(req: httpx.Request) -> httpx.Response:
        body = json.loads(req.content.decode())
        assert "system" in body
        return httpx.Response(
            200,
            json={
                "content": [{"type": "text", "text": "yes"}],
                "usage": {"input_tokens": 5, "output_tokens": 3},
            },
        )

    p = AnthropicProvider(
        api_key="sk-test", client=_client(httpx.MockTransport(handler))
    )
    out = p.complete(
        CompletionRequest(
            system_prompt="s", user_prompt="u", provider=ProviderName.ANTHROPIC
        )
    )
    assert out.text == "yes"
    assert out.tokens_used == 8


def test_mistral_success():
    def handler(req: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={
                "choices": [{"message": {"content": "hi"}}],
                "usage": {"total_tokens": 11},
            },
        )

    p = MistralProvider(api_key="sk-test", client=_client(httpx.MockTransport(handler)))
    out = p.complete(
        CompletionRequest(
            system_prompt="s", user_prompt="u", provider=ProviderName.MISTRAL
        )
    )
    assert out.text == "hi"


def test_anthropic_rejects_attacker_url():
    with pytest.raises(PermissionError):
        AnthropicProvider(base_url="https://evil.example.com/x")


def test_mistral_rejects_attacker_url():
    with pytest.raises(PermissionError):
        MistralProvider(base_url="https://attacker.local/x")


def test_ollama_rejects_attacker_url():
    with pytest.raises(PermissionError):
        OllamaProvider(base_url="http://attacker.com:1234")


def test_remote_providers_are_configured_only_with_their_key(monkeypatch):
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    monkeypatch.delenv("MISTRAL_API_KEY", raising=False)
    assert AnthropicProvider(api_key="").is_configured() is False
    assert MistralProvider(api_key="").is_configured() is False
    assert AnthropicProvider(api_key="k").is_configured() is True
    assert MistralProvider(api_key="k").is_configured() is True


def test_remote_provider_key_can_come_from_the_environment(monkeypatch):
    monkeypatch.setenv("MISTRAL_API_KEY", "from-env")
    assert MistralProvider().is_configured() is True


def test_local_provider_needs_no_key():
    assert OllamaProvider(base_url="http://ollama:11434", model="m").is_configured() is True
