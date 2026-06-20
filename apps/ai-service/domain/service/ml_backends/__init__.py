"""Backends ML lourds **enfichables, opt-in, import paresseux** (ADR 0031).

Le chemin par défaut de chaque capacité IA reste celui — réel, déterministe et
sans dépendance lourde — décrit aux ADR 0023 (Holt-Winters), 0022/0018 (anomalies),
0025 (NLP lexical) et `predict.py` (DBSCAN). Ce paquet n'ajoute **que** des
alternatives lourdes (Prophet, LSTM, HDBSCAN, BERT, Whisper) :

- chaque backend est du **vrai code** (vrais appels lib), jamais un faux résultat ;
- la dépendance lourde est **importée DANS la fonction** (jamais en tête de module),
  donc importer ce paquet n'entraîne aucune lib lourde ;
- la dépendance vit dans l'extra ``[project.optional-dependencies] ml`` du
  ``pyproject.toml`` — absente du runtime par défaut et de la CI ;
- si le backend est sélectionné mais la lib absente, on lève
  :class:`MlBackendUnavailableError` → la couche présentation la mappe en **422**
  (forecast/clustering/NLP) ou **501** (transcription Whisper, endpoint dédié),
  avec un message clair « installer l'extra ml ». **Jamais** d'inférence factice.

Le domaine reste sans framework (import-linter §domaine) : ces modules n'importent
que NumPy et — paresseusement — les libs ML.
"""
from __future__ import annotations


class MlBackendUnavailableError(RuntimeError):
    """Backend ML sélectionné mais sa dépendance lourde n'est pas installée.

    Porte le nom du backend et le paquet manquant pour un message actionnable
    (« installer l'extra ml »). La présentation la traduit en 422/501.
    """

    def __init__(self, backend: str, package: str) -> None:
        self.backend = backend
        self.package = package
        super().__init__(
            f"backend '{backend}' indisponible : dépendance '{package}' non installée "
            f"(installer l'extra ml : pip install '.[ml]')"
        )
