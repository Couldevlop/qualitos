"""Schéma Pydantic de la transcription audio (§3.3, ADR 0031)."""
from __future__ import annotations

from pydantic import BaseModel

from domain.model.transcription import Transcription


class TranscriptionResponse(BaseModel):
    text: str
    language: str
    duration_s: float
    model: str

    @classmethod
    def from_domain(cls, t: Transcription) -> "TranscriptionResponse":
        return cls(text=t.text, language=t.language, duration_s=t.duration_s, model=t.model)
