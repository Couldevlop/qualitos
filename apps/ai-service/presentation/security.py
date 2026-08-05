"""FastAPI security dependencies â€” JWT + correlation id + role guard."""
from __future__ import annotations

import os
import uuid
from typing import Callable

from fastapi import Depends, Header, HTTPException, Request, status

from domain.model.errors import CrossTenantAccessError
from domain.model.tenant import UserContext
from infrastructure.auth import DevTokenValidator, KeycloakJwksValidator

# Lazily-instantiated singletons.
_keycloak_validator: KeycloakJwksValidator | None = None
_dev_validator: DevTokenValidator | None = None


def _get_validators() -> tuple[KeycloakJwksValidator | None, DevTokenValidator | None]:
    global _keycloak_validator, _dev_validator
    if os.environ.get("QOS_DEV_AUTH", "").lower() in {"1", "true", "yes"}:
        if _dev_validator is None:
            _dev_validator = DevTokenValidator()
        return None, _dev_validator
    if _keycloak_validator is None:
        _keycloak_validator = KeycloakJwksValidator()
    return _keycloak_validator, None


async def current_user(
    request: Request,
    authorization: str | None = Header(default=None),
    x_dev_claims: str | None = Header(default=None, alias="X-Dev-Claims"),
    x_correlation_id: str | None = Header(default=None, alias="X-Correlation-Id"),
    x_tenant_id: str | None = Header(default=None, alias="X-Tenant-Id"),
) -> UserContext:
    """Resolve the user from JWT (Keycloak) or X-Dev-Claims in dev mode.

    Appels serveur-à-serveur (ADR 0048) : un jeton `client_credentials` ne porte
    aucun tenant ; l'en-tête `X-Tenant-Id` le fournit, et n'est cru que si l'`azp`
    du jeton figure dans `TRUSTED_SERVICE_AZP`. La claim de tenant, quand elle
    existe, l'emporte toujours.
    """
    corr = x_correlation_id or str(uuid.uuid4())
    kc, dev = _get_validators()
    try:
        if dev is not None:
            if not x_dev_claims:
                raise HTTPException(status_code=401, detail="missing X-Dev-Claims")
            return dev.validate(x_dev_claims, corr)
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(status_code=401, detail="missing Bearer token")
        token = authorization.split(" ", 1)[1]
        assert kc is not None
        return kc.validate(token, corr, tenant_header=x_tenant_id)
    except CrossTenantAccessError as exc:
        raise HTTPException(status_code=401, detail=str(exc)) from exc


def require_role(role: str) -> Callable[..., UserContext]:
    def _dep(user: UserContext = Depends(current_user)) -> UserContext:
        if not user.has_role(role) and not user.has_role("admin"):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"role required: {role}",
            )
        return user

    return _dep
