"""Pure KPI forecasting service — OLS trend + residual prediction intervals.

No I/O, no framework imports (hexagonal domain). The math is deliberately
classic and auditable :

1. Trend ``y = a + b·t`` fitted by ordinary least squares on the series.
2. Residual σ estimated on the fit (ddof=2).
3. Point forecast at horizon h with a 95 % prediction interval widened by the
   standard OLS extrapolation factor.
4. ``P(target reached)`` from the normal CDF of the forecast distribution at
   the horizon (direction-aware).

Confidence is qualitative and honest : few points or poor fit → "low".
"""
from __future__ import annotations

import math

import numpy as np

from domain.model.predict import KpiForecast, KpiForecastPoint

_MIN_POINTS = 4
_Z95 = 1.959963984540054


def forecast(
    values: list[float],
    target: float,
    *,
    horizon: int = 6,
    direction: str = "at_least",
) -> KpiForecast:
    """Forecast the series and the probability of reaching ``target``.

    :raises ValueError: series too short, bad horizon or bad direction.
    """
    if direction not in ("at_least", "at_most"):
        raise ValueError("direction must be 'at_least' or 'at_most'")
    if horizon < 1 or horizon > 60:
        raise ValueError("horizon must be within 1..60")
    if len(values) < _MIN_POINTS:
        raise ValueError(f"at least {_MIN_POINTS} data points are required")

    y = np.asarray(values, dtype=float)
    n = y.size
    t = np.arange(n, dtype=float)

    # OLS y = a + b·t
    b, a = np.polyfit(t, y, 1)
    fitted = a + b * t
    residuals = y - fitted
    dof = max(n - 2, 1)
    sigma = float(np.sqrt(np.sum(residuals**2) / dof))

    ss_tot = float(np.sum((y - y.mean()) ** 2))
    r2 = 1.0 if ss_tot == 0 else max(0.0, 1.0 - float(np.sum(residuals**2)) / ss_tot)

    t_mean = float(t.mean())
    sxx = float(np.sum((t - t_mean) ** 2)) or 1.0

    points: list[KpiForecastPoint] = []
    for step in range(1, horizon + 1):
        th = float(n - 1 + step)
        yh = a + b * th
        # Prediction-interval widening for extrapolation distance.
        widen = math.sqrt(1.0 + 1.0 / n + (th - t_mean) ** 2 / sxx)
        half = _Z95 * sigma * widen
        points.append(KpiForecastPoint(step=step, value=float(yh),
                                       low=float(yh - half), high=float(yh + half)))

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
        n=n, slope=float(b), intercept=float(a), residual_sigma=sigma, r2=float(r2),
        horizon=horizon, target=float(target), direction=direction,
        probability=float(min(1.0, max(0.0, probability))), confidence=confidence,
        points=points,
    )


def _normal_cdf(z: float) -> float:
    return 0.5 * (1.0 + math.erf(z / math.sqrt(2.0)))
