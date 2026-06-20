"""Standards Hub AI endpoints.

  * POST /v1/ai/standards/generate-document (§8.8) : document normatif
    multi-sections pré-rempli (BROUILLON, validation humaine côté engine).
  * POST /v1/ai/standards/mock-audit (§8.4 onglet 7) : audit blanc IA —
    30-100 questions ciblées sur les clauses à risque + gap analysis.
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.generate_norm_doc import GenerateNormDocRequest
from application.usecase.mock_audit import MockAuditUseCaseRequest
from domain.model.errors import (
    PiiViolationError,
    PromptInjectionError,
    ProviderUnavailableError,
)
from domain.model.mock_audit import (
    AuditClause,
    MockAuditSpec,
    ObligationLevel,
    RiskLevel,
)
from domain.model.normdoc import (
    NormDocKind,
    NormDocSpec,
    SectionSpec,
    TenantProfile,
)
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.mock_audit import (
    MockAuditRequestSchema,
    MockAuditResponseSchema,
)
from presentation.schemas.normdoc import (
    GenerateNormDocRequestSchema,
    GenerateNormDocResponseSchema,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/standards", tags=["standards"])
_container = Container.build_default()


@router.post(
    "/generate-document",
    response_model=GenerateNormDocResponseSchema,
    summary="Generate a complete multi-section normative document draft (§8.8)",
)
async def generate_document(
    payload: GenerateNormDocRequestSchema,
    user: UserContext = Depends(current_user),
) -> GenerateNormDocResponseSchema:
    try:
        spec = NormDocSpec(
            kind=NormDocKind.from_value(payload.kind),
            standard_code=payload.standard_code,
            standard_name=payload.standard_name,
            tenant_profile=TenantProfile(
                organization_name=payload.tenant_profile.organization_name,
                industry=payload.tenant_profile.industry,
                size=payload.tenant_profile.size,
                language=payload.tenant_profile.language,
                known_processes=tuple(payload.tenant_profile.known_processes),
            ),
            sections=tuple(
                SectionSpec(
                    key=s.key,
                    title=s.title,
                    clauses=tuple(s.clauses),
                    guidance=s.guidance,
                )
                for s in payload.sections
            ),
        )
        result = _container.generate_norm_doc().execute(
            user,
            GenerateNormDocRequest(
                spec=spec,
                provider=payload.provider,
                max_tokens_per_section=payload.max_tokens_per_section,
                temperature=payload.temperature,
            ),
        )
    except (PromptInjectionError, PiiViolationError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProviderUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return GenerateNormDocResponseSchema.from_domain(
        result.draft, result.pii_findings
    )


@router.post(
    "/mock-audit",
    response_model=MockAuditResponseSchema,
    summary="Audit blanc IA — questions ciblées + gap analysis (§8.4 onglet 7)",
)
async def mock_audit(
    payload: MockAuditRequestSchema,
    user: UserContext = Depends(current_user),
) -> MockAuditResponseSchema:
    try:
        spec = MockAuditSpec(
            standard_code=payload.standard_code,
            standard_name=payload.standard_name,
            industry=payload.industry,
            clauses=tuple(
                AuditClause(
                    clause_code=c.clause_code,
                    title=c.title,
                    obligation=ObligationLevel.from_value(c.obligation),
                    risk=RiskLevel.from_value(c.risk),
                    total_requirements=c.total_requirements,
                    covered_requirements=c.covered_requirements,
                    evidence_types=tuple(c.evidence_types),
                )
                for c in payload.clauses
            ),
            language=payload.language,
            min_questions=payload.min_questions,
            max_questions=payload.max_questions,
        )
        result = _container.mock_audit().execute(
            user,
            MockAuditUseCaseRequest(
                spec=spec,
                provider=payload.provider,
                max_tokens=payload.max_tokens,
                temperature=payload.temperature,
            ),
        )
    except (PromptInjectionError, PiiViolationError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProviderUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return MockAuditResponseSchema.from_domain(result.report, result.pii_findings)
