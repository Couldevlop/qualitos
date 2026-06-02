"""POST /v1/ai/spc/analyze — SPC control limits + the 8 Nelson rules."""
from __future__ import annotations

from fastapi import APIRouter, Depends

from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.spc import SpcAnalyzeRequest, SpcAnalyzeResponse
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/spc", tags=["spc"])
_container = Container.build_default()


@router.post(
    "/analyze",
    response_model=SpcAnalyzeResponse,
    summary="Detect SPC anomalies (8 Nelson rules) in a numeric series",
)
async def analyze(
    payload: SpcAnalyzeRequest,
    user: UserContext = Depends(current_user),
) -> SpcAnalyzeResponse:
    result = _container.spc_detect().execute(
        payload.values,
        user.tenant,
        center=payload.center,
        sigma=payload.sigma,
    )
    return SpcAnalyzeResponse.from_domain(result)
