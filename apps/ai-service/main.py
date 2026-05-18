"""Uvicorn entrypoint."""
from __future__ import annotations

from presentation.app import app  # re-exported for `uvicorn main:app`

__all__ = ["app"]
