"""BGE-M3 embedder adapter.

The model SHA is pinned at deploy time via the `BGE_M3_REVISION` env var
(supply-chain hygiene â€” OWASP LLM05).
"""
from __future__ import annotations

import hashlib
import os

from domain.model.errors import ProviderUnavailableError
from domain.port.embedder import Embedder

DEFAULT_REVISION = "5617a9f61b028005a4858fdac845db406aefb181"  # placeholder pin


class BgeM3Embedder(Embedder):
    """Loads BAAI/bge-m3 at the pinned revision."""

    DIMENSION = 1024

    def __init__(self, revision: str | None = None, model: object | None = None) -> None:
        self._revision = revision or os.environ.get("BGE_M3_REVISION", DEFAULT_REVISION)
        self._model = model  # injection point for tests
        if self._model is None:
            try:  # pragma: no cover - optional in dev
                from FlagEmbedding import BGEM3FlagModel  # type: ignore[import-not-found]

                self._model = BGEM3FlagModel(
                    "BAAI/bge-m3", use_fp16=True, revision=self._revision
                )
            except Exception:
                self._model = None

    def is_loaded(self) -> bool:
        """Le modèle est-il réellement chargé ? Interrogé au démarrage (ADR 0049)."""
        return self._model is not None

    def embed(self, texts: list[str]) -> list[list[float]]:
        if self._model is None:
            # Plus de repli silencieux : rendre des vecteurs de hachage laissait
            # le RAG répondre avec des voisinages arbitraires, sans que rien ne
            # le signale. Le choix de l'embedder appartient à la configuration
            # (EMBEDDINGS_PROVIDER), pas à un rattrapage discret.
            raise ProviderUnavailableError(
                "Modèle BGE-M3 non chargé : installer l'extra « ml » ou "
                "configurer EMBEDDINGS_PROVIDER=ollama."
            )
        out = self._model.encode(texts)["dense_vecs"]  # type: ignore[index]
        return [list(map(float, v)) for v in out]

    def dimension(self) -> int:
        return self.DIMENSION


class DeterministicEmbedder(Embedder):
    """Stable hash-based embedder for tests."""

    def __init__(self, dim: int = 64) -> None:
        self._dim = dim

    def embed(self, texts: list[str]) -> list[list[float]]:
        out: list[list[float]] = []
        for t in texts:
            digest = hashlib.sha256(t.encode("utf-8")).digest()
            # Spread digest bytes across `dim` floats in [-1, 1].
            v = [((digest[i % len(digest)] / 255.0) * 2.0 - 1.0) for i in range(self._dim)]
            out.append(v)
        return out

    def dimension(self) -> int:
        return self._dim
