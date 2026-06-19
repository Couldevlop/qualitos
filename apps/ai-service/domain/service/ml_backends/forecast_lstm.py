"""Backend de prévision KPI **LSTM** (PyTorch, opt-in, import paresseux — ADR 0031).

Vrai réseau LSTM (PyTorch) entraîné à la volée sur la série soumise, derrière le
contrat ``KpiForecast`` existant. La série est standardisée, découpée en fenêtres
glissantes ; un LSTM mono-couche apprend la dynamique one-step ; la prévision à
l'horizon est obtenue par projection auto-régressive. L'intervalle 95 % vient du
σ résiduel d'entraînement, élargi en √h (même convention que Holt-Winters).

``torch`` est **lourd** (GPU/CPU build volumineux) : il vit dans l'extra ``ml`` du
``pyproject.toml`` et n'est PAS installé en CI. L'import est fait **dans**
:func:`forecast` ; son absence lève :class:`MlBackendUnavailableError` (→ 422).
L'entraînement est déterministe (graine fixe) pour rester auditable (§12.3).
"""
from __future__ import annotations

import math

import numpy as np

from domain.model.predict import KpiForecast, KpiForecastPoint
from domain.service.ml_backends import MlBackendUnavailableError

_MIN_POINTS = 4
_Z95 = 1.959963984540054
_SEED = 1729
_EPOCHS = 200
_HIDDEN = 16


def forecast(
    values: list[float],
    target: float,
    *,
    horizon: int = 6,
    direction: str = "at_least",
    seasonal_period: int | None = None,
) -> KpiForecast:
    """Prévoit la série avec un LSTM et la probabilité d'atteindre ``target``.

    ``seasonal_period`` (si fourni, ≥ 2 et couvert) fixe la taille de fenêtre afin
    que le réseau « voie » un cycle complet ; sinon une fenêtre courte est utilisée.

    :raises ValueError: série trop courte, horizon ou direction invalides.
    :raises MlBackendUnavailableError: ``torch`` non installé (extra ml).
    """
    if direction not in ("at_least", "at_most"):
        raise ValueError("direction must be 'at_least' or 'at_most'")
    if horizon < 1 or horizon > 60:
        raise ValueError("horizon must be within 1..60")
    if len(values) < _MIN_POINTS:
        raise ValueError(f"at least {_MIN_POINTS} data points are required")

    try:  # import paresseux : torch n'est tiré que si ce backend est choisi.
        import torch
        from torch import nn
    except ImportError as exc:  # pragma: no cover - exercé sans la lib en CI via le wrapper
        raise MlBackendUnavailableError("lstm", "torch") from exc

    torch.manual_seed(_SEED)
    y = np.asarray(values, dtype=float)
    n = y.size

    # Standardisation (le LSTM converge mieux centré-réduit) ; σ=0 → série constante.
    mean = float(y.mean())
    std = float(y.std()) or 1.0
    z = (y - mean) / std

    use_season = (
        seasonal_period is not None and seasonal_period >= 2 and n >= 2 * seasonal_period
    )
    window = min(int(seasonal_period), n - 1) if use_season else min(3, n - 1)
    window = max(1, window)

    xs, ts = [], []
    for i in range(n - window):
        xs.append(z[i:i + window])
        ts.append(z[i + window])
    if not xs:  # série trop courte pour une fenêtre : repli fenêtre 1.
        window = 1
        xs = [z[i:i + 1] for i in range(n - 1)]
        ts = [z[i + 1] for i in range(n - 1)]

    x_t = torch.tensor(np.array(xs), dtype=torch.float32).unsqueeze(-1)  # (N, window, 1)
    y_t = torch.tensor(np.array(ts), dtype=torch.float32).unsqueeze(-1)  # (N, 1)

    class _Net(nn.Module):
        def __init__(self) -> None:
            super().__init__()
            self.lstm = nn.LSTM(input_size=1, hidden_size=_HIDDEN, batch_first=True)
            self.head = nn.Linear(_HIDDEN, 1)

        def forward(self, seq: "torch.Tensor") -> "torch.Tensor":
            out, _ = self.lstm(seq)
            return self.head(out[:, -1, :])

    net = _Net()
    optim = torch.optim.Adam(net.parameters(), lr=0.05)
    loss_fn = nn.MSELoss()
    net.train()
    for _ in range(_EPOCHS):
        optim.zero_grad()
        loss = loss_fn(net(x_t), y_t)
        loss.backward()
        optim.step()

    net.eval()
    with torch.no_grad():
        fitted_z = net(x_t).squeeze(-1).numpy()
    # Résidus one-step en échelle d'origine.
    target_orig = np.array(ts) * std + mean
    fitted_orig = fitted_z * std + mean
    resid = target_orig - fitted_orig
    dof = max(resid.size - 2, 1)
    sigma = float(np.sqrt(np.sum(resid**2) / dof))
    ss_tot = float(np.sum((target_orig - target_orig.mean()) ** 2)) if target_orig.size else 0.0
    r2 = 1.0 if ss_tot == 0 else max(0.0, 1.0 - float(np.sum(resid**2)) / ss_tot)

    # Projection auto-régressive à l'horizon (en échelle standardisée).
    history = list(z[-window:])
    points: list[KpiForecastPoint] = []
    with torch.no_grad():
        for step in range(1, horizon + 1):
            seq = torch.tensor(np.array(history[-window:]), dtype=torch.float32).reshape(1, window, 1)
            pred_z = float(net(seq).item())
            history.append(pred_z)
            base = pred_z * std + mean
            half = _Z95 * sigma * math.sqrt(step)
            points.append(KpiForecastPoint(step=step, value=float(base),
                                           low=float(base - half), high=float(base + half)))

    last = points[-1]
    spread = (last.high - last.low) / (2 * _Z95) or 1e-12
    zt = (target - last.value) / spread
    p_above = 1.0 - _normal_cdf(zt)
    probability = p_above if direction == "at_least" else 1.0 - p_above

    if n < 8 or r2 < 0.2:
        confidence = "low"
    elif n < 15 or r2 < 0.5:
        confidence = "medium"
    else:
        confidence = "high"

    slope = float(points[1].value - points[0].value) if len(points) >= 2 else 0.0

    return KpiForecast(
        n=n, slope=slope, intercept=float(fitted_orig[-1]) if fitted_orig.size else mean,
        residual_sigma=sigma, r2=float(r2),
        horizon=horizon, target=float(target), direction=direction,
        probability=float(min(1.0, max(0.0, probability))), confidence=confidence,
        model="lstm", seasonal_period=int(seasonal_period) if use_season else 0,
        points=points,
    )


def _normal_cdf(z: float) -> float:
    return 0.5 * (1.0 + math.erf(z / math.sqrt(2.0)))
