"""POST /v1/ai/transcribe — transcription audio → texte (Whisper, opt-in — ADR 0031).

Cas d'usage : compte-rendu de Cercle de Qualité (§3.3) ou note vocale terrain
(§15.3). Le backend Whisper est **opt-in, importé paresseusement** (extra ml) :
s'il n'est pas installé, l'endpoint répond **501** « transcription non disponible
(extra ml) », jamais un faux texte.

Garde-fous : taille maximale du fichier (anti-DoS, LLM04 §11.2), tenant issu du
JWT/``current_user`` (jamais du body, règle 18.2 #2).
"""
from __future__ import annotations

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile

from domain.model.tenant import UserContext
from domain.service.ml_backends import MlBackendUnavailableError
from presentation.container import Container
from presentation.schemas.transcription import TranscriptionResponse
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai", tags=["transcription"])
_container = Container.build_default()

# Garde-fou taille : 25 Mo (≈ plusieurs minutes audio compressé) — anti-DoS.
_MAX_AUDIO_BYTES = 25 * 1024 * 1024


@router.post(
    "/transcribe",
    response_model=TranscriptionResponse,
    summary="Transcribe an audio file to text (Whisper, opt-in ml extra)",
)
async def transcribe(
    file: UploadFile = File(...),
    language: str | None = Query(default=None, max_length=8),
    user: UserContext = Depends(current_user),
) -> TranscriptionResponse:
    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=422, detail="empty audio file")
    if len(audio) > _MAX_AUDIO_BYTES:
        raise HTTPException(
            status_code=413,
            detail=f"audio too large (max {_MAX_AUDIO_BYTES} bytes)",
        )
    try:
        result = _container.transcribe().execute(audio, user.tenant, language=language)
    except MlBackendUnavailableError as exc:
        # Aucun chemin par défaut léger pour l'audio → 501 (non implémenté ici).
        raise HTTPException(
            status_code=501,
            detail=f"transcription non disponible (extra ml) : {exc}",
        ) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return TranscriptionResponse.from_domain(result)
