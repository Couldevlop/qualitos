"""AI provider host allow-list — OWASP A10 + LLM07."""
from __future__ import annotations

import pytest

from infrastructure.providers._allowlist import assert_host_allowed


@pytest.mark.parametrize(
    "url",
    [
        "https://api.anthropic.com/v1/messages",
        "https://api.mistral.ai/v1/chat/completions",
        "http://ollama:11434/api/generate",
        "http://localhost:11434/api/generate",
    ],
)
def test_allowed_hosts_pass(url):
    assert_host_allowed(url)


@pytest.mark.parametrize(
    "url",
    [
        "http://attacker.com/llm",
        "https://169.254.169.254/latest/meta-data/",  # AWS metadata SSRF
        "https://internal-admin.local/secret",
        "http://10.0.0.1/exfil",
    ],
)
def test_blocked_hosts_raise(url):
    with pytest.raises(PermissionError):
        assert_host_allowed(url)
