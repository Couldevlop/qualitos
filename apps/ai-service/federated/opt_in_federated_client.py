"""FederatedClient implementation that enforces opt-in gating."""
from __future__ import annotations

from uuid import UUID

from domain.model.errors import DomainError
from domain.port.federated_client import FederatedClient, FederatedRoundReport
from federated.config import FederatedConfig


class OptInFederatedClient(FederatedClient):
    """Hard gate â€” refuses any tenant not in the opted-in set."""

    def __init__(self, config: FederatedConfig | None = None) -> None:
        self._config = config or FederatedConfig.from_env()

    def is_opted_in(self, tenant_id: UUID) -> bool:
        return tenant_id in self._config.opted_in_tenants

    def run_round(self, tenant_id: UUID) -> FederatedRoundReport:
        if not self.is_opted_in(tenant_id):
            raise DomainError(
                f"Federated learning disabled for tenant {tenant_id} (opt-in required)"
            )
        # Sprint 1: no real training happens. Return a synthetic report so
        # the use case + API contracts can be exercised end-to-end.
        return FederatedRoundReport(
            tenant_id=tenant_id,
            round_id=0,
            accuracy_delta=0.0,
            samples_used=0,
            differential_privacy_epsilon=self._config.dp_epsilon,
        )
