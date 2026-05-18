"""DevTokenValidator — JWT bypass for dev / tests."""
from __future__ import annotations

import json

import pytest

from domain.model.errors import CrossTenantAccessError
from infrastructure.auth import DevTokenValidator


def test_dev_validator_extracts_tenant_and_user():
    v = DevTokenValidator()
    claims = json.dumps(
        {
            "sub": "11111111-1111-1111-1111-111111111111",
            "tid": "22222222-2222-2222-2222-222222222222",
            "roles": ["analyst", "manager"],
        }
    )
    user = v.validate(claims, "corr-1")
    assert str(user.user_id) == "11111111-1111-1111-1111-111111111111"
    assert str(user.tenant.tenant_id) == "22222222-2222-2222-2222-222222222222"
    assert user.has_role("analyst")
    assert not user.has_role("admin")


def test_dev_validator_rejects_missing_tid():
    v = DevTokenValidator()
    with pytest.raises(CrossTenantAccessError):
        v.validate(json.dumps({"sub": "x"}), "corr")


def test_dev_validator_rejects_garbage():
    v = DevTokenValidator()
    with pytest.raises(CrossTenantAccessError):
        v.validate("not-json", "corr")


def test_dev_validator_rejects_bad_uuid():
    v = DevTokenValidator()
    with pytest.raises(CrossTenantAccessError):
        v.validate(json.dumps({"sub": "abc", "tid": "abc"}), "corr")
