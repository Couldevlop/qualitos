"""Tenant resolution logic of app/infrastructure/auth.py (ADR 0021).

We exercise the pure helpers (`_parse_trusted_azp`, `_resolve_tenant_id`)
without standing up Keycloak: the JWT signature/aud/iss validation itself is
delegated to python-jose and covered by its own test-suite. What matters here
is OUR contract: tenant_id claim wins; X-Tenant-Id is only trusted for a
service client (`azp`) explicitly allow-listed; everything else fails closed.
"""

from __future__ import annotations

from uuid import UUID

import pytest
from fastapi import HTTPException

from app.infrastructure.auth import _parse_trusted_azp, _resolve_tenant_id

TENANT = "11111111-2222-3333-4444-555555555555"
OTHER = "99999999-8888-7777-6666-555555555555"
TRUSTED = frozenset({"api-quality-engine-vision"})


# --- _parse_trusted_azp -----------------------------------------------------


def test_parse_trusted_azp_empty() -> None:
    assert _parse_trusted_azp("") == frozenset()


def test_parse_trusted_azp_list_with_spaces() -> None:
    assert _parse_trusted_azp(" a , b ,, c ") == frozenset({"a", "b", "c"})


# --- _resolve_tenant_id : claim path ----------------------------------------


def test_tenant_claim_wins() -> None:
    tid = _resolve_tenant_id({"tenant_id": TENANT}, "", frozenset())
    assert tid == UUID(TENANT)


def test_tenant_claim_wins_over_header_even_for_trusted_azp() -> None:
    payload = {"tenant_id": TENANT, "azp": "api-quality-engine-vision"}
    tid = _resolve_tenant_id(payload, OTHER, TRUSTED)
    assert tid == UUID(TENANT)


def test_invalid_tenant_claim_is_401() -> None:
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id({"tenant_id": "not-a-uuid"}, "", frozenset())
    assert exc.value.status_code == 401


# --- _resolve_tenant_id : trusted service path -------------------------------


def test_trusted_azp_with_header_resolves_tenant() -> None:
    payload = {"azp": "api-quality-engine-vision", "sub": "svc"}
    tid = _resolve_tenant_id(payload, TENANT, TRUSTED)
    assert tid == UUID(TENANT)


def test_trusted_client_id_claim_also_accepted() -> None:
    # Some IdPs emit `client_id` instead of `azp` for client_credentials.
    payload = {"client_id": "api-quality-engine-vision"}
    tid = _resolve_tenant_id(payload, TENANT, TRUSTED)
    assert tid == UUID(TENANT)


def test_trusted_azp_without_header_is_401() -> None:
    payload = {"azp": "api-quality-engine-vision"}
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id(payload, "", TRUSTED)
    assert exc.value.status_code == 401
    assert "X-Tenant-Id" in exc.value.detail


def test_trusted_azp_with_invalid_header_is_401() -> None:
    payload = {"azp": "api-quality-engine-vision"}
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id(payload, "not-a-uuid", TRUSTED)
    assert exc.value.status_code == 401


# --- _resolve_tenant_id : fail-closed ----------------------------------------


def test_untrusted_azp_with_header_is_401() -> None:
    payload = {"azp": "some-other-client"}
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id(payload, TENANT, TRUSTED)
    assert exc.value.status_code == 401


def test_no_azp_no_claim_is_401() -> None:
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id({}, TENANT, TRUSTED)
    assert exc.value.status_code == 401


def test_header_never_trusted_when_allowlist_empty() -> None:
    payload = {"azp": "api-quality-engine-vision"}
    with pytest.raises(HTTPException) as exc:
        _resolve_tenant_id(payload, TENANT, frozenset())
    assert exc.value.status_code == 401
