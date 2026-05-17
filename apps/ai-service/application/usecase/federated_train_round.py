"""Use case: trigger one round of federated training (scaffold only)."""
from __future__ import annotations

from domain.model.errors import DomainError
from domain.model.tenant import UserContext
from domain.port.federated_client import FederatedClient, FederatedRoundReport


class FederatedTrainRoundUseCase:
    """Opt-in gate + run a federated round. Sprint 1: no actual cross-tenant training."""

    def __init__(self, client: FederatedClient) -> None:
        self._client = client

    def execute(self, user: UserContext) -> FederatedRoundReport:
        tenant_id = user.tenant.tenant_id
        if not self._client.is_opted_in(tenant_id):
            raise DomainError(
                f"Tenant {tenant_id} has not opted in to federated learning"
            )
        return self._client.run_round(tenant_id)
