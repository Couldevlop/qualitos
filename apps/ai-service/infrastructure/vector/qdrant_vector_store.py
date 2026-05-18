"""Qdrant adapter â€” collection-per-tenant.

Lazy import of qdrant-client; falls back to an in-memory adapter when the
package is missing (dev/test).
"""
from __future__ import annotations

import os
from datetime import datetime, timezone
from uuid import UUID

from domain.model.rag import RagDocument
from domain.port.vector_store import VectorStore
from .in_memory_vector_store import InMemoryVectorStore


def _collection_name(tenant_id: UUID) -> str:
    return f"qos_tenant_{str(tenant_id).replace('-', '')}"


class QdrantVectorStore(VectorStore):
    """Per-tenant collections â€” hard isolation."""

    def __init__(
        self,
        url: str | None = None,
        api_key: str | None = None,
        dimension: int = 1024,
        client: object | None = None,
    ) -> None:
        self._url = url or os.environ.get("QDRANT_URL", "http://qdrant:6333")
        self._api_key = api_key or os.environ.get("QDRANT_API_KEY")
        self._dim = dimension
        self._client = client
        self._fallback = InMemoryVectorStore()
        if self._client is None:
            try:  # pragma: no cover
                from qdrant_client import QdrantClient

                self._client = QdrantClient(url=self._url, api_key=self._api_key)
            except Exception:
                self._client = None

    def upsert(self, tenant_id: UUID, documents: list[RagDocument]) -> int:
        if self._client is None:
            return self._fallback.upsert(tenant_id, documents)
        # Pragmatic: store payload + return count. Embedding is computed by
        # the caller and supplied via metadata['embedding'] (json string).
        from qdrant_client.http.models import (  # type: ignore[import-not-found]
            Distance,
            PointStruct,
            VectorParams,
        )

        coll = _collection_name(tenant_id)
        if not self._client.collection_exists(coll):  # type: ignore[union-attr]
            self._client.create_collection(  # type: ignore[union-attr]
                collection_name=coll,
                vectors_config=VectorParams(size=self._dim, distance=Distance.COSINE),
            )
        points = [
            PointStruct(
                id=d.document_id,
                vector=[float(x) for x in d.metadata.get("embedding", "").split(",") if x],
                payload={
                    "content": d.content,
                    "tenant_id": str(d.tenant_id),
                    "indexed_at": d.indexed_at.isoformat(),
                    **{k: v for k, v in d.metadata.items() if k != "embedding"},
                },
            )
            for d in documents
        ]
        self._client.upsert(collection_name=coll, points=points)  # type: ignore[union-attr]
        return len(points)

    def search(
        self,
        tenant_id: UUID,
        query_embedding: list[float],
        top_k: int,
        min_score: float,
    ) -> list[tuple[RagDocument, float]]:
        if self._client is None:
            return self._fallback.search(tenant_id, query_embedding, top_k, min_score)
        coll = _collection_name(tenant_id)
        if not self._client.collection_exists(coll):  # type: ignore[union-attr]
            return []
        hits = self._client.search(  # type: ignore[union-attr]
            collection_name=coll,
            query_vector=query_embedding,
            limit=top_k,
            score_threshold=min_score,
        )
        out: list[tuple[RagDocument, float]] = []
        for h in hits:
            payload = h.payload or {}
            doc = RagDocument(
                document_id=str(h.id),
                tenant_id=tenant_id,
                content=payload.get("content", ""),
                metadata={
                    k: str(v)
                    for k, v in payload.items()
                    if k not in {"content", "tenant_id", "indexed_at"}
                },
                indexed_at=datetime.fromisoformat(
                    payload.get("indexed_at", datetime.now(timezone.utc).isoformat())
                ),
            )
            out.append((doc, float(h.score)))
        return out

    def delete_collection(self, tenant_id: UUID) -> None:
        if self._client is None:
            self._fallback.delete_collection(tenant_id)
            return
        coll = _collection_name(tenant_id)
        try:  # pragma: no cover
            self._client.delete_collection(coll)  # type: ignore[union-attr]
        except Exception:
            pass
