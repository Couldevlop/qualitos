"""Vector store port — Qdrant adapter lives in infrastructure/vector/."""
from __future__ import annotations

from abc import ABC, abstractmethod
from uuid import UUID

from domain.model.rag import RagDocument


class VectorStore(ABC):
    """Tenant-scoped vector store.

    Adapters MUST use one collection per tenant (or apply a hard tenant filter
    on every search) — cross-tenant leaks are unacceptable.
    """

    @abstractmethod
    def upsert(
        self,
        tenant_id: UUID,
        documents: list[RagDocument],
        embeddings: list[list[float]],
    ) -> int:
        """Insert/update documents in the tenant's collection. Returns count.

        The vector is passed EXPLICITLY, one per document and in the same order.
        Computing it belongs to the application layer, which owns the embedder;
        an adapter that had to derive it would either duplicate that dependency
        or — as the previous contract did — smuggle the vector through a
        `metadata['embedding']` CSV string that nothing ever produced, leaving
        every stored document unreachable.
        """

    @abstractmethod
    def search(
        self,
        tenant_id: UUID,
        query_embedding: list[float],
        top_k: int,
        min_score: float,
    ) -> list[tuple[RagDocument, float]]:
        """Search the tenant's collection. Returns (doc, score) pairs."""

    @abstractmethod
    def delete_collection(self, tenant_id: UUID) -> None:
        """Tenant offboarding (GDPR right-to-erasure)."""
