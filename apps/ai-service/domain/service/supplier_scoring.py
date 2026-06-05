"""Pure supplier risk scoring — transparent weighted logistic model.

Chaque feature a un poids DOCUMENTÉ et auditable (pas de boîte noire — §12.3) ;
le score est la logistique de la somme pondérée des features normalisées, et
chaque ``RiskDriver`` expose sa contribution signée. Quand un historique réel
de défaillances existera, un modèle entraîné (XGBoost + SHAP) remplacera les
poids — même contrat de sortie.

Features attendues (toutes optionnelles, neutres par défaut) :
- ``nc_rate``              : NC pour 1000 unités reçues (0..∞, pivot 5)
- ``nc_trend``             : variation relative des NC sur 3 mois (-1..+1)
- ``late_delivery_rate``   : part de livraisons en retard (0..1)
- ``audit_score``          : dernier score d'audit fournisseur (0..100, pivot 70)
- ``days_since_last_audit``: ancienneté du dernier audit (pivot 365 j)
- ``open_complaints``      : réclamations ouvertes imputées (pivot 3)
"""
from __future__ import annotations

import math

from domain.model.predict import RiskDriver, SupplierRiskScore

# (poids, normalisation) — normalisation ramène la feature autour de [-1, +1]
# où +1 aggrave le risque. Poids relus en revue qualité (auditable).
_WEIGHTS: dict[str, float] = {
    "nc_rate": 1.6,
    "nc_trend": 1.1,
    "late_delivery_rate": 1.2,
    "audit_score": 1.4,
    "days_since_last_audit": 0.7,
    "open_complaints": 0.9,
}

_BIAS = -1.2   # un fournisseur « neutre » ressort ~23/100 (risque faible)


def score(features: dict[str, float]) -> SupplierRiskScore:
    """Score 0-100 + drivers explicables. Features inconnues → rejet explicite."""
    unknown = set(features) - set(_WEIGHTS)
    if unknown:
        raise ValueError(f"unknown features: {sorted(unknown)}")

    drivers: list[RiskDriver] = []
    linear = _BIAS
    for name, weight in _WEIGHTS.items():
        if name not in features:
            continue
        raw = float(features[name])
        normalized = _normalize(name, raw)
        contribution = weight * normalized
        linear += contribution
        drivers.append(RiskDriver(feature=name, value=raw, weight=weight,
                                  contribution=round(contribution, 4)))

    s = 100.0 / (1.0 + math.exp(-linear))
    drivers.sort(key=lambda d: abs(d.contribution), reverse=True)
    return SupplierRiskScore(score=round(s, 1), level=_level(s), drivers=drivers)


def _normalize(name: str, raw: float) -> float:
    """Ramène chaque feature vers [-1, +1] (positif = risque accru)."""
    if name == "nc_rate":                  # pivot 5 NC/1000
        return _squash(raw / 5.0 - 1.0)
    if name == "nc_trend":                 # déjà relatif (-1..+1)
        return _clamp(raw, -1.0, 1.0)
    if name == "late_delivery_rate":       # 0..1, pivot 10 %
        return _squash(raw / 0.10 - 1.0)
    if name == "audit_score":              # score haut = risque bas
        return _squash((70.0 - raw) / 30.0)
    if name == "days_since_last_audit":    # pivot 1 an
        return _squash(raw / 365.0 - 1.0)
    if name == "open_complaints":          # pivot 3 réclamations
        return _squash(raw / 3.0 - 1.0)
    raise ValueError(name)


def _squash(x: float) -> float:
    return math.tanh(x)


def _clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def _level(s: float) -> str:
    if s < 30:
        return "low"
    if s < 55:
        return "medium"
    if s < 75:
        return "high"
    return "critical"
