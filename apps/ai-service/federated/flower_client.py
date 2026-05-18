"""Flower client scaffold (no actual training in sprint 1)."""
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class FlowerClientScaffold:
    """Placeholder for a real `flwr.client.NumPyClient`.

    When `flwr` is installed and a tenant has opted in, the runtime would
    instantiate a concrete subclass implementing `get_parameters`, `fit`,
    `evaluate`. We keep the surface minimal in sprint 1.
    """

    server_address: str
    tenant_id: str
    dp_noise_multiplier: float

    def describe(self) -> str:
        return (
            f"Flower client (NOT YET ACTIVE) — server={self.server_address}, "
            f"tenant={self.tenant_id}, dp_noise={self.dp_noise_multiplier}"
        )
