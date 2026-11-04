"""Completion API schemas."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName
from presentation.provider_defaults import DEFAULT_PROVIDER


class CompletionRequestSchema(BaseModel):
    system_prompt: str = Field(
        default="You are a QualitOS quality assistant.", max_length=4000
    )
    # Plafond aligné sur qualitos.ai.guard.max-prompt-chars côté moteur
    # (20 000, ADR 0017). Deux bornes différentes sur le même flux donnaient un
    # refus 422 systématique dès 8 000 caractères, soit tout contrat réel.
    user_prompt: str = Field(..., min_length=1, max_length=20000)
    provider: ProviderName = DEFAULT_PROVIDER
    max_tokens: int = Field(default=512, ge=1, le=8192)
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)
    reject_on_pii: bool = False


class CitationSchema(BaseModel):
    document_id: str
    score: float
    excerpt: str


class ProviderStatusSchema(BaseModel):
    name: str
    configured: bool


class ProvidersResponseSchema(BaseModel):
    """Which providers can answer here, and which one a request without ``provider`` gets."""

    default_provider: str
    providers: list[ProviderStatusSchema]


class CompletionResponseSchema(BaseModel):
    text: str
    provider: str
    confidence: float
    confidence_method: str
    citations: list[CitationSchema]
    pii_findings: list[str]
    injection_score: float
    tokens_used: int
    latency_ms: int
