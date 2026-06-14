"""Prévision KPI par lissage exponentiel de Holt-Winters (NumPy pur).

Pas d'I/O, pas de framework (domaine hexagonal). Le modèle est un vrai modèle de
séries temporelles, déterministe et auditable — un cran au-dessus de la tendance
OLS : il capte un **niveau** et une **tendance** adaptatifs (Holt) et, si une
période saisonnière est fournie, une **composante saisonnière additive**
(Holt-Winters). Les LSTM/Prophet pourront se brancher derrière le même contrat
``KpiForecast`` quand un budget GPU/dépendances lourdes existera (même logique que
``nc_clustering`` / ADR 0014 §7.3) — ici, NumPy seul, zéro dépendance lourde.

Méthode :
1. Lissage de Holt (double ES) ``l_t, b_t`` ; + saisonnalité additive ``s_t`` si
   ``seasonal_period`` fourni et la série couvre ≥ 2 périodes.
2. Paramètres ``alpha, beta(, gamma)`` choisis par **grid-search** minimisant la
   somme des carrés des erreurs one-step (ajustement réel, reproductible).
3. σ résiduel sur l'ajustement en échantillon ; intervalle de prédiction à 95 %
   élargi en √h avec l'horizon.
4. ``P(cible atteinte)`` via la CDF normale de la distribution de prévision à
   l'horizon (selon la direction).

Une série parfaitement linéaire est reproduite exactement par Holt (l'erreur
one-step est nulle dès l'initialisation l₀=y₀, b₀=y₁−y₀), donc la tendance et la
prévision restent exactes — la baseline linéaire est un cas particulier propre.
"""
from __future__ import annotations

import math

import numpy as np

from domain.model.predict import KpiForecast, KpiForecastPoint

_MIN_POINTS = 4
_Z95 = 1.959963984540054
# Grille de lissage (bornée, exclut 0/1 pour rester adaptatif sans coller au bruit).
_GRID = (0.1, 0.3, 0.5, 0.7, 0.9)


def forecast(
    values: list[float],
    target: float,
    *,
    horizon: int = 6,
    direction: str = "at_least",
    seasonal_period: int | None = None,
) -> KpiForecast:
    """Prévoit la série et la probabilité d'atteindre ``target``.

    :param seasonal_period: période saisonnière (ex. 7, 12). Utilisée seulement si
        ``2 ≤ period`` et la série couvre au moins 2 périodes ; sinon Holt linéaire.
    :raises ValueError: série trop courte, horizon ou direction invalides.
    """
    if direction not in ("at_least", "at_most"):
        raise ValueError("direction must be 'at_least' or 'at_most'")
    if horizon < 1 or horizon > 60:
        raise ValueError("horizon must be within 1..60")
    if len(values) < _MIN_POINTS:
        raise ValueError(f"at least {_MIN_POINTS} data points are required")

    y = np.asarray(values, dtype=float)
    n = y.size

    use_season = (
        seasonal_period is not None and seasonal_period >= 2 and n >= 2 * seasonal_period
    )
    if use_season:
        m = int(seasonal_period)  # type: ignore[arg-type]
        level, trend, fitted, start, season = _fit_holt_winters(y, m)
        model_name = "holt_winters_additive"
    else:
        m = 0
        level, trend, fitted, start = _fit_holt_linear(y)
        season = None
        model_name = "holt_linear"

    # σ résiduel + R² sur l'ajustement one-step (à partir de l'index `start`).
    resid = y[start:] - fitted[start:]
    dof = max(resid.size - 2, 1)
    sigma = float(np.sqrt(np.sum(resid**2) / dof))
    obs = y[start:]
    ss_tot = float(np.sum((obs - obs.mean()) ** 2)) if obs.size else 0.0
    r2 = 1.0 if ss_tot == 0 else max(0.0, 1.0 - float(np.sum(resid**2)) / ss_tot)

    last_level = float(level[-1])
    last_trend = float(trend[-1])
    # Saisonniers les plus récents (un cycle), alignés sur les périodes à venir.
    seasonals = season[n - m:n] if (use_season and season is not None) else None

    points: list[KpiForecastPoint] = []
    for step in range(1, horizon + 1):
        base = last_level + step * last_trend
        if seasonals is not None:
            base += seasonals[(step - 1) % m]
        half = _Z95 * sigma * math.sqrt(step)  # élargissement standard en √h
        points.append(KpiForecastPoint(step=step, value=float(base),
                                       low=float(base - half), high=float(base + half)))

    last = points[-1]
    spread = (last.high - last.low) / (2 * _Z95) or 1e-12
    z = (target - last.value) / spread
    p_above = 1.0 - _normal_cdf(z)
    probability = p_above if direction == "at_least" else 1.0 - p_above

    if n < 8 or r2 < 0.2:
        confidence = "low"
    elif n < 15 or r2 < 0.5:
        confidence = "medium"
    else:
        confidence = "high"

    return KpiForecast(
        n=n, slope=last_trend, intercept=last_level, residual_sigma=sigma, r2=float(r2),
        horizon=horizon, target=float(target), direction=direction,
        probability=float(min(1.0, max(0.0, probability))), confidence=confidence,
        model=model_name, seasonal_period=m,
        points=points,
    )


