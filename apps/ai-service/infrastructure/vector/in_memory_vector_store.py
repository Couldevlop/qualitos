"""In-memory VectorStore used by tests."""
from __future__ import annotations

import math
from uuid import UUID

from domain.model.rag import RagDocument
from domain.port.vector_store import VectorStore


class InMemoryVectorStore(VectorStore):
    """Per-tenant in-memory store (cosine similarity)."""

    def __init__(self) -> None:
        self._docs: dict[UUID, list[tuple[RagDocument, list[float]]]] = {}

    def seed(self, tenant_id: UUID, doc: RagDocument, embedding: list[float]) -> None:
        self._docs.setdefault(tenant_id, []).append((doc, embedding))

    def upsert(self, tenant_id: UUID, documents: list[RagDocument]) -> int:
        # Tests inject embeddings separately via seed().
        # In normal use, the use-case would compute embeddings first and
        # call seed() from the application layer; this adapter still works.
        bucket = self._docs.setdefault(tenant_id, [])
        for d in documents:
            bucket.append((d, [0.0]))
        return len(documents)

    def search(
        self,
        tenant_id: UUID,
        query_embedding: list[float],
        top_k: int,
        min_score: float,
    ) -> list[tuple[RagDocument, float]]:
        bucket = self._docs.get(tenant_id, [])
        scored = []
        for doc, emb in bucket:
            score = _cosine(query_embedding, emb)
            if score >= min_score:
                scored.append((doc, score))
        scored.sort(key=lambda x: x[1], reverse=True)
        return scored[:top_k]

    def delete_collection(self, tenant_id: UUID) -> None:
        self._docs.pop(tenant_id, None)


def _cosine(a: list[float], b: list[float]) -> float:
    n = min(len(a), len(b))
    if n == 0:
        return 0.0
    dot = sum(a[i] * b[i] for i in range(n))
    na = math.sqrt(sum(x * x for x in a[:n])) or 1.0
    nb = math.sqrt(sum(x * x for x in b[:n])) or 1.0
    return dot / (na * nb)
