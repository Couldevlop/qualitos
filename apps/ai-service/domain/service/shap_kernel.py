"""Explicabilité model-agnostic par Kernel SHAP (NumPy pur).

Attribue la sortie d'une fonction de score ``f(X) -> scores`` à chacune des
features d'un échantillon, par rapport à un arrière-plan (background) — réponse à
l'invariant §12.3 « toute prédiction/recommandation montre ses sources ». Les
valeurs de Shapley sont les seules attributions vérifiant efficacité, symétrie,
nullité et additivité ; Kernel SHAP (Lundberg & Lee, NeurIPS 2017) les estime par
**régression linéaire pondérée** sur des coalitions de features, où une feature
« absente » est remplacée par sa valeur de background (imputation marginale).

NumPy pur, déterministe (graine fixe pour l'échantillonnage des coalitions),
aucune dépendance lourde — cohérent avec le reste de la couche domaine. La vraie
lib `shap` (C++/TreeSHAP) pourra s'y substituer derrière le même contrat.

Propriété d'efficacité garantie : Σ φ_i = f(x) − E[f(background)].
"""
from __future__ import annotations

import math
from collections.abc import Callable

import numpy as np

# Au-delà de ce nombre de features, on échantillonne les coalitions (sinon 2^d exact).
_EXACT_MAX_FEATURES = 12
_DEFAULT_SAMPLES = 512


def shapley_values(
    x: np.ndarray,
    background: np.ndarray,
    predict: Callable[[np.ndarray], np.ndarray],
    *,
    nsamples: int = _DEFAULT_SAMPLES,
    seed: int = 42,
) -> tuple[np.ndarray, float, float]:
    """Valeurs de Shapley de l'échantillon ``x`` pour la fonction ``predict``.

    :param x: vecteur de l'échantillon expliqué (d,).
    :param background: matrice de référence (m × d) — l'« absence » d'une feature
        est imputée par tirage dans ces lignes (espérance marginale).
    :param predict: fonction vectorisée matrice (k × d) -> scores (k,).
    :returns: (phi (d,), base_value E[f(bg)], prediction f(x)).
    :raises ValueError: dimensions incohérentes ou entrées vides.
    """
    x = np.asarray(x, dtype=float).ravel()
    bg = np.asarray(background, dtype=float)
    if bg.ndim != 2 or bg.shape[0] == 0:
        raise ValueError("background must be a non-empty 2D matrix")
    d = x.size
    if bg.shape[1] != d:
        raise ValueError("x and background must share the feature dimension")

    rng = np.random.default_rng(seed)
    base_value = float(np.mean(predict(bg)))
    fx = float(predict(x.reshape(1, -1))[0])

    if d == 0:
        return np.zeros(0), base_value, fx
    if d == 1:
        return np.array([fx - base_value]), base_value, fx

    masks = _coalitions(d, nsamples, rng)
    # Valeur de chaque coalition : moyenne de f sur le background imputé.
    values = _coalition_values(masks, x, bg, predict, rng)

    phi = _weighted_least_squares(masks, values, base_value, fx, d)
    return phi, base_value, fx


def _coalitions(d: int, nsamples: int, rng: np.random.Generator) -> np.ndarray:
    """Matrice booléenne (k × d) des coalitions (features présentes=True).

    d ≤ _EXACT_MAX_FEATURES → toutes les coalitions non triviales (2^d − 2).
    Sinon → tirage pondéré par le noyau SHAP (tailles de coalition fréquentes).
    """
    if d <= _EXACT_MAX_FEATURES:
        rows = []
        for bits in range(1, (1 << d) - 1):  # exclut coalition vide et pleine
            rows.append([(bits >> j) & 1 for j in range(d)])
        return np.array(rows, dtype=bool)

    # Échantillonnage : taille z ∈ [1, d-1] ∝ poids du noyau SHAP, puis sous-ensemble.
    sizes = np.arange(1, d)
    kernel_w = (d - 1) / (_comb(d, sizes) * sizes * (d - sizes))
    probs = kernel_w / kernel_w.sum()
    masks = np.zeros((nsamples, d), dtype=bool)
    for i in range(nsamples):
        z = int(rng.choice(sizes, p=probs))
        idx = rng.choice(d, size=z, replace=False)
        masks[i, idx] = True
    return masks


