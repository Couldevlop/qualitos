"""FederatedTrainRoundUseCase — opt-in gating."""
from __future__ import annotations

import pytest

from application.usecase.federated_train_round import FederatedTrainRoundUseCase
from domain.model.errors import DomainError
from federated.config import FederatedConfig
from federated.opt_in_federated_client import OptInFederatedClient


def test_non_opted_in_tenant_blocked(user_context):
    client = OptInFederatedClient(FederatedConfig(opted_in_tenants=frozenset()))
    uc = FederatedTrainRoundUseCase(client)
    with pytest.raises(DomainError, match="opted in"):
        uc.execute(user_context)


def test_opted_in_tenant_returns_synthetic_report(user_context):
    client = OptInFederatedClient(
        FederatedConfig(opted_in_tenants=frozenset({user_context.tenant.tenant_id}))
    )
    uc = FederatedTrainRoundUseCase(client)
    report = uc.execute(user_context)
    assert report.tenant_id == user_context.tenant.tenant_id
    assert report.samples_used == 0  # sprint-1 scaffold = no real training