# --- Holt (niveau + tendance) ----------------------------------------------------


def _holt_linear_pass(y: np.ndarray, alpha: float, beta: float
                      ) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Un passage de Holt. Renvoie (niveaux, tendances, prévisions one-step)."""
    n = y.size
    level = np.empty(n)
    trend = np.empty(n)
    fitted = np.empty(n)
    level[0] = y[0]
    trend[0] = y[1] - y[0]
    fitted[0] = y[0]
    for t in range(1, n):
        fitted[t] = level[t - 1] + trend[t - 1]  # prévision one-step (avant y[t])
        level[t] = alpha * y[t] + (1 - alpha) * (level[t - 1] + trend[t - 1])
        trend[t] = beta * (level[t] - level[t - 1]) + (1 - beta) * trend[t - 1]
    return level, trend, fitted


def _fit_holt_linear(y: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray, int]:
    """Grid-search (alpha, beta) minimisant la SSE one-step. start=1 (init sur 2 pts)."""
    best = None
    for alpha in _GRID:
        for beta in _GRID:
            level, trend, fitted = _holt_linear_pass(y, alpha, beta)
            sse = float(np.sum((y[1:] - fitted[1:]) ** 2))
            if best is None or sse < best[0]:
                best = (sse, level, trend, fitted)
    _, level, trend, fitted = best  # type: ignore[misc]
    return level, trend, fitted, 1


# --- Holt-Winters additif (niveau + tendance + saisonnalité) ---------------------


def _holt_winters_pass(y: np.ndarray, m: int, alpha: float, beta: float, gamma: float
                       ) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Un passage de Holt-Winters additif. Renvoie (niveaux, tendances, saisonniers, prévisions)."""
    n = y.size
    level = np.empty(n)
    trend = np.empty(n)
    fitted = np.empty(n)
    season = np.zeros(n + m)  # season[t] = composante saisonnière appliquée à l'instant t
    # Initialisation classique : niveau = moyenne de la 1re période, tendance = pente
    # moyenne entre les 2 premières périodes, saisonniers = écarts à la moyenne.
    first = y[:m]
    second = y[m:2 * m]
    level[m - 1] = float(first.mean())
    trend[m - 1] = float((second.mean() - first.mean()) / m)
    for i in range(m):
        season[i] = y[i] - level[m - 1]
    fitted[:m] = y[:m]
    for t in range(m, n):
        fitted[t] = level[t - 1] + trend[t - 1] + season[t - m]
        level[t] = alpha * (y[t] - season[t - m]) + (1 - alpha) * (level[t - 1] + trend[t - 1])
        trend[t] = beta * (level[t] - level[t - 1]) + (1 - beta) * trend[t - 1]
        season[t] = gamma * (y[t] - level[t]) + (1 - gamma) * season[t - m]
    return level, trend, season, fitted


def _fit_holt_winters(y: np.ndarray, m: int
                      ) -> tuple[np.ndarray, np.ndarray, np.ndarray, int, np.ndarray]:
    """Grid-search (alpha, beta, gamma) minimisant la SSE one-step. start=m.

    Renvoie (niveaux, tendances, prévisions, start, saisonniers) — les saisonniers
    couvrent les indices 0..n-1 (alignés sur la série) pour la projection future.
    """
    best = None
    for alpha in _GRID:
        for beta in _GRID:
            for gamma in _GRID:
                level, trend, season, fitted = _holt_winters_pass(y, m, alpha, beta, gamma)
                sse = float(np.sum((y[m:] - fitted[m:]) ** 2))
                if best is None or sse < best[0]:
                    best = (sse, level, trend, season, fitted)
    _, level, trend, season, fitted = best  # type: ignore[misc]
    return level, trend, fitted, m, season[:y.size]


def _normal_cdf(z: float) -> float:
    return 0.5 * (1.0 + math.erf(z / math.sqrt(2.0)))
