"""Embedder port — produces dense vectors for RAG."""
from __future__ import annotations

from abc import ABC, abstractmethod


class Embedder(ABC):
    """Computes embeddings. BGE-M3 in the default adapter — pinned by SHA (LLM05)."""

    @abstractmethod
    def embed(self, texts: list[str]) -> list[list[float]]:
        """Return one vector per input string."""

    @abstractmethod
    def dimension(self) -> int:
        """Vector dimension — used for collection creation."""
