"""Quand un procédé sous contrôle se dirige vers la sortie de ses limites.

La carte de contrôle dit si un procédé EST hors contrôle : elle regarde derrière,
et l'opérateur découvre la dérive quand les pièces sont déjà faites. Ce service
dit quand il VA l'être — la seule version sur laquelle on peut encore agir
(§6.5 « le processus Y va sortir des limites SPC dans N jours »).

Méthode, en trois gestes et aucun de plus :

1. Les limites viennent de ``spc_rules`` — les MÊMES que celles que la carte
   dessine (centre, σ = MR̄/d2), ou celles que l'appelant fournit quand une
   référence procédé est établie. Juger une prévision contre d'autres limites
   que celles affichées ne parlerait pas du même procédé.
2. La projection vient de ``forecasting.project`` — le même Holt que la prévision
   de KPI, donc le même niveau, la même tendance, le même σ résiduel. Une seconde
   implémentation finirait par répondre autrement à l'une qu'à l'autre.
3. Pour chaque période à venir, la loi normale de la prévision donne
   ``P(y > LCS)`` et ``P(y < LCI)``. La première période dont le risque atteint
   le seuil est celle qu'on annonce.

L'incertitude s'élargit en √h : plus l'horizon est loin, plus l'intervalle est
large, donc plus le risque de franchissement monte mécaniquement. C'est pour cela
que le résultat porte sa confiance (quantité de points, qualité d'ajustement) et
sa tendance : un risque de 0,6 à huit périodes sur une série de cinq points n'est
pas une prédiction, et doit se lire comme telle (§12.3).

NumPy pur, aucune I/O, aucun framework : fonction du domaine, déterministe.
"""
from __future__ import annotations

import math

from domain.model.predict import SeriesProjection
from domain.model.spc import SpcBreachForecast, SpcBreachPoint, SpcLimits
from domain.service import forecasting, spc_rules

_Z95 = 1.959963984540054
# Au-delà de ce risque, la période est annoncée comme un franchissement : plus
# probable dehors que dedans. Réglable par l'appelant.
_DEFAULT_THRESHOLD = 0.5
_MIN_POINTS = 4


def forecast_breach(
    values: list[float],
    *,
    horizon: int = 6,
    center: float | None = None,
    sigma: float | None = None,
    seasonal_period: int | None = None,
    threshold: float = _DEFAULT_THRESHOLD,
) -> SpcBreachForecast:
    """Prévoit la sortie des limites de contrôle sur ``horizon`` périodes.

    :param center: centre du procédé connu ; fourni avec ``sigma`` ou pas du tout.
    :param sigma: σ du procédé connu (> 0) ; les limites deviennent alors celles
        de la référence et non celles estimées sur la série.
    :param threshold: risque à partir duquel une période est annoncée (0, 1].
    :raises ValueError: série trop courte, horizon/seuil invalide, référence
        incomplète ou σ non strictement positif.
    """
    if (center is None) != (sigma is None):
        raise ValueError("center and sigma must be provided together")
    if sigma is not None and sigma <= 0:
        raise ValueError("sigma must be > 0")
    if horizon < 1 or horizon > 60:
        raise ValueError("horizon must be within 1..60")
    if not 0.0 < threshold <= 1.0:
        raise ValueError("threshold must be within (0, 1]")
    if len(values) < _MIN_POINTS:
        raise ValueError(f"at least {_MIN_POINTS} data points are required")

    limits = _limits(values, center, sigma)
    proj = forecasting.project(values, horizon=horizon, seasonal_period=seasonal_period)

    points = [_point(p, limits, proj) for p in proj.points]
    first, side = _first_breach(points, limits, threshold)

    return SpcBreachForecast(
        n=proj.n,
        limits=limits,
        horizon=horizon,
        threshold=float(threshold),
        points=points,
        first_breach_step=first,
        side=side,
        trend=proj.trend,
        residual_sigma=proj.residual_sigma,
        r2=proj.r2,
        confidence=proj.confidence,
        model=proj.model,
    )


def _limits(values: list[float], center: float | None, sigma: float | None) -> SpcLimits:
    """Les limites de la carte : celles de la référence, sinon celles de la série."""
    if center is None or sigma is None:
        return spc_rules.estimate_limits(values)
    return SpcLimits(
        center_line=float(center),
        sigma=float(sigma),
        ucl=float(center) + 3.0 * float(sigma),
        lcl=float(center) - 3.0 * float(sigma),
        estimated=False,
    )


def _point(p, limits: SpcLimits, proj: SeriesProjection) -> SpcBreachPoint:
    """Le risque de franchissement d'une période, des deux côtés.

    L'écart-type de la prévision à l'horizon h se relit dans l'intervalle déjà
    calculé par la projection (``(haut − bas) / 2z``) : le reprendre d'ici
    garantit que le risque annoncé et la bande dessinée disent la même chose.
    """
    spread = (p.high - p.low) / (2 * _Z95)
    if spread <= 0:
        # Ajustement parfait (série sans bruit) : la seule inconnue est la
        # position du point, et elle est franche. Pas de demi-mesure à inventer.
        return SpcBreachPoint(
            step=p.step, value=p.value, low=p.low, high=p.high,
            p_above_ucl=1.0 if p.value > limits.ucl else 0.0,
            p_below_lcl=1.0 if p.value < limits.lcl else 0.0,
        )
    return SpcBreachPoint(
        step=p.step, value=p.value, low=p.low, high=p.high,
        p_above_ucl=1.0 - _normal_cdf((limits.ucl - p.value) / spread),
        p_below_lcl=_normal_cdf((limits.lcl - p.value) / spread),
    )


def _first_breach(points: list[SpcBreachPoint], limits: SpcLimits,
                  threshold: float) -> tuple[int | None, str | None]:
    """La première période à risque, et de quel côté elle sort.

    Les limites dégénérées (σ nul, procédé plat) ne décrivent aucune dispersion :
    annoncer un franchissement contre une bande d'épaisseur nulle n'informerait
    de rien.
    """
    if limits.ucl == limits.lcl:
        return None, None
    for p in points:
        if p.p_breach >= threshold:
            return p.step, "upper" if p.p_above_ucl >= p.p_below_lcl else "lower"
    return None, None


def _normal_cdf(z: float) -> float:
    """CDF normale standard par la fonction d'erreur — exacte, sans table."""
    return 0.5 * (1.0 + math.erf(z / math.sqrt(2.0)))
