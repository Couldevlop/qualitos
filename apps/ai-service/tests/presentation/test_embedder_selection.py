"""Choix de l'adaptateur d'embeddings (ADR 0049).

Le point sensible n'est pas quel adaptateur est retenu, mais qu'AUCUN repli ne
se fasse en silence : jusqu'ici, faute de `FlagEmbedding` dans l'image, le RAG
indexait et interrogeait avec des vecteurs de hachage, sans le dire à personne.
"""
from __future__ import annotations

import pytest

from infrastructure.vector.bge_m3_embedder import BgeM3Embedder, DeterministicEmbedder
from infrastructure.vector.ollama_embedder import OllamaEmbedder
from presentation.container import build_embedder


def test_ollama_is_the_default_when_a_server_is_configured(monkeypatch):
    monkeypatch.delenv("EMBEDDINGS_PROVIDER", raising=False)
    monkeypatch.setenv("OLLAMA_BASE_URL", "http://ollama:11434")

    assert isinstance(build_embedder(), OllamaEmbedder)


def test_explicit_choice_wins(monkeypatch):
    monkeypatch.setenv("EMBEDDINGS_PROVIDER", "deterministic")
    monkeypatch.setenv("OLLAMA_BASE_URL", "http://ollama:11434")

    assert isinstance(build_embedder(), DeterministicEmbedder)


def test_deterministic_keeps_the_production_dimension(monkeypatch):
    # Un changement de dimension entre développement et production rendrait la
    # collection vectorielle incompatible d'un environnement à l'autre.
    monkeypatch.setenv("EMBEDDINGS_PROVIDER", "deterministic")

    assert build_embedder().dimension() == BgeM3Embedder.DIMENSION


def test_unknown_provider_refuses_to_start(monkeypatch):
    # Se rabattre sur un défaut serait exactement le silence que l'on corrige.
    monkeypatch.setenv("EMBEDDINGS_PROVIDER", "maison")

    with pytest.raises(ValueError):
        build_embedder()


def test_local_model_absent_fails_loudly(monkeypatch):
    monkeypatch.setenv("EMBEDDINGS_PROVIDER", "local")

    # `FlagEmbedding` n'est pas installé ici : autrefois l'adaptateur se taisait
    # et rendait un embedder de hachage.
    with pytest.raises(RuntimeError):
        build_embedder()


def test_no_server_and_no_choice_fails_loudly(monkeypatch):
    monkeypatch.delenv("EMBEDDINGS_PROVIDER", raising=False)
    monkeypatch.delenv("OLLAMA_BASE_URL", raising=False)

    with pytest.raises(RuntimeError):
        build_embedder()
