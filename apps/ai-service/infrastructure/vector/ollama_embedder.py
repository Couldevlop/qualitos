"""Embeddings servies par Ollama (ADR 0049).

Le RAG chargeait BGE-M3 dans le processus, par `FlagEmbedding`. Cette voie
suppose torch — plusieurs gigaoctets — que l'image de production n'embarque pas ;
l'adaptateur retombait donc en silence sur un embedder de hachage, et la
recherche sémantique répondait avec des vecteurs dénués de sens.

Ollama est déjà déployé et sait servir `bge-m3`. On lui délègue le calcul : même
modèle, aucun poids dans l'image, un seul endroit où le tirer. Et surtout, plus
aucun repli silencieux : toute défaillance remonte en
:class:`ProviderUnavailableError`, que la couche de présentation traduit en 503.
Un module qui cite ses sources doit s'arrêter plutôt que d'en inventer.
"""
from __future__ import annotations

import os

import httpx

from domain.model.errors import ProviderUnavailableError
from domain.port.embedder import Embedder
from infrastructure.providers._allowlist import assert_host_allowed

#: BGE-M3 produit des vecteurs de 1024 dimensions.
DEFAULT_DIMENSION = 1024
DEFAULT_MODEL = "bge-m3"
#: Un lot d'ingestion documentaire est bien plus long qu'une requête de chat.
DEFAULT_TIMEOUT_S = 120.0


class OllamaEmbedder(Embedder):
    """Appelle `POST /api/embed` d'un serveur Ollama."""

    def __init__(
        self,
        base_url: str | None = None,
        model: str | None = None,
        dimension: int | None = None,
        timeout_s: float | None = None,
        client: httpx.Client | None = None,
    ) -> None:
        self._base_url = (base_url or os.environ.get("OLLAMA_BASE_URL")
                          or "http://ollama:11434").rstrip("/")
        # Même garde SSRF que les autres sorties du service (OWASP A10).
        assert_host_allowed(self._base_url)
        self._model = model or os.environ.get("EMBEDDINGS_MODEL") or DEFAULT_MODEL
        self._dimension = dimension or int(
            os.environ.get("EMBEDDINGS_DIMENSION", DEFAULT_DIMENSION)
        )
        timeout = timeout_s or float(
            os.environ.get("EMBEDDINGS_TIMEOUT_S", DEFAULT_TIMEOUT_S)
        )
        self._client = client or httpx.Client(timeout=timeout)

    def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        try:
            resp = self._client.post(
                f"{self._base_url}/api/embed",
                json={"model": self._model, "input": texts},
            )
            resp.raise_for_status()
            data = resp.json()
        except (httpx.HTTPError, httpx.TimeoutException, ValueError) as exc:
            raise ProviderUnavailableError(
                f"Service d'embeddings indisponible ({self._model}) : {exc}"
            ) from exc

        vectors = data.get("embeddings")
        if vectors is None and data.get("embedding") is not None:
            # Réponse unitaire, selon la version du serveur.
            vectors = [data["embedding"]]
        if not isinstance(vectors, list) or len(vectors) != len(texts):
            # Moins de vecteurs que de textes : indexer quand même associerait un
            # fragment au vecteur d'un autre, faute silencieuse et durable.
            raise ProviderUnavailableError(
                f"Réponse d'embeddings inexploitable ({self._model}) : "
                f"{len(texts)} texte(s) envoyé(s)"
            )
        return [[float(x) for x in vector] for vector in vectors]

    def dimension(self) -> int:
        return self._dimension
