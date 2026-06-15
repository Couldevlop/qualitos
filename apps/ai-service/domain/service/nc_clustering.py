"""Clustering de non-conformités — TF-IDF + **DBSCAN** (densité, NumPy pur).

Détecte les groupes de NC similaires (patterns invisibles à l'œil — §1.4, §6.5,
§12.1) sans dépendance lourde : NumPy seul, déterministe, explicable (top-termes
par cluster). DBSCAN (Ester et al., KDD 1996) remplace le simple regroupement par
composantes connexes : il introduit la **densité** (points-cœur ayant ≥
``min_samples`` voisins) et distingue proprement clusters denses et **bruit**, là où
les composantes connexes fusionnaient par simple chaînage.

HDBSCAN (densité variable, hiérarchique) sur embeddings BGE-M3 pourra remplacer
cette v1 derrière le **même contrat** ``NcClusteringResult`` quand un budget GPU
existera (même logique que les modèles NLQ / ADR 0014 §7.3).

Algorithme :
1. Tokenisation (minuscules, désaccentuation, stop-words FR/EN minimaux).
2. Matrice TF-IDF (lissage standard), normalisation L2 → similarité cosinus = produit.
3. Voisinage ε : ``sim(i, j) ≥ threshold`` (distance cosinus ≤ 1 − threshold).
4. DBSCAN : points-cœur (≥ ``min_samples`` voisins, soi inclus) → expansion par
   atteignabilité de densité ; le reste = bruit. Parcours en ordre d'indice
   (déterministe).
"""
from __future__ import annotations

import re
import unicodedata

import numpy as np

from domain.model.predict import NcCluster, NcClusteringResult

_MAX_TEXTS = 2000
_TOKEN = re.compile(r"[a-z0-9]{2,}")
_STOPWORDS = frozenset(
    "le la les un une des de du au aux et ou sur dans pour par avec sans est sont "
    "ce cette ces il elle on nous vous ils elles ne pas plus tres a the of and or "
    "in on for with is are was to at an be this that it as by from".split()
)


def cluster(texts: list[str], *, threshold: float = 0.35, min_samples: int = 2) -> NcClusteringResult:
    """Regroupe les textes similaires par densité (DBSCAN). Liste vide → résultat vide.

    :param threshold: similarité cosinus minimale pour un voisinage (ε = 1 − threshold).
    :param min_samples: taille minimale du voisinage (soi inclus) d'un point-cœur.
    :raises ValueError: trop de textes, seuil hors (0, 1) ou min_samples < 2.
    """
    if not 0.0 < threshold < 1.0:
        raise ValueError("threshold must be within (0, 1)")
    if min_samples < 2:
        raise ValueError("min_samples must be >= 2")
    if len(texts) > _MAX_TEXTS:
        raise ValueError(f"too many texts (max {_MAX_TEXTS})")
    n = len(texts)
    if n == 0:
        return NcClusteringResult(n=0)

    docs = [_tokenize(t) for t in texts]
    vocab: dict[str, int] = {}
    for tokens in docs:
        for tok in tokens:
            vocab.setdefault(tok, len(vocab))
    if not vocab:
        return NcClusteringResult(n=n, noise_indices=list(range(n)))

    tfidf = _tfidf_matrix(docs, vocab, n)
    sim = tfidf @ tfidf.T  # cosinus (vecteurs L2-normalisés)

    labels = _dbscan(sim, threshold, min_samples)

    return _assemble(labels, tfidf, vocab, n)


# --- DBSCAN sur matrice de similarité ---------------------------------------------


def _dbscan(sim: np.ndarray, threshold: float, min_samples: int) -> np.ndarray:
    """Étiquette chaque point : −1 = bruit, ≥ 0 = identifiant de cluster.

    Voisinage ε = {j : sim[i, j] ≥ threshold} (inclut i). Point-cœur si |voisinage|
    ≥ min_samples. Parcours en ordre d'indice → déterministe.
    """
    n = sim.shape[0]
    # neighbors[i] = indices j (≠ rien d'exclu) au-dessus du seuil, i inclus.
    neighbors = [np.flatnonzero(sim[i] >= threshold) for i in range(n)]
    is_core = np.array([nb.size >= min_samples for nb in neighbors])

    labels = np.full(n, -1, dtype=int)
    visited = np.zeros(n, dtype=bool)
    cluster_id = 0

    for i in range(n):
        if visited[i] or not is_core[i]:
            continue
        # Nouveau cluster : expansion par atteignabilité de densité (BFS déterministe).
        labels[i] = cluster_id
        visited[i] = True
        queue = [int(j) for j in neighbors[i]]
        head = 0
        while head < len(queue):
            j = queue[head]
            head += 1
            if labels[j] == -1:
                labels[j] = cluster_id  # point de bordure rattaché
            if visited[j]:
                continue
            visited[j] = True
            if is_core[j]:
                for k in neighbors[j]:
                    queue.append(int(k))
        cluster_id += 1

    return labels


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

    return NcClusteringResult(n=n, clusters=clusters, noise_indices=sorted(noise))


# --- Vectorisation TF-IDF ---------------------------------------------------------


def _tfidf_matrix(docs: list[list[str]], vocab: dict[str, int], n: int) -> np.ndarray:
    """TF-IDF lissé + normalisation L2 (vecteurs lignes)."""
    tf = np.zeros((n, len(vocab)), dtype=float)
    for i, tokens in enumerate(docs):
        for tok in tokens:
            tf[i, vocab[tok]] += 1.0
        if tokens:
            tf[i] /= len(tokens)
    df = np.count_nonzero(tf > 0, axis=0)
    idf = np.log((1.0 + n) / (1.0 + df)) + 1.0
    tfidf = tf * idf
    norms = np.linalg.norm(tfidf, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    return tfidf / norms


def _tokenize(text: str) -> list[str]:
    norm = unicodedata.normalize("NFD", (text or "").lower())
    norm = "".join(c for c in norm if unicodedata.category(c) != "Mn")
    return [t for t in _TOKEN.findall(norm) if t not in _STOPWORDS]
