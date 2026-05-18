"""Vector store port â€” Qdrant adapter lives in infrastructure/vector/."""
from __future__ import annotations

from abc import ABC, abstractmethod
from uuid import UUID

from domain.model.rag import RagDocument


class VectorStore(ABC):
    """Tenant-scoped vector store.

    Adapters MUST use one collection per tenant (or apply a hard tenant filter
    on every search) â€” cross-tenant leaks are unacceptable.
    """

    @abstractmethod
    def upsert(self, tenant_id: UUID, documents: list[RagDocument]) -> int:
        """Insert/update documents in the tenant's collection. Returns count."""

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
