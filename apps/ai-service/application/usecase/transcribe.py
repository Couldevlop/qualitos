"""Use case : transcription audio → texte (Whisper, opt-in — ADR 0031, §3.3).

Même couture que les autres use cases : le tenant est porté pour l'auditabilité ;
le calcul vit dans le backend lourd :mod:`domain.service.ml_backends.transcribe_whisper`
(import paresseux). Aucun chemin par défaut léger : si l'extra ml est absent, le
backend lève ``MlBackendUnavailableError`` (→ 501 côté présentation).
"""
from __future__ import annotations

import logging

from domain.model.tenant import TenantContext
from domain.model.transcription import Transcription
from domain.service.ml_backends import transcribe_whisper

logger = logging.getLogger(__name__)


class TranscribeUseCase:
    """Octets audio → texte transcrit (compte-rendu Cercle, note vocale terrain)."""

    def execute(
        self,
        audio: bytes,
        tenant: TenantContext,
        *,
        language: str | None = None,
    ) -> Transcription:
        result = transcribe_whisper.transcribe(audio, language=language)
        logger.info(
            "Transcription tenant=%s bytes=%d language=%s model=%s",
            tenant.tenant_id, len(audio), result.language, result.model,
        )
        return result
