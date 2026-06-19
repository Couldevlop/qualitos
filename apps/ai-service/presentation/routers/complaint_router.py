"""POST /v1/ai/complaints/analyze — NLP des réclamations (§4.9, §12.1).

Sentiment lexical + classification par termes-graines + détection critique. Calcul
pur (aucun provider) ; le tenant provient du JWT (jamais du body, règle 18.2 #2).
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from domain.model.tenant import UserContext
from domain.service.ml_backends import MlBackendUnavailableError
from presentation.container import Container
from presentation.schemas.complaint import ComplaintAnalyzeRequest, ComplaintAnalyzeResponse
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/complaints", tags=["complaints"])
_container = Container.build_default()


@router.post(
    "/analyze",
    response_model=ComplaintAnalyzeResponse,
    summary="Analyze customer complaints (sentiment, category, criticality)",
)
async def analyze(
    payload: ComplaintAnalyzeRequest,
    user: UserContext = Depends(current_user),
) -> ComplaintAnalyzeResponse:
    try:
        result = _container.complaint_analyze().execute(
            payload.texts, user.tenant, categories=payload.categories,
            backend=payload.backend,
        )
    except (ValueError, MlBackendUnavailableError) as exc:
        # Backend BERT indisponible (extra ml absent) ou entrée invalide → 422 clair.
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return ComplaintAnalyzeResponse.from_domain(result)