# Budget de lignes synthétiques (k coalitions × r backgrounds) — borne le coût.
_SYNTH_BUDGET = 200_000


def _coalition_values(
    masks: np.ndarray, x: np.ndarray, bg: np.ndarray,
    predict: Callable[[np.ndarray], np.ndarray], rng: np.random.Generator,
) -> np.ndarray:
    """Pour chaque coalition S : E[f(z)] où z = x sur S, background ailleurs.

    Espérance d'imputation marginale : **exacte** (moyenne sur TOUT le background,
    déterministe) tant que ``k·m`` tient dans le budget ; sinon estimée par moyenne
    sur ``r`` tirages aléatoires (réduit la variance ~√r, coût borné).
    """
    k, d = masks.shape
    m = bg.shape[0]
    xb = np.broadcast_to(x, (k, d))
    acc = np.zeros(k, dtype=float)

    if k * m <= _SYNTH_BUDGET:
        # Exact : chaque ligne de background contribue à chaque coalition.
        for j in range(m):
            synth = np.tile(bg[j], (k, 1))
            synth[masks] = xb[masks]
            acc += predict(synth)
        return acc / m

    r = min(m, max(16, _SYNTH_BUDGET // max(k, 1)))
    for _ in range(r):
        bg_idx = rng.integers(0, m, size=k)
        synth = bg[bg_idx].copy()
        synth[masks] = xb[masks]
        acc += predict(synth)
    return acc / r


def _weighted_least_squares(
    masks: np.ndarray, values: np.ndarray, base_value: float, fx: float, d: int,
) -> np.ndarray:
    """Régression pondérée (noyau SHAP) sous contrainte d'efficacité Σφ = fx − base.

    On résout pour φ en imposant la contrainte par substitution de la dernière
    feature : φ_d = (fx − base) − Σ_{i<d} φ_i.
    """
    sizes = masks.sum(axis=1).astype(float)
    weights = _shap_kernel_weights(sizes, d)

    # Cible centrée : y = f(S) − base − (présence de la feature d) · (fx − base)
    eff = fx - base_value
    y = values - base_value - masks[:, -1].astype(float) * eff
    # Variables : différences (présence_i − présence_d) pour i < d
    last = masks[:, -1].astype(float)
    a = masks[:, :-1].astype(float) - last[:, None]

    w = weights
    aw = a * w[:, None]
    lhs = a.T @ aw
    rhs = a.T @ (w * y)
    # Régularisation de Tikhonov minime pour la stabilité numérique.
    lhs += 1e-8 * np.eye(d - 1)
    phi_head = np.linalg.solve(lhs, rhs)
    phi_last = eff - phi_head.sum()
    return np.concatenate([phi_head, [phi_last]])


def _shap_kernel_weights(sizes: np.ndarray, d: int) -> np.ndarray:
    """Poids du noyau SHAP π(z) = (d−1) / (C(d,z)·z·(d−z))."""
    z = np.clip(sizes, 1, d - 1)
    return (d - 1) / (_comb(d, z) * z * (d - z))


def _comb(n: int, k: np.ndarray | int) -> np.ndarray:
    """Coefficient binomial C(n, k) vectorisé (via lgamma, stable)."""
    k_arr = np.asarray(k, dtype=float)
    return np.round(np.exp(
        math.lgamma(n + 1) - _lgamma(k_arr + 1) - _lgamma(n - k_arr + 1)
    ))


def _lgamma(arr: np.ndarray) -> np.ndarray:
    return np.array([math.lgamma(v) for v in np.atleast_1d(arr)]).reshape(np.shape(arr))
