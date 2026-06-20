"""Backend de clustering NC **HDBSCAN** (opt-in, import paresseux — ADR 0031).

Vrai HDBSCAN (densité variable, hiérarchique — Campello et al. 2013) derrière le
contrat ``NcClusteringResult`` existant. Réutilise exactement la même
vectorisation TF-IDF / tokenisation que le backend DBSCAN par défaut
(:mod:`domain.service.nc_clustering`) : seul l'algorithme d'agrégation change,
la sémantique « top-termes par cluster + bruit » est identique.

La distance fournie à HDBSCAN est la **distance cosinus précalculée**
(``1 − similarité``), cohérente avec le seuil DBSCAN du défaut.

``hdbscan`` est **lourd** (dépend de scikit-learn/Cython) : il vit dans l'extra
``ml`` du ``pyproject.toml`` et n'est PAS installé en CI. L'import est fait
**dans** :func:`cluster` ; son absence lève :class:`MlBackendUnavailableError`
(→ 422), jamais un faux résultat.
"""
from __future__ import annotations

import numpy as np

from domain.model.predict import NcCluster, NcClusteringResult
from domain.service import nc_clustering as _base
from domain.service.ml_backends import MlBackendUnavailableError

_MAX_TEXTS = 2000


def cluster(texts: list[str], *, min_samples: int = 2) -> NcClusteringResult:
    """Regroupe les textes par densité variable (HDBSCAN). Même contrat que le défaut.

    :param min_samples: ``min_cluster_size`` HDBSCAN (taille mini d'un cluster, ≥ 2).
    :raises ValueError: trop de textes ou ``min_samples`` < 2.
    :raises MlBackendUnavailableError: ``hdbscan`` non installé (extra ml).
    """
    if min_samples < 2:
        raise ValueError("min_samples must be >= 2")
    if len(texts) > _MAX_TEXTS:
        raise ValueError(f"too many texts (max {_MAX_TEXTS})")
    n = len(texts)
    if n == 0:
        return NcClusteringResult(n=0, method="hdbscan")

    try:  # import paresseux : hdbscan n'est tiré que si ce backend est choisi.
        import hdbscan as _hdbscan
    except ImportError as exc:  # pragma: no cover - exercé sans la lib en CI via le wrapper
        raise MlBackendUnavailableError("hdbscan", "hdbscan") from exc

    # Vectorisation identique au défaut (réutilise les helpers privés validés).
    docs = [_base._tokenize(t) for t in texts]
    vocab: dict[str, int] = {}
    for tokens in docs:
        for tok in tokens:
            vocab.setdefault(tok, len(vocab))
    if not vocab:
        return NcClusteringResult(n=n, noise_indices=list(range(n)), method="hdbscan")

    tfidf = _base._tfidf_matrix(docs, vocab, n)
    sim = tfidf @ tfidf.T  # cosinus (vecteurs L2-normalisés)
    distance = np.clip(1.0 - sim, 0.0, None)
    np.fill_diagonal(distance, 0.0)

    clusterer = _hdbscan.HDBSCAN(
        metric="precomputed",
        min_cluster_size=max(2, int(min_samples)),
        min_samples=int(min_samples),
        allow_single_cluster=True,
    )
    labels = clusterer.fit_predict(distance.astype(np.float64))

    return _assemble(np.asarray(labels), tfidf, vocab, n)


def _assemble(labels: np.ndarray, tfidf: np.ndarray, vocab: dict[str, int], n: int
              ) -> NcClusteringResult:
    """Construit le résultat : clusters triés (taille décroissante) + bruit + top-termes."""
    inv_vocab = {v: k for k, v in vocab.items()}
    groups: dict[int, list[int]] = {}
    noise: list[int] = []
    for i in range(n):
        lbl = int(labels[i])
        if lbl < 0:
            noise.append(i)
        else:
            groups.setdefault(lbl, []).append(i)

    clusters: list[NcCluster] = []
    cluster_id = 0
    for members in sorted(groups.values(), key=lambda m: (-len(m), m[0])):
        centroid = tfidf[members].mean(axis=0)
        top = [inv_vocab[k] for k in np.argsort(-centroid)[:5] if centroid[k] > 0]
        clusters.append(NcCluster(cluster_id=cluster_id, indices=sorted(members),
                                  size=len(members), top_terms=top))
        cluster_id += 1

    return NcClusteringResult(n=n, clusters=clusters, noise_indices=sorted(noise),
                              method="hdbscan")
