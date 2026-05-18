"""InMemoryVectorStore — tenant isolation."""
from __future__ import annotations

from datetime import datetime, timezone

from domain.model.rag import RagDocument
from infrastructure.vector import DeterministicEmbedder, InMemoryVectorStore


def test_search_is_tenant_scoped(tenant_id, other_tenant_user):
    store = InMemoryVectorStore()
    emb = DeterministicEmbedder(dim=32)
    doc_a = RagDocument(
        document_id="A",
        tenant_id=tenant_id,
        content="quality of materials",
        metadata={},
        indexed_at=datetime.now(timezone.utc),
    )
    doc_b = RagDocument(
        document_id="B",
        tenant_id=other_tenant_user.tenant.tenant_id,
        content="quality of materials",
        metadata={},
        indexed_at=datetime.now(timezone.utc),
    )
    store.seed(tenant_id, doc_a, emb.embed(["quality of materials"])[0])
    store.seed(
        other_tenant_user.tenant.tenant_id,
        doc_b,
        emb.embed(["quality of materials"])[0],
    )

    hits = store.search(
        tenant_id, emb.embed(["quality of materials"])[0], top_k=5, min_score=0.0
    )
    assert [d.document_id for d, _ in hits] == ["A"]

    hits_other = store.search(
        other_tenant_user.tenant.tenant_id,
        emb.embed(["quality of materials"])[0],
        top_k=5,
        min_score=0.0,
    )
    assert [d.document_id for d, _ in hits_other] == ["B"]


def test_delete_collection_clears_tenant(tenant_id):
    store = InMemoryVectorStore()
    emb = DeterministicEmbedder(dim=32)
    doc = RagDocument(
        document_id="A",
        tenant_id=tenant_id,
        content="x",
        metadata={},
        indexed_at=datetime.now(timezone.utc),
    )
    store.seed(tenant_id, doc, emb.embed(["x"])[0])
    store.delete_collection(tenant_id)
    assert store.search(tenant_id, emb.embed(["x"])[0], top_k=1, min_score=0.0) == []
