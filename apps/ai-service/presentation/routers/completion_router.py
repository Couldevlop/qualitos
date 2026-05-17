"""POST /v1/ai/complete."""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.complete_text import CompleteTextRequest
from domain.model.errors import (
    PiiViolationError,
    PromptInjectionError,
    ProviderUnavailableError,
)
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.completion import (
    CitationSchema,
    CompletionRequestSchema,
    CompletionResponseSchema,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai", tags=["ai"])
_container = Container.build_default()


@router.post(
    "/complete",
    response_model=CompletionResponseSchema,
    summary="Generate a guarded completion via the AIProvider port",
)
async def complete(
    payload: CompletionRequestSchema,
    user: UserContext = Depends(current_user),
) -> CompletionResponseSchema:
    try:
        result = _container.complete_text().execute(
            user,
            CompleteTextRequest(
                system_prompt=payload.system_prompt,
                user_prompt=payload.user_prompt,
                provider=payload.provider,
                max_tokens=payload.max_tokens,
                temperature=payload.temperature,
                reject_on_pii=payload.reject_on_pii,
            ),
        )
    except PromptInjectionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except PiiViolationError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProviderUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    resp = result.response
    return CompletionResponseSchema(
        text=resp.text,
        provider=resp.provider.value,
        confidence=resp.confidence.value,
        confidence_method=resp.confidence.method,
        citations=[
            CitationSchema(document_id=c.document_id, score=c.score, excerpt=c.excerpt)
            for c in resp.citations
        ],
        pii_findings=list(result.pii_findings),
        injection_score=result.injection_score,
        tokens_used=resp.tokens_used,
        latency_ms=resp.latency_ms,
    )
