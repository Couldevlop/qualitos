"""Audit blanc IA avancé — API schemas (Standards Hub §8.4 onglet 7)."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.completion import ProviderName
from domain.model.mock_audit import MockAuditReport


class AuditClauseSchema(BaseModel):
    clause_code: str = Field(..., min_length=1, max_length=30)
    title: str = Field(..., min_length=1, max_length=500)
    obligation: str = Field(..., description="must | should | may")
    risk: str = Field(..., description="low | medium | high | critical")
    total_requirements: int = Field(..., ge=0, le=10000)
    covered_requirements: int = Field(..., ge=0, le=10000)
    evidence_types: list[str] = Field(default_factory=list, max_length=50)


class MockAuditRequestSchema(BaseModel):
    standard_code: str = Field(..., min_length=1, max_length=100)
    standard_name: str = Field(..., min_length=1, max_length=500)
    industry: str = Field(..., min_length=1, max_length=120)
    clauses: list[AuditClauseSchema] = Field(..., min_length=1, max_length=500)
    language: str = Field(default="fr", min_length=2, max_length=10)
    min_questions: int = Field(default=30, ge=1, le=200)
    max_questions: int = Field(default=100, ge=1, le=200)
    provider: ProviderName = ProviderName.OLLAMA
    max_tokens: int = Field(default=2048, ge=256, le=8192)
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)


class AuditQuestionSchema(BaseModel):
    clause_code: str
    question: str
    rationale: str


class ClauseGapSchema(BaseModel):
    clause_code: str
    title: str
    criticality: str
    coverage_ratio: float
    finding: str
    questions: list[AuditQuestionSchema]


class MockAuditResponseSchema(BaseModel):
    standard_code: str
    standard_name: str
    questions: list[AuditQuestionSchema]
    gaps: list[ClauseGapSchema]
    readiness: float
    major_count: int
    minor_count: int
    observation_count: int
    question_count: int
    provider: str
    tokens_used: int
    latency_ms: int
    pii_findings: list[str]

    @classmethod
    def from_domain(
        cls, report: MockAuditReport, pii_findings: tuple[str, ...]
    ) -> "MockAuditResponseSchema":
        return cls(
            standard_code=report.standard_code,
            standard_name=report.standard_name,
            questions=[
                AuditQuestionSchema(
                    clause_code=q.clause_code,
                    question=q.question,
                    rationale=q.rationale,
                )
                for q in report.questions
            ],
            gaps=[
                ClauseGapSchema(
                    clause_code=g.clause_code,
                    title=g.title,
                    criticality=g.criticality.value,
                    coverage_ratio=g.coverage_ratio,
                    finding=g.finding,
                    questions=[
                        AuditQuestionSchema(
                            clause_code=q.clause_code,
                            question=q.question,
                            rationale=q.rationale,
                        )
                        for q in g.questions
                    ],
                )
                for g in report.gaps
            ],
            readiness=report.readiness,
            major_count=report.major_count,
            minor_count=report.minor_count,
            observation_count=report.observation_count,
            question_count=len(report.questions),
            provider=report.provider,
            tokens_used=report.tokens_used,
            latency_ms=report.latency_ms,
            pii_findings=list(pii_findings),
        )
