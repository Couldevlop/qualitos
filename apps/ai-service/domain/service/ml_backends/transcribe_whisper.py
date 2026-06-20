"""Backend de transcription audio **Whisper** (opt-in, import paresseux — ADR 0031).

Vrai modèle Whisper (OpenAI, via le paquet ``openai-whisper``, lui-même fondé sur
``torch``) pour transcrire un compte-rendu de Cercle de Qualité ou une note vocale
terrain (§3.3, §15.3). Les octets audio reçus sont écrits dans un fichier temporaire
(Whisper lit un chemin via ffmpeg) puis transcrits ; le fichier est supprimé ensuite.

Contrairement aux autres capacités, **il n'existe pas de chemin par défaut léger** :
sans modèle audio réel, il n'y a pas de transcription honnête possible. L'endpoint
dédié répond donc **501** si l'extra ml est absent (au lieu de 422). ``openai-whisper``
et ``torch`` vivent dans l'extra ``ml`` du ``pyproject.toml`` et ne sont PAS
installés en CI : l'import est fait **dans** :func:`transcribe`, son absence lève
:class:`MlBackendUnavailableError`, jamais un faux résultat.
"""
from __future__ import annotations

import os
import tempfile

from domain.model.transcription import Transcription
from domain.service.ml_backends import MlBackendUnavailableError

# Modèle compact par défaut (CPU-friendly) ; surchargeable par l'appelant.
_DEFAULT_MODEL = "base"


def transcribe(audio: bytes, *, language: str | None = None,
               model_name: str | None = None) -> Transcription:
    """Transcrit des octets audio en texte via Whisper.

    :param audio: contenu binaire d'un fichier audio (wav/mp3/m4a/ogg…).
    :param language: code langue forcé (ex. "fr"), ou None pour auto-détection.
    :param model_name: taille de modèle Whisper ("tiny".."large"), défaut "base".
    :raises ValueError: audio vide.
    :raises MlBackendUnavailableError: ``whisper``/``torch`` non installé (extra ml).
    """
    if not audio:
        raise ValueError("audio must be non-empty")

    try:  # import paresseux : whisper/torch ne sont tirés que si ce backend est appelé.
        import whisper
    except ImportError as exc:  # pragma: no cover - exercé sans la lib en CI via le wrapper
        raise MlBackendUnavailableError("whisper", "openai-whisper") from exc

    model = whisper.load_model(model_name or _DEFAULT_MODEL)
    tmp_path: str | None = None
    try:
        with tempfile.NamedTemporaryFile(suffix=".audio", delete=False) as tmp:
            tmp.write(audio)
            tmp_path = tmp.name
        result = model.transcribe(tmp_path, language=language)
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)

    segments = result.get("segments") or []
    duration = float(segments[-1].get("end", 0.0)) if segments else 0.0
    return Transcription(
        text=str(result.get("text", "")).strip(),
        language=str(result.get("language", language or "unknown")),
        duration_s=duration,
        model="whisper",
    )
