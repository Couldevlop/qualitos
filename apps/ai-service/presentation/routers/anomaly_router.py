"""POST /v1/ai/anomaly/detect — anomalies non-supervisées multivariées (§3.4, §12.1).

Isolation Forest ou reconstruction par ACP (auto-encodeur linéaire). Calcul pur
NumPy (aucun provider) ; le tenant provient du JWT (jamais du body, règle 18.2 #2).
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.anomaly import (
    AnomalyDetectRequest,
    AnomalyDetectResponse,
    AnomalyExplainRequest,
    AnomalyExplainResponse,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/anomaly", tags=["anomaly"])
_container = Container.build_default()


@router.post(
    "/detect",
    response_model=AnomalyDetectResponse,
    summary="Detect multivariate anomalies (Isolation Forest / PCA reconstruction)",
)
async def detect(
    payload: AnomalyDetectRequest,
    user: UserContext = Depends(current_user),
) -> AnomalyDetectResponse:
    result = _container.anomaly_detect().execute(
        payload.samples,
        user.tenant,
        method=payload.method,
        contamination=payload.contamination,
        threshold=payload.threshold,
        seed=payload.seed,
        n_trees=payload.n_trees,
        sample_size=payload.sample_size,
        n_components=payload.n_components,
    )
    return AnomalyDetectResponse.from_domain(result)


@router.post(
    "/explain",
    response_model=AnomalyExplainResponse,
    summary="Explain a sample's anomaly score by feature (Kernel SHAP)",
)
async def explain(
    payload: AnomalyExplainRequest,
    user: UserContext = Depends(current_user),
) -> AnomalyExplainResponse:
    try:
        result = _container.anomaly_explain().execute(
            payload.samples,
            payload.index,
            user.tenant,
            seed=payload.seed,
            n_trees=payload.n_trees,
            sample_size=payload.sample_size,
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return AnomalyExplainResponse.from_domain(result)
