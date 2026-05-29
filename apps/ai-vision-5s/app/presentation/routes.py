"""FastAPI router for /v1/vision/5s/* endpoints."""

from __future__ import annotations

import logging
from typing import Annotated

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status
from fastapi.responses import JSONResponse
from slowapi import Limiter
from slowapi.util import get_remote_address

from app.application.use_cases import AnalyzeImageUseCase, ScoreImageUseCase
from app.infrastructure.auth import TenantContext, require_tenant_context
from app.infrastructure.backend_factory import build_backend
from app.infrastructure.image_safety import ImageRejected, sanitize
from app.presentation.dtos import (
    AnalysisResponse,
    FindingResponse,
    ProblemDetail,
    ScoreResponse,
)

_LOG = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/vision/5s", tags=["vision-5s"])

# Rate limit (OWASP LLM04 model DoS) — 60 requests / minute / IP.
# Per-tenant quotas should be added in P5 once API Gateway is in place.
limiter = Limiter(key_func=get_remote_address, default_limits=["60/minute"])

# Composition root: pick ONNX backend if a model is configured & loadable,
# otherwise fall back transparently to the deterministic stub.
_backend = build_backend()
_analyze_uc = AnalyzeImageUseCase(_backend)
_score_uc = ScoreImageUseCase(_backend)


def _problem(status_code: int, title: str, detail: str | None = None) -> JSONResponse:
    body = ProblemDetail(
        type=f"https://qualitos.local/errors/{title.lower().replace(' ', '-')}",
        title=title,
        status=status_code,
        detail=detail,
    )
    return JSONResponse(
        status_code=status_code,
        content=body.model_dump(),
        media_type="application/problem+json",
    )


@router.post(
    "/analyze",
    response_model=AnalysisResponse,
    responses={400: {"model": ProblemDetail}, 401: {"model": ProblemDetail}},
)
async def analyze(
    ctx: Annotated[TenantContext, Depends(require_tenant_context)],
    image: UploadFile = File(...),
) -> AnalysisResponse:
    """Full 5S analysis on the uploaded image."""
    try:
        content = await image.read()
        safe = sanitize(content)
    except ImageRejected as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    result = _analyze_uc.execute(safe.bytes, safe.width, safe.height)

    _LOG.info(
        "5s.analyze tenant=%s sub=%s sha=%s overall=%s findings=%d",
        ctx.tenant_id, ctx.subject, result.image_sha256[:12], result.score.overall,
        len(result.findings),
    )

    return AnalysisResponse(
        image_sha256=result.image_sha256,
        width=result.width,
        height=result.height,
        score=ScoreResponse(**result.score.as_dict()),
        findings=[
            FindingResponse(
                pillar=f.pillar.value,
                description=f.description,
                severity=f.severity.value,
                confidence=f.confidence,
                bbox=f.bbox,
            ) for f in result.findings
        ],
    )


@router.post(
    "/score",
    response_model=ScoreResponse,
    responses={400: {"model": ProblemDetail}},
)
async def score(
    ctx: Annotated[TenantContext, Depends(require_tenant_context)],
    image: UploadFile = File(...),
) -> ScoreResponse:
    """Lightweight scoring endpoint — no findings, just the per-pillar grades."""
    try:
        content = await image.read()
        safe = sanitize(content)
    except ImageRejected as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    s = _score_uc.execute(safe.bytes, safe.width, safe.height)

    _LOG.info("5s.score tenant=%s sub=%s overall=%s", ctx.tenant_id, ctx.subject, s.overall)
    return ScoreResponse(**s.as_dict())
