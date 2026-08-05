"""Embeddings servies par Ollama, et choix de l'adaptateur (ADR 0049).

Pourquoi ces tests. Le RAG chargeait BGE-M3 par `FlagEmbedding`, absent de
l'image de production (torch pèse plusieurs gigaoctets). L'adaptateur retombait
alors EN SILENCE sur un embedder de hachage : la recherche sémantique continuait
de répondre, avec des vecteurs qui ne veulent rien dire. Une mauvaise réponse
sans avertissement est pire qu'une panne — surtout sur un module qui cite ses
sources.

Deux exigences vérifiées ici : de vrais vecteurs peuvent venir d'Ollama, déjà
déployé, sans embarquer torch ; et aucun repli silencieux ne subsiste.
"""
from __future__ import annotations

import httpx
import pytest

from domain.model.errors import ProviderUnavailableError
from infrastructure.vector.ollama_embedder import OllamaEmbedder

BASE = "http://ollama:11434"


def _client(handler) -> httpx.Client:
    return httpx.Client(transport=httpx.MockTransport(handler), base_url=BASE)


def test_embeds_a_batch_and_preserves_order():
    seen: list[dict] = []

    def handler(request: httpx.Request) -> httpx.Response:
        import json

        payload = json.loads(request.content)
        seen.append(payload)
        return httpx.Response(200, json={"embeddings": [[0.1, 0.2], [0.3, 0.4]]})

    embedder = OllamaEmbedder(base_url=BASE, model="bge-m3", dimension=2,
                              client=_client(handler))

    vectors = embedder.embed(["premier", "second"])

    assert vectors == [[0.1, 0.2], [0.3, 0.4]]
    assert seen[0]["model"] == "bge-m3"
    assert seen[0]["input"] == ["premier", "second"]


def test_accepts_the_single_embedding_shape():
    # Selon la version, Ollama répond `embeddings` (lot) ou `embedding` (unitaire).
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"embedding": [1.0, 2.0]})

    embedder = OllamaEmbedder(base_url=BASE, dimension=2, client=_client(handler))

    assert embedder.embed(["seul"]) == [[1.0, 2.0]]


def test_empty_batch_makes_no_call():
    def handler(request: httpx.Request) -> httpx.Response:  # pragma: no cover
        raise AssertionError("aucun appel ne doit partir pour un lot vide")

    embedder = OllamaEmbedder(base_url=BASE, client=_client(handler))

    assert embedder.embed([]) == []


def test_unreachable_server_fails_loudly():
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("connexion refusée")

    embedder = OllamaEmbedder(base_url=BASE, client=_client(handler))

    with pytest.raises(ProviderUnavailableError):
        embedder.embed(["texte"])


def test_missing_model_fails_loudly():
    # Modèle non tiré sur la machine : Ollama répond 404. Le RAG doit s'arrêter
    # là, et surtout pas fabriquer des vecteurs de repli.
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"error": "model 'bge-m3' not found"})

    embedder = OllamaEmbedder(base_url=BASE, client=_client(handler))

    with pytest.raises(ProviderUnavailableError):
        embedder.embed(["texte"])


def test_unusable_response_fails_loudly():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"rien": "d'exploitable"})

    embedder = OllamaEmbedder(base_url=BASE, client=_client(handler))

    with pytest.raises(ProviderUnavailableError):
        embedder.embed(["texte"])


def test_short_batch_answer_fails_loudly():
    # Moins de vecteurs que de textes : les fragments seraient indexés avec le
    # vecteur d'un autre. Mieux vaut refuser.
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"embeddings": [[0.1, 0.2]]})

    embedder = OllamaEmbedder(base_url=BASE, dimension=2, client=_client(handler))

    with pytest.raises(ProviderUnavailableError):
        embedder.embed(["un", "deux"])


def test_dimension_follows_configuration():
    embedder = OllamaEmbedder(base_url=BASE, dimension=1024)
    assert embedder.dimension() == 1024
