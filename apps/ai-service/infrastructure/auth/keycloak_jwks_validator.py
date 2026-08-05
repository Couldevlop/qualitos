"""JWT validation against Keycloak JWKS.

Hardening (OWASP A07):
  * audience checked
  * issuer checked
  * `exp` enforced
  * tenant context mandatory: `tid` claim, or — for a service token from a
    trusted `azp` — the `X-Tenant-Id` header (ADR 0048)
  * key cache TTL 10 minutes
"""
from __future__ import annotations

import os
import time
from dataclasses import dataclass
from typing import Any
from uuid import UUID

try:
    import jwt
    from jwt import PyJWKClient
except Exception:  # pragma: no cover
    jwt = None  # type: ignore[assignment]
    PyJWKClient = None  # type: ignore[assignment]

from domain.model.errors import CrossTenantAccessError, DomainError
from domain.model.tenant import TenantContext, UserContext


@dataclass(slots=True)
class _CachedJwks:
    fetched_at: float
    client: Any


def _parse_trusted_azp(raw: str) -> frozenset[str]:
    """Liste de clients de service de confiance (séparés par des virgules)."""
    return frozenset(part.strip() for part in raw.split(",") if part.strip())


class KeycloakJwksValidator:
    """Validates a Bearer token; returns the UserContext."""

    def __init__(
        self,
        issuer: str | None = None,
        audience: str | None = None,
        jwks_url: str | None = None,
        cache_ttl_s: int = 600,
        trusted_service_azp: frozenset[str] | None = None,
    ) -> None:
        self._issuer = issuer or os.environ.get(
            "KEYCLOAK_ISSUER", "http://keycloak:8080/realms/qualitos"
        )
        self._audience = audience or os.environ.get("KEYCLOAK_AUDIENCE", "qualitos-ai")
        self._jwks_url = jwks_url or f"{self._issuer}/protocol/openid-connect/certs"
        self._cache_ttl = cache_ttl_s
        self._cache: _CachedJwks | None = None
        # Clients de service (`azp`) autorisés à porter le tenant via X-Tenant-Id.
        # Vide par défaut : l'en-tête n'est alors JAMAIS cru.
        self._trusted_azp = (
            trusted_service_azp
            if trusted_service_azp is not None
            else _parse_trusted_azp(os.environ.get("TRUSTED_SERVICE_AZP", ""))
        )

    def _jwks_client(self) -> Any:
        now = time.monotonic()
        if self._cache is None or (now - self._cache.fetched_at) > self._cache_ttl:
            if PyJWKClient is None:
                raise DomainError("PyJWT not installed")
            self._cache = _CachedJwks(fetched_at=now, client=PyJWKClient(self._jwks_url))
        return self._cache.client

    def validate(
        self,
        bearer_token: str,
        correlation_id: str,
        tenant_header: str | None = None,
    ) -> UserContext:
        if jwt is None:
            raise DomainError("PyJWT not installed")
        if not bearer_token:
            raise CrossTenantAccessError("missing bearer token")
        try:
            signing_key = self._jwks_client().get_signing_key_from_jwt(bearer_token).key
            claims = jwt.decode(
                bearer_token,
                signing_key,
                algorithms=["RS256"],
                audience=self._audience,
                issuer=self._issuer,
                options={"require": ["exp", "iat", "sub"]},
            )
        except Exception as exc:
            raise CrossTenantAccessError(f"invalid JWT: {exc}") from exc
        return _build_context(
            claims,
            self._issuer,
            correlation_id,
            tenant_header=tenant_header,
            trusted_azp=self._trusted_azp,
        )


class DevTokenValidator:
    """Test/dev validator â€” accepts a base64-encoded JSON header value.

    Format (header `X-Dev-Claims`):
      {"sub":"<uuid>", "tid":"<uuid>", "roles":["analyst"]}
    NEVER enable in production; the FastAPI router only wires it when the
    QOS_DEV_AUTH env var is truthy.
    """

    def __init__(self, issuer: str = "dev") -> None:
        self._issuer = issuer

    def validate(self, raw_claims_json: str, correlation_id: str) -> UserContext:
        import json

        try:
            claims = json.loads(raw_claims_json)
        except Exception as exc:
            raise CrossTenantAccessError(f"invalid dev claims: {exc}") from exc
        return _build_context(claims, self._issuer, correlation_id)


def _resolve_tenant(
    claims: dict[str, Any], tenant_header: str | None, trusted_azp: frozenset[str]
) -> UUID:
    """Détermine le tenant d'une requête déjà authentifiée (ADR 0048).

    La claim de tenant gagne toujours. À défaut — cas d'un jeton
    ``client_credentials``, qui représente une machine et ne porte aucun tenant —
    l'en-tête ``X-Tenant-Id`` n'est accepté que si le jeton a été émis pour un
    client de service déclaré de confiance. Tout le reste échoue : sans cette
    restriction, n'importe quel porteur d'un jeton valide se déclarerait de
    n'importe quel tenant, ce qui est exactement la faille que le retrait de
    ``X-Dev-Claims`` avait fermée.
    """
    tid_raw = claims.get("tid") or claims.get("tenant_id")
    if tid_raw:
        try:
            return UUID(str(tid_raw))
        except ValueError as exc:
            raise CrossTenantAccessError(f"invalid UUID in JWT: {exc}") from exc

    azp = str(claims.get("azp") or claims.get("client_id") or "")
    if not azp or azp not in trusted_azp:
        raise CrossTenantAccessError("JWT missing 'tid'")
    if not tenant_header:
        raise CrossTenantAccessError("missing X-Tenant-Id header for service token")
    try:
        return UUID(str(tenant_header))
    except ValueError as exc:
        raise CrossTenantAccessError(f"invalid X-Tenant-Id header: {exc}") from exc


def _build_context(
    claims: dict[str, Any],
    issuer: str,
    correlation_id: str,
    tenant_header: str | None = None,
    trusted_azp: frozenset[str] = frozenset(),
) -> UserContext:
    sub_raw = claims.get("sub")
    if not sub_raw:
        raise CrossTenantAccessError("JWT missing 'sub'")
    tenant_id = _resolve_tenant(claims, tenant_header, trusted_azp)
    try:
        user_id = UUID(str(sub_raw))
    except ValueError as exc:
        raise CrossTenantAccessError(f"invalid UUID in JWT: {exc}") from exc
    roles = frozenset(map(str, claims.get("roles", [])))
    return UserContext(
        user_id=user_id,
        tenant=TenantContext(tenant_id=tenant_id, issuer=issuer),
        roles=roles,
        correlation_id=correlation_id,
    )
