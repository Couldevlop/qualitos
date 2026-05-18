"""Federated learning client port (CLAUDE.md §12.4).

The scaffold is opt-in per tenant — the application layer asks IsOptedIn
before invoking any federated round. No cross-tenant training in sprint 1.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True, slots=True)
class FederatedRoundReport:
    tenant_id: UUID
    round_id: int
    accuracy_delta: float
    samples_used: int
    differential_privacy_epsilon: float


class FederatedClient(ABC):
    """Flower client wrapper. opt-in gating is enforced before invocation."""

    @abstractmethod
    def is_opted_in(self, tenant_id: UUID) -> bool:
        """Returns True iff the tenant explicitly opted in via configuration."""

    @abstractmethod
    def run_round(self, tenant_id: UUID) -> FederatedRoundReport:
        """Run one round. Raises if the tenant is not opted in."""
