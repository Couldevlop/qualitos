"""Tests du provider LLM par défaut piloté par l'environnement (extension ADR 0014)."""
from __future__ import annotations

import importlib

from domain.model.completion import ProviderName
from presentation.provider_defaults import resolve_default_provider


def test_none_falls_back_to_ollama():
    assert resolve_default_provider(None) is ProviderName.OLLAMA


def test_empty_falls_back_to_ollama():
    assert resolve_default_provider("") is ProviderName.OLLAMA


def test_mistral():
    assert resolve_default_provider("mistral") is ProviderName.MISTRAL


def test_case_and_space_insensitive():
    assert resolve_default_provider("  Mistral ") is ProviderName.MISTRAL


def test_anthropic():
    assert resolve_default_provider("anthropic") is ProviderName.ANTHROPIC


def test_unknown_falls_back_to_ollama():
    assert resolve_default_provider("gpt-9000") is ProviderName.OLLAMA


def test_module_constant_reads_env(monkeypatch):
    """Le constant DEFAULT_PROVIDER est résolu depuis l'env au chargement du module."""
    monkeypatch.setenv("AI_DEFAULT_PROVIDER", "mistral")
    import presentation.provider_defaults as mod
    importlib.reload(mod)
    try:
        assert mod.DEFAULT_PROVIDER is ProviderName.MISTRAL
    finally:
        monkeypatch.delenv("AI_DEFAULT_PROVIDER", raising=False)
        importlib.reload(mod)  # restaure le défaut (ollama) pour les autres tests
