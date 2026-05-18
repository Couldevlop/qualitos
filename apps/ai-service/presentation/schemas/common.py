"""RFC 7807 problem schema."""
from __future__ import annotations

from pydantic import BaseModel, Field


class ProblemDetail(BaseModel):
    type: str = Field(default="https://qualitos.io/errors/generic")
    title: str
    status: int
    detail: str | None = None
    instance: str | None = None
    code: str | None = None
