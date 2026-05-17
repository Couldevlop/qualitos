"""POST /v1/ai/federated/round â€” opt-in only."""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from domain.model.errors import DomainError
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.federated import FederatedRoundResponseSchema
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/federated", tags=["federated"])
_container = Container.build_default()


@router.post(
    "/round",
    response_model=FederatedRoundResponseSchema,
    summary="Run one federated learning round (sprint 1 scaffold, opt-in only)",
)
async def run_round(
    user: UserContext = Depends(current_user),
) -> FederatedRoundResponseSchema:
    try:
        report = _container.federated_round().execute(user)
    except DomainError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    return FederatedRoundResponseSchema(
        tenant_id=report.tenant_id,
        round_id=report.round_id,
        accuracy_delta=report.accuracy_delta,
        samples_used=report.samples_used,
        differential_privacy_epsilon=report.differential_privacy_epsilon,
    )
