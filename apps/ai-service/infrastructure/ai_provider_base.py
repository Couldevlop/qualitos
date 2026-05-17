"""Shared base for AIProvider adapters."""
from __future__ import annotations

from domain.port.ai_provider import AIProvider


class AIProviderBase(AIProvider):
    """Common scaffolding for provider adapters (timeouts, naming)."""
