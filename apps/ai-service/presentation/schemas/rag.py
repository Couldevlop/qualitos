"""RAG API schemas."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName


class RagQueryRequestSchema(BaseModel):
    question: str = Field(..., min_length=1, max_length=2000)
    top_k: int = Field(default=5, ge=1, le=20)
    min_score: float = Field(default=0.5, ge=0.0, le=1.0)
    provider: ProviderName = ProviderName.OLLAMA


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
