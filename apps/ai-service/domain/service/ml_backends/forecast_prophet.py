"""Backend de prévision KPI **Prophet** (opt-in, import paresseux — ADR 0031).

Vrai modèle Prophet (Facebook/Meta) derrière le contrat ``KpiForecast`` existant :
la série de mesures est traitée comme un signal périodique régulier (un point =
une période), Prophet ajuste tendance + saisonnalités et fournit un intervalle
``yhat_lower/yhat_upper`` natif — réutilisé tel quel pour les bornes 95 %.

``prophet`` (et sa dépendance ``cmdstanpy``) est **lourd** : il vit dans l'extra
``ml`` du ``pyproject.toml`` et n'est PAS installé en CI. L'import est donc fait
**dans** :func:`forecast`, et son absence lève :class:`MlBackendUnavailableError`
(→ 422 côté présentation), jamais un faux résultat.
"""
from __future__ import annotations

import math

import numpy as np

from domain.model.predict import KpiForecast, KpiForecastPoint
from domain.service.ml_backends import MlBackendUnavailableError

_MIN_POINTS = 4
_Z95 = 1.959963984540054


def forecast(
    values: list[float],
    target: float,
    *,
    horizon: int = 6,
    direction: str = "at_least",
    seasonal_period: int | None = None,
) -> KpiForecast:
    """Prévoit la série avec Prophet et la probabilité d'atteindre ``target``.

    Même contrat que :func:`domain.service.forecasting.forecast`. La saisonnalité
    Prophet est activée si ``seasonal_period`` est fourni et la série la couvre.

    :raises ValueError: série trop courte, horizon ou direction invalides.
    :raises MlBackendUnavailableError: ``prophet`` non installé (extra ml).
    """
    if direction not in ("at_least", "at_most"):
        raise ValueError("direction must be 'at_least' or 'at_most'")
    if horizon < 1 or horizon > 60:
        raise ValueError("horizon must be within 1..60")
    if len(values) < _MIN_POINTS:
        raise ValueError(f"at least {_MIN_POINTS} data points are required")

    try:  # import paresseux : la lib lourde n'est tirée que si ce backend est choisi.
        import pandas as pd
        from prophet import Prophet
    except ImportError as exc:  # pragma: no cover - exercé sans la lib en CI via le wrapper
        raise MlBackendUnavailableError("prophet", "prophet") from exc

    n = len(values)
    # Indice temporel synthétique régulier (1 période = 1 jour) : Prophet exige des dates.
    ds = pd.date_range("2000-01-01", periods=n, freq="D")
    frame = pd.DataFrame({"ds": ds, "y": np.asarray(values, dtype=float)})

    use_season = (
        seasonal_period is not None and seasonal_period >= 2 and n >= 2 * seasonal_period
    )
    model = Prophet(
        weekly_seasonality=False,
        yearly_seasonality=False,
        daily_seasonality=False,
        interval_width=0.95,
    )
    if use_season:
        model.add_seasonality(name="kpi_cycle", period=int(seasonal_period), fourier_order=3)
    model.fit(frame)

    future = model.make_future_dataframe(periods=horizon, freq="D", include_history=True)
    forecast_df = model.predict(future)

    fitted = forecast_df["yhat"].to_numpy()[:n]
    resid = np.asarray(values, dtype=float) - fitted
    dof = max(resid.size - 2, 1)
    sigma = float(np.sqrt(np.sum(resid**2) / dof))
    obs = np.asarray(values, dtype=float)
    ss_tot = float(np.sum((obs - obs.mean()) ** 2))
    r2 = 1.0 if ss_tot == 0 else max(0.0, 1.0 - float(np.sum(resid**2)) / ss_tot)

    horizon_rows = forecast_df.iloc[n:n + horizon]
    points: list[KpiForecastPoint] = []
    for step, (_, row) in enumerate(horizon_rows.iterrows(), start=1):
        points.append(
            KpiForecastPoint(
                step=step,
                value=float(row["yhat"]),
                low=float(row["yhat_lower"]),
                high=float(row["yhat_upper"]),
            )
        )

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

    # slope = pente moyenne de la composante de tendance sur l'horizon (explicabilité).
    trend = forecast_df["trend"].to_numpy()
    slope = float(trend[-1] - trend[-2]) if trend.size >= 2 else 0.0

    return KpiForecast(
        n=n, slope=slope, intercept=float(fitted[-1]), residual_sigma=sigma, r2=float(r2),
        horizon=horizon, target=float(target), direction=direction,
        probability=float(min(1.0, max(0.0, probability))), confidence=confidence,
        model="prophet", seasonal_period=int(seasonal_period) if use_season else 0,
        points=points,
    )


def _normal_cdf(z: float) -> float:
    return 0.5 * (1.0 + math.erf(z / math.sqrt(2.0)))
