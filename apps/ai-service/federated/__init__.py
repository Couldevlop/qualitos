"""Federated learning scaffold (CLAUDE.md §12.4).

Sprint 1: opt-in gating + tests proving the gate, plus an ADR. No actual
cross-tenant rounds are executed.
"""
from .config import FederatedConfig
from .flower_client import FlowerClientScaffold
from .opt_in_federated_client import OptInFederatedClient

__all__ = ["FederatedConfig", "FlowerClientScaffold", "OptInFederatedClient"]
