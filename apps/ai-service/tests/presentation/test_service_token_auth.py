"""Chaîne complète d'authentification d'un jeton de service (ADR 0048).

Le test précédent couvre la règle de résolution du tenant ; celui-ci couvre le
câblage, c'est-à-dire précisément ce qui a lâché en préproduction : le jeton
était irréprochable, la règle correcte, mais l'en-tête `X-Tenant-Id` n'était pas
lu par la dépendance FastAPI. On signe donc un vrai jeton RS256 et on le fait
traverser `current_user`, plutôt que d'observer un validateur simulé.
"""
from __future__ import annotations

import asyncio
import time
from types import SimpleNamespace

import jwt as pyjwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import HTTPException

import presentation.security as security
from infrastructure.auth.keycloak_jwks_validator import (
    KeycloakJwksValidator,
    _CachedJwks,
)

ISSUER = "https://keycloak.test/realms/qualitos"
AUDIENCE = "qualitos-ai"
TENANT = "22222222-2222-2222-2222-222222222222"
AUTRE_TENANT = "33333333-3333-3333-3333-333333333333"
SUB = "0000000a-0000-0000-0000-000000000a01"
CLIENT_DE_CONFIANCE = "api-quality-engine-ai"


@pytest.fixture(scope="module")
def keypair():
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    return key, key.public_key()


class _JwksLocal:
    """Fournit la clé publique de la paire de test, comme le ferait Keycloak."""

    def __init__(self, public_key) -> None:
        self._public_key = public_key

    def get_signing_key_from_jwt(self, token: str):
        return SimpleNamespace(key=self._public_key)


def _signe(private_key, **claims) -> str:
    now = int(time.time())
    payload = {
        "iss": ISSUER,
        "aud": AUDIENCE,
        "sub": SUB,
        "iat": now,
        "exp": now + 300,
        **claims,
    }
    return pyjwt.encode(payload, private_key, algorithm="RS256")


def _cable(monkeypatch, public_key, trusted: frozenset[str]) -> None:
    validateur = KeycloakJwksValidator(
        issuer=ISSUER, audience=AUDIENCE, trusted_service_azp=trusted
    )
    validateur._cache = _CachedJwks(
        fetched_at=time.monotonic(), client=_JwksLocal(public_key)
    )
    # Un autre module de test active QOS_DEV_AUTH à l'import : on impose ici le
    # chemin de production, sinon ce test ne prouverait rien.
    monkeypatch.delenv("QOS_DEV_AUTH", raising=False)
    monkeypatch.setattr(security, "_keycloak_validator", validateur)
    monkeypatch.setattr(security, "_dev_validator", None)


def _appelle(token: str, tenant_header: str | None):
    return asyncio.run(
        security.current_user(
            request=None,
            authorization=f"Bearer {token}",
            x_dev_claims=None,
            x_correlation_id="corr-test",
            x_tenant_id=tenant_header,
        )
    )


def test_jeton_de_service_de_confiance_est_accepte(monkeypatch, keypair):
    private_key, public_key = keypair
    _cable(monkeypatch, public_key, frozenset({CLIENT_DE_CONFIANCE}))

    user = _appelle(_signe(private_key, azp=CLIENT_DE_CONFIANCE), TENANT)

    assert str(user.tenant.tenant_id) == TENANT
    assert user.correlation_id == "corr-test"


def test_jeton_de_service_d_un_client_inconnu_est_refuse(monkeypatch, keypair):
    private_key, public_key = keypair
    _cable(monkeypatch, public_key, frozenset({CLIENT_DE_CONFIANCE}))

    with pytest.raises(HTTPException) as exc:
        _appelle(_signe(private_key, azp="client-pirate"), TENANT)

    assert exc.value.status_code == 401


def test_jeton_de_service_sans_en_tete_de_tenant_est_refuse(monkeypatch, keypair):
    private_key, public_key = keypair
    _cable(monkeypatch, public_key, frozenset({CLIENT_DE_CONFIANCE}))

    with pytest.raises(HTTPException) as exc:
        _appelle(_signe(private_key, azp=CLIENT_DE_CONFIANCE), None)

    assert exc.value.status_code == 401


def test_jeton_utilisateur_garde_le_tenant_de_sa_claim(monkeypatch, keypair):
    private_key, public_key = keypair
    _cable(monkeypatch, public_key, frozenset({CLIENT_DE_CONFIANCE}))

    # L'en-tête annonce un autre tenant : il doit être ignoré, sans quoi un
    # utilisateur authentifié pourrait lire les données d'un autre client.
    user = _appelle(_signe(private_key, tid=TENANT), AUTRE_TENANT)

    assert str(user.tenant.tenant_id) == TENANT
