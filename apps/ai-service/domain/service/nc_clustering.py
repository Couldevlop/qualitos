"""Pure NC clustering — TF-IDF + similarité cosinus + composantes connexes.

Détecte les groupes de non-conformités similaires (patterns invisibles à l'œil
— §1.4, §6.5) sans dépendance lourde : NumPy seul, déterministe, explicable
(top-termes par cluster). Un HDBSCAN sur embeddings BGE-M3 pourra remplacer
cette V1 derrière le même contrat quand le GPU sera disponible.

Algorithme :
1. Tokenisation (minuscules, désaccentuation, stop-words FR/EN minimaux).
2. Matrice TF-IDF (lissage standard), normalisation L2.
3. Similarité cosinus ; arête si ≥ ``threshold`` (défaut 0.35).
4. Clusters = composantes connexes de taille ≥ 2 (union-find) ; le reste = bruit.
"""
from __future__ import annotations

import math
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


def cluster(texts: list[str], *, threshold: float = 0.35) -> NcClusteringResult:
    """Regroupe les textes similaires. Liste vide → résultat vide.

    :raises ValueError: trop de textes ou seuil hors (0, 1).
    """
    if not 0.0 < threshold < 1.0:
        raise ValueError("threshold must be within (0, 1)")
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

    # TF-IDF lissé + normalisation L2.
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
    tfidf /= norms

    sim = tfidf @ tfidf.T

    # Union-find sur les paires au-dessus du seuil.
    parent = list(range(n))

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for i in range(n):
        for j in range(i + 1, n):
            if sim[i, j] >= threshold:
                ri, rj = find(i), find(j)
                if ri != rj:
                    parent[rj] = ri

    groups: dict[int, list[int]] = {}
    for i in range(n):
        groups.setdefault(find(i), []).append(i)

    clusters: list[NcCluster] = []
    noise: list[int] = []
    inv_vocab = {v: k for k, v in vocab.items()}
    cluster_id = 0
    for members in sorted(groups.values(), key=lambda m: (-len(m), m[0])):
        if len(members) < 2:
            noise.extend(members)
            continue
        centroid = tfidf[members].mean(axis=0)
        top = [inv_vocab[k] for k in np.argsort(-centroid)[:5] if centroid[k] > 0]
        clusters.append(NcCluster(cluster_id=cluster_id, indices=sorted(members),
                                  size=len(members), top_terms=top))
        cluster_id += 1

    return NcClusteringResult(n=n, clusters=clusters, noise_indices=sorted(noise))


def _tokenize(text: str) -> list[str]:
    norm = unicodedata.normalize("NFD", (text or "").lower())
    norm = "".join(c for c in norm if unicodedata.category(c) != "Mn")
    return [t for t in _TOKEN.findall(norm) if t not in _STOPWORDS]
