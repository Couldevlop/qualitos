"""Modèle de domaine : détection d'anomalies non-supervisée multivariée.

Contrairement au SPC (univarié, carte de contrôle individuelle), l'entrée est une
**matrice** échantillons × features. Deux méthodes non-supervisées reconnues sont
proposées (CLAUDE.md §3.4, §12.1 « Isolation Forest, Autoencoders ») :

- ``isolation_forest`` : forêt d'arbres d'isolation (Liu, Ting, Zhou 2008).
- ``reconstruction`` : auto-encodeur LINÉAIRE par ACP (SVD) — erreur de
  reconstruction L2 par échantillon. Honnête : ce n'est PAS un réseau de neurones.

Tous les objets de valeur sont immuables ; le calcul vit dans
``domain.service.isolation_forest`` et ``domain.service.reconstruction`` comme des
fonctions pures NumPy (déterministes à graine fixe, auditables, testables).
"""
from __future__ import annotations

from dataclasses import dataclass, field

# Méthodes supportées (validées en présentation et au use case).
METHOD_ISOLATION_FOREST = "isolation_forest"
METHOD_RECONSTRUCTION = "reconstruction"
SUPPORTED_METHODS = frozenset({METHOD_ISOLATION_FOREST, METHOD_RECONSTRUCTION})


@dataclass(frozen=True, slots=True)
class AnomalyPoint:
    """Score d'anomalie d'un échantillon (ligne de la matrice d'entrée).

    ``score`` est croissant avec l'anormalité, dans [0, 1] :
    - Isolation Forest : ``s(x) = 2^(-E[h(x)]/c(n))`` (proche de 1 = anomalie).
    - Reconstruction : erreur L2 normalisée par l'erreur maximale du lot.

    ``top_feature`` (optionnel) : index de la feature qui contribue le plus à
    l'erreur de reconstruction — None pour Isolation Forest (non attribuable
    naturellement à une feature unique).
    """

    index: int
    score: float
    is_anomaly: bool
    top_feature: int | None = None


@dataclass(frozen=True, slots=True)
class AnomalyResult:
    """Résultat d'une détection d'anomalies sur une matrice multivariée."""

    n: int
    n_features: int
    method: str
    contamination: float
    threshold: float
    anomaly_count: int
    points: list[AnomalyPoint] = field(default_factory=list)

    @property
    def has_anomalies(self) -> bool:
        return self.anomaly_count > 0
