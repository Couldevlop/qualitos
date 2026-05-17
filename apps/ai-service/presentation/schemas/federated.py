"""Federated learning API schemas."""
from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel


class FederatedRoundResponseSchema(BaseModel):
    tenant_id: UUID
    round_id: int
    accuracy_delta: float
    samples_used: int
    differential_privacy_epsilon: float
    note: str = (
        "Sprint 1 scaffold — no real cross-tenant training executed yet."
    )
