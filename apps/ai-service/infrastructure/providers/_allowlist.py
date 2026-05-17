"""Allow-listed AI provider endpoints (OWASP A10 SSRF / LLM07)."""
from __future__ import annotations

from urllib.parse import urlparse

ALLOWED_HOSTS: frozenset[str] = frozenset(
    {
        "api.anthropic.com",
        "api.mistral.ai",
        "ollama",  # docker hostname
        "localhost",
        "127.0.0.1",
    }
)


def assert_host_allowed(url: str) -> None:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    if host not in ALLOWED_HOSTS:
        raise PermissionError(
            f"AI provider host '{host}' not in allow-list — refusing call"
        )
