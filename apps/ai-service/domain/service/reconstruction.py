"""Détecteur d'anomalies par reconstruction — auto-encodeur LINÉAIRE via ACP.

Méthode non-supervisée multivariée reconnue : on apprend le sous-espace principal
des données (ACP par SVD), on projette puis reconstruit chaque échantillon, et on
mesure l'**erreur de reconstruction** L2. Les points mal reconstruits (loin de la
variété principale) sont anormaux. C'est l'équivalent linéaire d'un auto-encodeur
(encodeur = projection sur les k composantes, décodeur = reconstruction) — à ne PAS
confondre avec un réseau de neurones (CLAUDE.md §3.4, §12.1).

Algorithme :
1. Centrer les données (soustraire la moyenne par feature).
2. SVD : ``X_c = U·S·Vᵀ``. Les lignes de ``Vᵀ`` sont les axes principaux.
3. Choisir ``k`` composantes (par défaut : assez pour ~95 % de variance, borné).
4. Reconstruire ``X̂ = X_c · Vₖ · Vₖᵀ`` (+ moyenne), erreur = ‖x − x̂‖₂.
5. Feature dominante = celle de plus grand résidu absolu (explicabilité).

Déterministe (SVD), aucune dépendance hors NumPy.
"""
from __future__ import annotations

import numpy as np

_DEFAULT_VARIANCE_KEPT = 0.95


def reconstruction_errors(
    samples: np.ndarray,
    *,
    n_components: int | None = None,
    variance_kept: float = _DEFAULT_VARIANCE_KEPT,
) -> tuple[np.ndarray, np.ndarray]:
    """Erreur de reconstruction L2 par échantillon + feature dominante.

    :param samples: matrice (n_échantillons × n_features), float.
    :param n_components: nombre de composantes ACP retenues. Si None, on retient
        le minimum couvrant ``variance_kept`` de la variance (au moins 1, au plus
        n_features − 1 pour qu'une erreur de reconstruction puisse exister).
    :returns: (erreurs L2 par point, index de feature dominante par point).
    :raises ValueError: matrice vide.
    """
    x = np.asarray(samples, dtype=float)
    if x.ndim != 2 or x.shape[0] == 0 or x.shape[1] == 0:
        raise ValueError("samples must be a non-empty 2D matrix")

    n, n_features = x.shape
    mean = x.mean(axis=0)
    x_centered = x - mean

    # Cas dégénérés : 1 seul échantillon ou 1 seule feature → pas de sous-espace
    # « résiduel » exploitable. Erreurs nulles, pas de feature dominante.
    if n == 1 or n_features == 1:
        return np.zeros(n, dtype=float), np.full(n, -1, dtype=int)

    # SVD économique : Vt contient les axes principaux (lignes).
    _, singular, vt = np.linalg.svd(x_centered, full_matrices=False)

    k = _choose_k(singular, n_components, variance_kept, n_features)
    components = vt[:k]  # (k × n_features)

    # Reconstruction par projection sur le sous-espace principal.
    projected = x_centered @ components.T  # (n × k)
    reconstructed = projected @ components  # (n × n_features)
    residual = x_centered - reconstructed

    errors = np.linalg.norm(residual, axis=1)
    # Feature qui porte le plus grand résidu absolu (explicabilité, §12.3).
    top_feature = np.argmax(np.abs(residual), axis=1).astype(int)
    # Reconstruction (quasi) parfaite : pas de feature « contributrice ». Le seuil est
    # relatif à l'échelle des données pour absorber le bruit numérique de la SVD.
    scale = float(np.abs(x_centered).max())
    eps = 1e-9 * max(scale, 1.0)
    top_feature = np.where(errors > eps, top_feature, -1)
    return errors, top_feature


def _choose_k(
    singular: np.ndarray, n_components: int | None, variance_kept: float, n_features: int
) -> int:
    """Nombre de composantes retenues (borné à n_features − 1 pour laisser un résidu)."""
    max_k = max(1, n_features - 1)
    if n_components is not None:
        return int(min(max(1, n_components), max_k))

    variance = singular**2
    total = float(variance.sum())
    if total <= 0.0:
        return 1
    cumulative = np.cumsum(variance) / total
    # Première composante atteignant le seuil de variance ; +1 car index 0-based.
    k = int(np.searchsorted(cumulative, variance_kept) + 1)
    return min(max(1, k), max_k)


def normalize_scores(errors: np.ndarray) -> np.ndarray:
    """Normalise les erreurs L2 dans [0, 1] par l'erreur maximale du lot.

    Score relatif au lot (comme les détecteurs par seuil de contamination) :
    1.0 = pire reconstruction observée, 0.0 = parfaite. Lot homogène (max nul) →
    tous les scores à 0.
    """
    arr = np.asarray(errors, dtype=float)
    peak = float(arr.max()) if arr.size else 0.0
    if peak <= 0.0:
        return np.zeros_like(arr)
    return arr / peak
