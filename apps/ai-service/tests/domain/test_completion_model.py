"""Domain invariants for completion entities."""
from __future__ import annotations

import pytest

from domain.model.completion import (
    Citation,
    CompletionRequest,
    Confidence,
    ProviderName,
)


def test_citation_score_bounds():
    Citation(document_id="d1", score=0.5, excerpt="x")
    with pytest.raises(ValueError):
        Citation(document_id="d1", score=1.5, excerpt="x")
    with pytest.raises(ValueError):
        Citation(document_id="d1", score=-0.1, excerpt="x")


def test_confidence_bounds():
    Confidence(value=0.0, method="heuristic")
    Confidence(value=1.0, method="logprob")
    with pytest.raises(ValueError):
        Confidence(value=1.1, method="x")


def test_completion_request_requires_prompt():
    with pytest.raises(ValueError):
        CompletionRequest(system_prompt="s", user_prompt="", provider=ProviderName.OLLAMA)


def test_completion_request_max_tokens_bounds():
    with pytest.raises(ValueError):
        CompletionRequest(
            system_prompt="s",
            user_prompt="hi",
            provider=ProviderName.OLLAMA,
            max_tokens=0,
        )
    with pytest.raises(ValueError):
        CompletionRequest(
            system_prompt="s",
            user_prompt="hi",
            provider=ProviderName.OLLAMA,
            max_tokens=20_000,
        )
