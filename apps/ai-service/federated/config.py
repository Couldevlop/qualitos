"""Federated learning configuration.

The set of opted-in tenants is read from QOS_FEDERATED_OPTIN_TENANTS
(comma-separated UUIDs). Default = empty set, meaning the feature is OFF.
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field
from uuid import UUID


@dataclass(frozen=True, slots=True)
class FederatedConfig:
    opted_in_tenants: frozenset[UUID] = field(default_factory=frozenset)
    server_address: str = "federated-server:8888"
    dp_epsilon: float = 1.0
    dp_noise_multiplier: float = 1.1
    rounds_per_day: int = 1

    @classmethod
    def from_env(cls) -> "FederatedConfig":
        raw = os.environ.get("QOS_FEDERATED_OPTIN_TENANTS", "").strip()
        ids: set[UUID] = set()
        if raw:
            for tok in raw.split(","):
                tok = tok.strip()
                if tok:
                    try:
                        ids.add(UUID(tok))
                    except ValueError:
                        continue
        return cls(
            opted_in_tenants=frozenset(ids),
            server_address=os.environ.get(
                "QOS_FEDERATED_SERVER", "federated-server:8888"
            ),
            dp_epsilon=float(os.environ.get("QOS_FEDERATED_DP_EPSILON", "1.0")),
            dp_noise_multiplier=float(
                os.environ.get("QOS_FEDERATED_DP_NOISE", "1.1")
            ),
        )
