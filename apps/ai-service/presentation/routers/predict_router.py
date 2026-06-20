"""Predictive endpoints (§6.5) — KPI forecast, supplier risk, NC clustering.

Là où les concurrents montrent du passé, QualitOS prédit l'avenir — et chaque
prédiction est explicable (drivers, pente, top-termes) et actionnable.
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from domain.model.tenant import UserContext
from domain.service.ml_backends import MlBackendUnavailableError
from presentation.container import Container
from presentation.schemas.predict import (
    KpiForecastRequest,
    KpiForecastResponse,
    NcClusterRequest,
    NcClusteringResponse,
    SupplierRiskRequest,
    SupplierRiskResponse,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/predict", tags=["predict"])
_container = Container.build_default()


@router.post(
    "/kpi",
    response_model=KpiForecastResponse,
    summary="Forecast a KPI series and the probability of reaching its target",
)
async def forecast_kpi(
    payload: KpiForecastRequest,
    user: UserContext = Depends(current_user),
) -> KpiForecastResponse:
    try:
        result = _container.kpi_forecast().execute(
            payload.values, payload.target, user.tenant,
            horizon=payload.horizon, direction=payload.direction,
            seasonal_period=payload.seasonal_period, model=payload.model,
        )
    except (ValueError, MlBackendUnavailableError) as exc:
        # Backend lourd indisponible (extra ml absent) ou entrée invalide → 422 clair.
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return KpiForecastResponse.from_domain(result)


@router.post(
    "/supplier-risk",
    response_model=SupplierRiskResponse,
    summary="Score supplier risk (0-100) with explainable drivers",
)
async def supplier_risk(
    payload: SupplierRiskRequest,
    user: UserContext = Depends(current_user),
) -> SupplierRiskResponse:
    try:
        result = _container.supplier_risk().execute(payload.features, user.tenant)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return SupplierRiskResponse.from_domain(result)


@router.post(
    "/nc-clusters",
    response_model=NcClusteringResponse,
    summary="Cluster similar non-conformities (recurrent patterns)",
)
async def nc_clusters(
    payload: NcClusterRequest,
    user: UserContext = Depends(current_user),
) -> NcClusteringResponse:
    try:
        result = _container.nc_cluster().execute(
            payload.texts, user.tenant,
            threshold=payload.threshold, min_samples=payload.min_samples,
            method=payload.method,
        )
    except (ValueError, MlBackendUnavailableError) as exc:
        # Backend lourd indisponible (extra ml absent) ou entrée invalide → 422 clair.
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return NcClusteringResponse.from_domain(result)
