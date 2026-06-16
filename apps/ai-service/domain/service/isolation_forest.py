"""Isolation Forest — détection d'anomalies non-supervisée (NumPy pur).

Implémentation fidèle de Liu, Ting & Zhou, *Isolation Forest* (ICDM 2008) :
les anomalies sont rares et différentes, donc **plus faciles à isoler** par des
partitions aléatoires — elles ont une longueur de chemin moyenne plus courte.

Algorithme :
1. Construire ``n_trees`` arbres d'isolation (iTrees), chacun sur un
   sous-échantillon de ``sample_size`` points tiré sans remise.
2. Chaque iTree partitionne récursivement : choix d'une feature aléatoire, d'une
   valeur de coupe aléatoire dans [min, max] de la feature au nœud, jusqu'à
   isoler un point ou atteindre la hauteur limite ``ceil(log2(sample_size))``.
3. Longueur de chemin ``h(x)`` d'un point = profondeur atteinte + ``c(taille)``
   (correction pour les nœuds externes non développés).
4. Score d'anomalie ``s(x) = 2^(-E[h(x)] / c(n))`` où ``c(n)`` est la longueur de
   chemin moyenne d'une recherche infructueuse dans un BST :
   ``c(n) = 2·H(n-1) − 2(n-1)/n`` (H = nombre harmonique).

Score proche de 1 → anomalie ; proche de 0.5 → normal. Déterministe via
``np.random.default_rng(seed)``. Aucune dépendance hors NumPy.
"""
from __future__ import annotations

import math

import numpy as np

# Bornes par défaut (article original).
_DEFAULT_N_TREES = 100
_DEFAULT_SAMPLE_SIZE = 256


def _c(n: int) -> float:
    """Longueur de chemin moyenne d'une recherche infructueuse dans un BST de n nœuds.

    ``c(n) = 2·H(n-1) − 2(n-1)/n`` avec H(i) ≈ ln(i) + γ (constante d'Euler).
    Sert à normaliser les longueurs de chemin (facteur d'échelle du score).
    """
    if n <= 1:
        return 0.0
    if n == 2:
        return 1.0
    harmonic = math.log(n - 1) + 0.5772156649015329  # γ d'Euler-Mascheroni
    return 2.0 * harmonic - 2.0 * (n - 1) / n


def _path_length(x: np.ndarray, node: "_Node", current_depth: int) -> float:
    """Profondeur de l'échantillon ``x`` dans un iTree (+ correction c() en feuille)."""
    while node.feature is not None:
        if x[node.feature] < node.split:
            node = node.left  # type: ignore[assignment]
        else:
            node = node.right  # type: ignore[assignment]
        current_depth += 1
    # Nœud externe : ajouter la longueur de chemin estimée du sous-arbre non développé.
    return current_depth + _c(node.size)


class _Node:
    """Nœud d'un iTree : interne (feature/split/left/right) ou externe (size)."""

    __slots__ = ("feature", "split", "left", "right", "size")

    def __init__(self) -> None:
        self.feature: int | None = None
        self.split: float = 0.0
        self.left: "_Node | None" = None
        self.right: "_Node | None" = None
        self.size: int = 0


def _build_tree(
    data: np.ndarray, depth: int, height_limit: int, rng: np.random.Generator
) -> _Node:
    """Construit récursivement un iTree par coupes aléatoires."""
    node = _Node()
    n = data.shape[0]
    if depth >= height_limit or n <= 1:
        node.size = n
        return node

    n_features = data.shape[1]
    # Choix d'une feature ayant de l'étendue (sinon on ne peut pas couper).
    feature_order = rng.permutation(n_features)
    for feature in feature_order:
        col = data[:, feature]
        lo, hi = float(col.min()), float(col.max())
        if hi > lo:
            split = rng.uniform(lo, hi)
            left_mask = col < split
            node.feature = int(feature)
            node.split = split
            node.left = _build_tree(data[left_mask], depth + 1, height_limit, rng)
            node.right = _build_tree(data[~left_mask], depth + 1, height_limit, rng)
            return node

    # Toutes les features constantes au nœud : points indiscernables → feuille.
    node.size = n
    return node


class IsolationForest:
    """Forêt d'isolation entraînée — sépare l'entraînement du scoring.

    Permet de scorer des points ARBITRAIRES contre un modèle FIXE (indispensable à
    l'explicabilité Kernel SHAP, qui évalue des points synthétiques). Immuable après
    construction.
    """

    __slots__ = ("_trees", "_norm")

    def __init__(self, trees: list, norm: float) -> None:
        self._trees = trees
        self._norm = norm

    def score(self, points: np.ndarray) -> np.ndarray:
        """Score d'anomalie ``s(x) ∈ [0, 1]`` de chaque ligne de ``points``."""
        p = np.asarray(points, dtype=float)
        if p.ndim == 1:
            p = p.reshape(1, -1)
        if self._norm <= 0.0:
            return np.full(p.shape[0], 0.5, dtype=float)
        out = np.empty(p.shape[0], dtype=float)
        for i in range(p.shape[0]):
            avg_path = sum(_path_length(p[i], t, 0) for t in self._trees) / len(self._trees)
            out[i] = math.pow(2.0, -avg_path / self._norm)
        return out


def build_forest(
    samples: np.ndarray,
    *,
    n_trees: int = _DEFAULT_N_TREES,
    sample_size: int = _DEFAULT_SAMPLE_SIZE,
    seed: int = 42,
) -> IsolationForest:
    """Construit une forêt d'isolation à partir de ``samples`` (fit unique).

    :raises ValueError: matrice vide ou paramètres invalides.
    """
    x = np.asarray(samples, dtype=float)
    if x.ndim != 2 or x.shape[0] == 0 or x.shape[1] == 0:
        raise ValueError("samples must be a non-empty 2D matrix")
    if n_trees < 1:
        raise ValueError("n_trees must be >= 1")
    if sample_size < 1:
        raise ValueError("sample_size must be >= 1")

    n = x.shape[0]
    rng = np.random.default_rng(seed)
    effective_sample = min(sample_size, n)
    # Hauteur limite = profondeur moyenne attendue (article original).
    height_limit = max(1, int(math.ceil(math.log2(max(effective_sample, 2)))))

    trees = []
    for _ in range(n_trees):
        if effective_sample < n:
            idx = rng.choice(n, size=effective_sample, replace=False)
            subsample = x[idx]
        else:
            subsample = x
        trees.append(_build_tree(subsample, 0, height_limit, rng))
    return IsolationForest(trees, _c(effective_sample))


def score_samples(
    samples: np.ndarray,
    *,
    n_trees: int = _DEFAULT_N_TREES,
    sample_size: int = _DEFAULT_SAMPLE_SIZE,
    seed: int = 42,
) -> np.ndarray:
    """Calcule le score d'anomalie ``s(x) ∈ [0, 1]`` de chaque échantillon.

    Conserve l'API historique : construit la forêt depuis ``samples`` et score
    ``samples`` (équivalent à ``build_forest(samples).score(samples)``).

    :param samples: matrice (n_échantillons × n_features), float.
    :returns: vecteur des scores (croissant avec l'anormalité).
    :raises ValueError: matrice vide ou paramètres invalides.
    """
    forest = build_forest(samples, n_trees=n_trees, sample_size=sample_size, seed=seed)
    return forest.score(np.asarray(samples, dtype=float))
