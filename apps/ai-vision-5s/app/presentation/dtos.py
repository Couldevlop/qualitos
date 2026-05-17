"""Pydantic DTOs for the Vision 5S REST API."""

from __future__ import annotations

from typing import List, Optional, Tuple

from pydantic import BaseModel, Field


class FindingResponse(BaseModel):
    pillar: str
    description: str
    severity: str
    confidence: float = Field(ge=0.0, le=1.0)
    bbox: Optional[Tuple[int, int, int, int]] = None


class ScoreResponse(BaseModel):
    seiri: int = Field(ge=0, le=100)
    seiton: int = Field(ge=0, le=100)
    seiso: int = Field(ge=0, le=100)
    seiketsu: int = Field(ge=0, le=100)
    shitsuke: int = Field(ge=0, le=100)
    overall: int = Field(ge=0, le=100)


class AnalysisResponse(BaseModel):
    image_sha256: str = Field(min_length=64, max_length=64)
    width: int
    height: int
    score: ScoreResponse
    findings: List[FindingResponse]


class ProblemDetail(BaseModel):
    """RFC 7807 Problem Details."""
    type: str = "about:blank"
    title: str
    status: int
    detail: Optional[str] = None
