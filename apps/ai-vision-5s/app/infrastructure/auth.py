"""JWT validation against Keycloak JWKS (CLAUDE.md §11 / OWASP A07).

We support two modes:

- production: fetch + cache the JWKS from `KEYCLOAK_JWKS_URI`,
  validate `iss`, `aud`, `exp`, `nbf`, signature using RS256.
- test/dev (`AUTH_BYPASS=true`): accept any token and synthesize a tenant
  context. THIS MUST NOT be enabled in production — checked at startup.
"""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from typing import Optional
from uuid import UUID

import httpx
from fastapi import Header, HTTPException, status
from jose import jwt
from jose.exceptions import JWTError

_LOG = logging.getLogger(__name__)

_JWKS_CACHE: Optional[dict] = None
_JWKS_URI = os.getenv("KEYCLOAK_JWKS_URI", "")
_ISSUER = os.getenv("KEYCLOAK_ISSUER", "")
_AUDIENCE = os.getenv("KEYCLOAK_AUDIENCE", "api-ai-vision-5s")
_AUTH_BYPASS = os.getenv("AUTH_BYPASS", "false").lower() == "true"


@dataclass(frozen=True)
class TenantContext:
    tenant_id: UUID
    subject: str
    roles: tuple[str, ...]


def _fetch_jwks() -> dict:
    global _JWKS_CACHE
    if _JWKS_CACHE is not None:
        return _JWKS_CACHE
    if not _JWKS_URI:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="KEYCLOAK_JWKS_URI not configured",
        )
    try:
        resp = httpx.get(_JWKS_URI, timeout=3.0)
        resp.raise_for_status()
        _JWKS_CACHE = resp.json()
    except Exception as exc:  # pragma: no cover
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="JWKS unavailable",
        ) from exc
    return _JWKS_CACHE


def require_tenant_context(authorization: str = Header(default="")) -> TenantContext:
    """FastAPI dependency: extract & validate a JWT, return the tenant context.

    OWASP rule: `tenant_id` is sourced from the JWT claim, NEVER from the body.
    """
    if _AUTH_BYPASS:
        # Test/dev shortcut. The Dockerfile sets AUTH_BYPASS=false for prod and
        # main() asserts it at startup.
        return TenantContext(
            tenant_id=UUID("00000000-0000-0000-0000-000000000001"),
            subject="bypass",
            roles=("QUALITY_MANAGER",),
        )

    if not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Bearer token required")
    token = authorization.split(" ", 1)[1].strip()
    if not token:
        raise HTTPException(status_code=401, detail="Empty bearer token")

    try:
        jwks = _fetch_jwks()
        payload = jwt.decode(
            token,
            jwks,
            algorithms=["RS256"],
            audience=_AUDIENCE,
            issuer=_ISSUER or None,
            options={"require": ["exp", "iat"]},
        )
    except JWTError as exc:
        raise HTTPException(status_code=401, detail="Invalid token") from exc

    tenant_claim = payload.get("tenant_id")
    if not tenant_claim:
        raise HTTPException(status_code=401, detail="Missing tenant_id claim")
    try:
        tenant_id = UUID(str(tenant_claim))
    except ValueError as exc:
        raise HTTPException(status_code=401, detail="Invalid tenant_id claim") from exc

    roles = tuple(payload.get("realm_access", {}).get("roles", []))
    return TenantContext(
        tenant_id=tenant_id,
        subject=str(payload.get("sub", "")),
        roles=roles,
    )


def assert_production_safe() -> None:
    """Called from main: refuse to boot in prod profile with AUTH_BYPASS=true."""
    if _AUTH_BYPASS and os.getenv("APP_PROFILE", "").lower() == "prod":
        raise RuntimeError("AUTH_BYPASS=true cannot run with APP_PROFILE=prod")
