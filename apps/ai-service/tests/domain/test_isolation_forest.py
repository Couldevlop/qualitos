"""Tests du service Isolation Forest (NumPy pur, déterministe)."""
from __future__ import annotations

import numpy as np
import pytest

from domain.service import isolation_forest as iforest


def _gaussian_cloud_with_outlier(seed: int = 7):
    """Nuage gaussien 2D + 1 point aberrant éloigné — vérité-terrain connue."""
    rng = np.random.default_rng(seed)
    cloud = rng.normal(loc=0.0, scale=1.0, size=(60, 2))
    outlier = np.array([[12.0, -12.0]])
    return np.vstack([cloud, outlier])  # l'aberrant est le dernier index (60)


def test_detects_injected_outlier():
    data = _gaussian_cloud_with_outlier()
    scores = iforest.score_samples(data, n_trees=120, sample_size=64, seed=1)
    # Le point aberrant injecté doit avoir le score le plus élevé.
    assert int(np.argmax(scores)) == data.shape[0] - 1
    assert scores[-1] > float(np.median(scores))


def test_deterministic_with_fixed_seed():
    data = _gaussian_cloud_with_outlier()
    a = iforest.score_samples(data, n_trees=50, sample_size=32, seed=99)
    b = iforest.score_samples(data, n_trees=50, sample_size=32, seed=99)
    np.testing.assert_array_equal(a, b)


def test_scores_in_unit_range():
    data = _gaussian_cloud_with_outlier()
    scores = iforest.score_samples(data, n_trees=40, sample_size=48, seed=3)
    assert np.all(scores >= 0.0) and np.all(scores <= 1.0)


def test_single_point_is_neutral():
    # Un seul échantillon : aucune isolation possible → score neutre 0.5.
    scores = iforest.score_samples(np.array([[1.0, 2.0]]), n_trees=10, sample_size=4)
    assert scores.shape == (1,)
    assert scores[0] == pytest.approx(0.5)


def test_constant_columns_do_not_crash():
    # Toutes les features constantes : aucune coupe possible, feuille immédiate.
    data = np.ones((20, 3))
    scores = iforest.score_samples(data, n_trees=10, sample_size=8, seed=5)
    assert scores.shape == (20,)
    assert np.all(np.isfinite(scores))


def test_c_helper_edge_cases():
    assert iforest._c(0) == 0.0
    assert iforest._c(1) == 0.0
    assert iforest._c(2) == 1.0
    assert iforest._c(10) > 0.0


def test_rejects_empty_matrix():
    with pytest.raises(ValueError):
        iforest.score_samples(np.empty((0, 3)))


def test_rejects_bad_params():
    data = _gaussian_cloud_with_outlier()
    with pytest.raises(ValueError):
        iforest.score_samples(data, n_trees=0)
    with pytest.raises(ValueError):
        iforest.score_samples(data, sample_size=0)


def test_rejects_1d_input():
    with pytest.raises(ValueError):
        iforest.score_samples(np.array([1.0, 2.0, 3.0]))
