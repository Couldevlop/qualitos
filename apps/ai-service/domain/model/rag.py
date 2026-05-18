"""RAG (Retrieval Augmented Generation) domain objects."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from uuid import UUID

from .tenant import TenantContext


@dataclass(frozen=True, slots=True)
class RagDocument:
    """An indexed document chunk inside the tenant's vector collection."""

    document_id: str
    tenant_id: UUID
    content: str
    metadata: dict[str, str]
    indexed_at: datetime

    def __post_init__(self) -> None:
        if not self.document_id:
            raise ValueError("document_id required")
        if not self.content or not self.content.strip():
            raise ValueError("content required")


@dataclass(frozen=True, slots=True)
class RagQuery:
    """A RAG query scoped to one tenant — no cross-tenant access ever."""

    question: str
    tenant: TenantContext
    top_k: int = 5
    min_score: float = 0.5

    def __post_init__(self) -> None:
        if self.top_k < 1 or self.top_k > 50:
            raise ValueError("top_k must be 1..50")
        if not 0.0 <= self.min_score <= 1.0:
            raise ValueError("min_score must be 0..1")
        if not self.question.strip():
            raise ValueError("question required")


@dataclass(frozen=True, slots=True)
class RagResult:
    """RAG result with grounded answer + citations."""

    answer: str
    documents: tuple[RagDocument, ...]
    scores: tuple[float, ...]
    explanation: str = field(default="")

    def __post_init__(self) -> None:
        if len(self.documents) != len(self.scores):
            raise ValueError("documents/scores length mismatch")
