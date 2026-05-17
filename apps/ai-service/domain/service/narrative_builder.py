"""Builds a deterministic narrative explaining a result set.

Pure function — used as a fallback when LLM is unavailable or for grounding.
"""
from __future__ import annotations

from typing import Any


class NarrativeBuilder:
    """Generates a one-paragraph plain-text explanation of rows + question."""

    @staticmethod
    def build(question: str, rows: list[dict[str, Any]]) -> str:
        if not rows:
            return f"No rows returned for: {question}"
        if len(rows) == 1 and len(rows[0]) == 1:
            (col, val), = rows[0].items()
            return f"For '{question}': {col} = {val} (single value)."
        cols = list(rows[0].keys())
        return (
            f"Question: {question}. "
            f"Result: {len(rows)} row(s), {len(cols)} column(s): "
            f"{', '.join(cols)}."
        )
