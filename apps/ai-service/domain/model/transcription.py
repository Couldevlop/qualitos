"""Modèle de domaine : transcription audio → texte (§3.3, §15.3, §12.1).

Objet de valeur immuable. Le calcul vit dans un backend ML lourd opt-in
(:mod:`domain.service.ml_backends.transcribe_whisper`, ADR 0031) ; il n'y a pas
de chemin par défaut « léger » (la transcription exige un vrai modèle audio),
donc l'endpoint répond **501** si l'extra ml n'est pas installé.
"""
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class Transcription:
    """Résultat d'une transcription audio."""

    text: str
    language: str        # langue détectée (code ISO, ou "unknown")
    duration_s: float    # durée audio estimée en secondes (0 si inconnue)
    model: str           # backend appliqué (ex. "whisper")
