"""RAG API schemas."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName
from presentation.provider_defaults import DEFAULT_PROVIDER


class RagQueryRequestSchema(BaseModel):
    question: str = Field(..., min_length=1, max_length=2000)
    top_k: int = Field(default=5, ge=1, le=20)
    min_score: float = Field(default=0.5, ge=0.0, le=1.0)
    provider: ProviderName = DEFAULT_PROVIDER


class RagDocumentSchema(BaseModel):
    document_id: str
    score: float
    excerpt: str


class RagQueryResponseSchema(BaseModel):
    answer: str
    documents: list[RagDocumentSchema]
    confidence: float
    confidence_method: str
    explanation: str


class RagSourceSchema(BaseModel):
    """A real document submitted for indexing (ADR 0046).

    `origin` and `licence` are REQUIRED: a fragment nobody can trace back is not
    indexed, and only licences cleared in ADR 0046 are accepted — ISO and other
    copyrighted normative texts are refused, their editors prohibiting both
    redistribution and ingestion into AI systems.
    """

    source_id: str = Field(..., min_length=1, max_length=200)
    title: str = Field(default="", max_length=500)
    content: str = Field(..., min_length=1, max_length=1_000_000)
    origin: str = Field(..., min_length=1, max_length=100)
    licence: str = Field(..., min_length=1, max_length=50)
    version: str = Field(default="", max_length=100)
    url: str = Field(default="", max_length=1000)


class RagIngestRequestSchema(BaseModel):
    sources: list[RagSourceSchema] = Field(..., min_length=1, max_length=200)


class RagIngestResponseSchema(BaseModel):
    documents_read: int
    fragments_indexed: int
    skipped_empty: int
