"""Les deux lectures d'une carte de contrôle.

- ``POST /v1/ai/spc/analyze`` : le procédé EST-il hors contrôle ?
  (limites + 8 règles de Nelson, ce que la carte montre déjà).
- ``POST /v1/ai/spc/forecast`` : QUAND va-t-il en sortir ? (§6.5) —
  la seule des deux sur laquelle on peut encore agir.
"""
from __future__ import annotations

from fastapi import APIRouter, Depends

from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.spc import (
    SpcAnalyzeRequest,
    SpcAnalyzeResponse,
    SpcForecastRequest,
    SpcForecastResponse,
)
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


@router.post(
    "/forecast",
    response_model=SpcForecastResponse,
    summary="Forecast when the process will leave its SPC control limits",
)
async def forecast(
    payload: SpcForecastRequest,
    user: UserContext = Depends(current_user),
) -> SpcForecastResponse:
    result = _container.spc_forecast().execute(
        payload.values,
        user.tenant,
        horizon=payload.horizon,
        center=payload.center,
        sigma=payload.sigma,
        seasonal_period=payload.seasonal_period,
        threshold=payload.threshold,
    )
    return SpcForecastResponse.from_domain(result)
