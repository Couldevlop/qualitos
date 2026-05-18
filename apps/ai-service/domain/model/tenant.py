"""Tenant + user context value objects.

A request is always bound to a tenant (from JWT) — never trust body input.
"""
from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True, slots=True)
class TenantContext:
    """Tenant identification, propagated from JWT validation."""

    tenant_id: UUID
    issuer: str

    def __post_init__(self) -> None:
        if not isinstance(self.tenant_id, UUID):
            raise ValueError("tenant_id must be a UUID")
        if not self.issuer:
            raise ValueError("issuer required")


@dataclass(frozen=True, slots=True)
class UserContext:
    """Authenticated user calling the AI service."""

    user_id: UUID
    tenant: TenantContext
    roles: frozenset[str]
    correlation_id: str

    def has_role(self, role: str) -> bool:
        return role in self.roles
