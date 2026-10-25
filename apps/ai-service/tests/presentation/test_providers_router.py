"""GET /v1/ai/providers: which providers can answer, without leaking any secret."""
from __future__ import annotations

import importlib
import json
import os

os.environ["QOS_DEV_AUTH"] = "true"

from fastapi.testclient import TestClient  # noqa: E402

from domain.model.completion import CompletionRequest, CompletionResponse, ProviderName  # noqa: E402
from domain.port.ai_provider import AIProvider  # noqa: E402
from presentation.app import create_app  # noqa: E402
from presentation.provider_defaults import DEFAULT_PROVIDER  # noqa: E402

_HEADERS = {
    "X-Dev-Claims": json.dumps({
        "sub": "22222222-2222-2222-2222-222222222222",
        "tid": "11111111-1111-1111-1111-111111111111",
        "roles": ["quality_manager"],
    }),
}


class _Provider(AIProvider):
    def __init__(self, name: ProviderName, configured: bool) -> None:
        self._name = name
        self._configured = configured

    def name(self) -> ProviderName:
        return self._name

    def complete(self, request: CompletionRequest) -> CompletionResponse:  # pragma: no cover
        raise AssertionError("listing the providers must not call any of them")

    def is_configured(self) -> bool:
        return self._configured


class _Container:
    def __init__(self, providers: dict[ProviderName, AIProvider]) -> None:
        self.providers = providers


def _client_with(providers: dict[ProviderName, AIProvider]) -> TestClient:
    router = importlib.import_module("presentation.routers.completion_router")
    router._container = _Container(providers)  # type: ignore[attr-defined]
    return TestClient(create_app())


def test_lists_each_provider_with_its_status_in_a_stable_order():
    client = _client_with({
        ProviderName.MISTRAL: _Provider(ProviderName.MISTRAL, False),
        ProviderName.OLLAMA: _Provider(ProviderName.OLLAMA, True),
        ProviderName.ANTHROPIC: _Provider(ProviderName.ANTHROPIC, True),
    })
    with client:
        r = client.get("/v1/ai/providers", headers=_HEADERS)
    assert r.status_code == 200, r.text
    assert r.json() == {
        "default_provider": DEFAULT_PROVIDER.value,
        "providers": [
            {"name": "anthropic", "configured": True},
            {"name": "mistral", "configured": False},
            {"name": "ollama", "configured": True},
        ],
    }


def test_answers_nothing_but_names_and_yes_no():
    client = _client_with({ProviderName.OLLAMA: _Provider(ProviderName.OLLAMA, True)})
    with client:
        body = client.get("/v1/ai/providers", headers=_HEADERS).json()
    assert set(body) == {"default_provider", "providers"}
    assert all(set(p) == {"name", "configured"} for p in body["providers"])


def test_requires_authentication():
    client = _client_with({ProviderName.OLLAMA: _Provider(ProviderName.OLLAMA, True)})
    with client:
        r = client.get("/v1/ai/providers")
    assert r.status_code == 401
