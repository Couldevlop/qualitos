"""Normative-document generation API schemas (§8.8)."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName
from domain.model.normdoc import NormDocDraft


class TenantProfileSchema(BaseModel):
    organization_name: str = Field(..., min_length=1, max_length=250)
    industry: str = Field(..., min_length=1, max_length=120)
    size: str = Field(..., min_length=1, max_length=120)
    language: str = Field(default="fr", min_length=2, max_length=10)
    known_processes: list[str] = Field(default_factory=list, max_length=50)


class SectionSpecSchema(BaseModel):
    key: str = Field(..., min_length=1, max_length=120)
    title: str = Field(..., min_length=1, max_length=300)
    clauses: list[str] = Field(default_factory=list, max_length=50)
    guidance: str = Field(default="", max_length=2000)


class GenerateNormDocRequestSchema(BaseModel):
    kind: str = Field(..., description="manual | policy | procedure")
    standard_code: str = Field(..., min_length=1, max_length=100)
    standard_name: str = Field(..., min_length=1, max_length=500)
    tenant_profile: TenantProfileSchema
    sections: list[SectionSpecSchema] = Field(..., min_length=1, max_length=40)
    provider: ProviderName = ProviderName.OLLAMA
    max_tokens_per_section: int = Field(default=512, ge=64, le=4096)
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)


class GeneratedSectionSchema(BaseModel):
    key: str
    title: str
    clauses: list[str]
    body_markdown: str


class GenerateNormDocResponseSchema(BaseModel):
    kind: str
    standard_code: str
    standard_name: str
    title: str
    sections: list[GeneratedSectionSchema]
    markdown: str
    provider: str
    tokens_used: int
    latency_ms: int
    pii_findings: list[str]

    @classmethod
    def from_domain(
        cls, draft: NormDocDraft, pii_findings: tuple[str, ...]
    ) -> "GenerateNormDocResponseSchema":
        return cls(
            kind=draft.kind.value,
            standard_code=draft.standard_code,
            standard_name=draft.standard_name,
            title=draft.title,
            sections=[
                GeneratedSectionSchema(
                    key=s.key,
                    title=s.title,
                    clauses=list(s.clauses),
                    body_markdown=s.body_markdown,
                )
                for s in draft.sections
            ],
            markdown=draft.to_markdown(),
            provider=draft.provider,
            tokens_used=draft.tokens_used,
            latency_ms=draft.latency_ms,
            pii_findings=list(pii_findings),
        )
