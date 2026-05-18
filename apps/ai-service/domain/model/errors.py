"""Domain-level errors. Mapped to RFC 7807 ProblemDetail at the presentation layer."""
from __future__ import annotations


class DomainError(Exception):
    """Base for all domain errors. Mapped to ProblemDetail."""

    code: str = "domain-error"
    http_status: int = 400


class PiiViolationError(DomainError):
    code = "pii-violation"
    http_status = 422


class PromptInjectionError(DomainError):
    code = "prompt-injection-detected"
    http_status = 422


class UnsafeSqlError(DomainError):
    code = "unsafe-sql"
    http_status = 422


class ProviderUnavailableError(DomainError):
    code = "provider-unavailable"
    http_status = 503


class CrossTenantAccessError(DomainError):
    code = "cross-tenant-access"
    http_status = 403


class RateLimitExceededError(DomainError):
    code = "rate-limit-exceeded"
    http_status = 429
