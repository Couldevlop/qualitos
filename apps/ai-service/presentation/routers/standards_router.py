"""POST /v1/ai/standards/generate-document (Standards Hub §8.8).

Génère un document normatif complet et multi-sections (Manuel/Politique/
Procédure) pré-rempli avec le contexte tenant. Le résultat est un BROUILLON :
la publication reste soumise à validation humaine côté engine (§18.2 #5).
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.generate_norm_doc import GenerateNormDocRequest
from domain.model.errors import (
    PiiViolationError,
    PromptInjectionError,
    ProviderUnavailableError,
)
from domain.model.normdoc import (
    NormDocKind,
    NormDocSpec,
    SectionSpec,
    TenantProfile,
)
from domain.model.tenant import UserContext
from presentation.container import Container
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
