"""Use case : détection d'anomalies non-supervisée multivariée.

Couche applicative fine au-dessus des services de domaine NumPy purs
(``isolation_forest`` et ``reconstruction``) — symétrique de ``SpcDetectUseCase``,
et point d'accroche des préoccupations transverses (logging, future persistance).
Le tenant est porté pour l'auditabilité/multi-tenancy même si le calcul est
sans état (CLAUDE.md §3.4, §12.1).

Le drapeau d'anomalie se fait :
- par **seuil** explicite sur le score si ``threshold`` est fourni ;
- sinon par **quantile de contamination** : la fraction ``contamination`` des
  scores les plus élevés est marquée anormale.
"""
from __future__ import annotations

import logging

import numpy as np

from domain.model.anomaly import (
    METHOD_ISOLATION_FOREST,
    SUPPORTED_METHODS,
    AnomalyExplanation,
    AnomalyPoint,
    AnomalyResult,
    FeatureContribution,
)
from domain.model.tenant import TenantContext
from domain.service import isolation_forest, reconstruction, shap_kernel

logger = logging.getLogger(__name__)

# Garde-fous de taille (anti-DoS, cohérent avec les autres chemins IA).
_MAX_SAMPLES = 50_000
_MAX_FEATURES = 200
# Arrière-plan SHAP borné (échantillon de référence ; coût maîtrisé).
_SHAP_BACKGROUND = 256


class AnomalyDetectUseCase:
    """Matrice multivariée -> scores d'anomalie + drapeaux (Isolation Forest / ACP)."""

    def execute(
        self,
        samples: list[list[float]],
        tenant: TenantContext,
        *,
        method: str = METHOD_ISOLATION_FOREST,
        contamination: float = 0.1,
        threshold: float | None = None,
        seed: int = 42,
        n_trees: int = 100,
        sample_size: int = 256,
        n_components: int | None = None,
    ) -> AnomalyResult:
        """Détecte les anomalies de la matrice ``samples`` (échantillons × features).

        :raises ValueError: matrice invalide, méthode inconnue ou bornes dépassées.
        """
        if method not in SUPPORTED_METHODS:
            raise ValueError(f"unknown method: {method}")
        if not 0.0 < contamination <= 0.5:
            raise ValueError("contamination must be within (0, 0.5]")
        if not samples:
            raise ValueError("samples must be a non-empty matrix")

        matrix = _to_matrix(samples)
        n, n_features = matrix.shape

        scores, top_features = self._score(
            matrix, method, seed=seed, n_trees=n_trees,
            sample_size=sample_size, n_components=n_components,
        )

        effective_threshold, flags = self._flag(scores, contamination, threshold)

        points = [
            AnomalyPoint(
                index=i,
                score=float(scores[i]),
                is_anomaly=bool(flags[i]),
                top_feature=self._top_feature(top_features, i),
            )
            for i in range(n)
        ]
        anomaly_count = int(flags.sum())

        result = AnomalyResult(
            n=n,
            n_features=n_features,
            method=method,
            contamination=contamination,
            threshold=float(effective_threshold),
            anomaly_count=anomaly_count,
            points=points,
        )
        logger.info(
            "Anomaly detection tenant=%s method=%s n=%d features=%d anomalies=%d threshold=%.4f",
            tenant.tenant_id, method, n, n_features, anomaly_count, effective_threshold,
        )
        return result

    # ---- internes ----------------------------------------------------------

    def _score(
        self, matrix: np.ndarray, method: str, *, seed: int, n_trees: int,
        sample_size: int, n_components: int | None,
    ) -> tuple[np.ndarray, np.ndarray | None]:
        """Délègue au service de domaine ; renvoie (scores, features dominantes|None)."""
        if method == METHOD_ISOLATION_FOREST:
            scores = isolation_forest.score_samples(
                matrix, n_trees=n_trees, sample_size=sample_size, seed=seed,
            )
            return scores, None
        # METHOD_RECONSTRUCTION
        errors, top_features = reconstruction.reconstruction_errors(
            matrix, n_components=n_components,
        )
        return reconstruction.normalize_scores(errors), top_features

    def _flag(
        self, scores: np.ndarray, contamination: float, threshold: float | None,
    ) -> tuple[float, np.ndarray]:
        """Détermine le seuil effectif et le masque d'anomalies."""
        if threshold is not None:
            return float(threshold), scores >= threshold
        # Quantile de contamination : top fraction des scores. ``higher`` garantit
        # qu'au plus la fraction visée est marquée (le seuil tombe sur un score réel).
        quantile = float(np.quantile(scores, 1.0 - contamination, method="higher"))
        return quantile, scores >= quantile

    def _top_feature(self, top_features: np.ndarray | None, i: int) -> int | None:
        if top_features is None:
            return None
        value = int(top_features[i])
        return value if value >= 0 else None


class AnomalyExplainUseCase:
    """Explique le score d'anomalie d'UN échantillon par Kernel SHAP (§12.3).

    v1 : Isolation Forest uniquement (la reconstruction ACP expose déjà la feature
    dominante via ``top_feature``). La forêt est entraînée une fois sur la matrice ;
    SHAP attribue le score de l'échantillon ``index`` à ses features par rapport à
    l'arrière-plan (la matrice elle-même, bornée).
    """

    def execute(
        self,
        samples: list[list[float]],
        index: int,
        tenant: TenantContext,
        *,
        seed: int = 42,
        n_trees: int = 100,
        sample_size: int = 256,
    ) -> AnomalyExplanation:
        if not samples:
            raise ValueError("samples must be a non-empty matrix")
        matrix = _to_matrix(samples)
        n, n_features = matrix.shape
        if not 0 <= index < n:
            raise ValueError(f"index out of range (0..{n - 1})")

        forest = isolation_forest.build_forest(
            matrix, n_trees=n_trees, sample_size=sample_size, seed=seed)
        background = matrix if n <= _SHAP_BACKGROUND else matrix[
            np.random.default_rng(seed).choice(n, size=_SHAP_BACKGROUND, replace=False)]

        phi, base_value, score = shap_kernel.shapley_values(
            matrix[index], background, forest.score, seed=seed)

        contributions = [
            FeatureContribution(feature=j, value=float(matrix[index, j]),
                                contribution=float(phi[j]))
            for j in range(n_features)
        ]
        contributions.sort(key=lambda c: abs(c.contribution), reverse=True)

        logger.info(
            "Anomaly explain tenant=%s index=%d score=%.4f base=%.4f features=%d",
            tenant.tenant_id, index, score, base_value, n_features,
        )
        return AnomalyExplanation(
            index=index, method=METHOD_ISOLATION_FOREST, score=float(score),
            base_value=float(base_value), contributions=contributions,
        )


def _to_matrix(samples: list[list[float]]) -> np.ndarray:
    """Valide la forme (lignes de même longueur, finitude, bornes) → matrice float."""
    width = len(samples[0])
    if width == 0:
        raise ValueError("samples rows must have at least one feature")
    if any(len(row) != width for row in samples):
        raise ValueError("all sample rows must have the same length")
    if len(samples) > _MAX_SAMPLES:
        raise ValueError(f"too many samples (max {_MAX_SAMPLES})")
    if width > _MAX_FEATURES:
        raise ValueError(f"too many features (max {_MAX_FEATURES})")
    try:
        matrix = np.asarray(samples, dtype=float)
    except (TypeError, ValueError) as exc:
        raise ValueError("samples must contain only numbers") from exc
    if not np.all(np.isfinite(matrix)):
        raise ValueError("samples must contain only finite numbers")
    return matrix
