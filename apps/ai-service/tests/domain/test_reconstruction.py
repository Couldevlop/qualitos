"""Tests du détecteur par reconstruction ACP (auto-encodeur linéaire, NumPy pur)."""
from __future__ import annotations

import numpy as np
import pytest

from domain.service import reconstruction as recon


def _collinear_with_outlier():
    """Données quasi sur une droite (variété 1D) + 1 point hors de cette droite."""
    t = np.linspace(0.0, 10.0, 40)
    # x2 ≈ 2·x1 : variance portée par 1 composante principale.
    cloud = np.column_stack([t, 2.0 * t])
    outlier = np.array([[5.0, -8.0]])  # loin de la droite y = 2x
    return np.vstack([cloud, outlier])  # aberrant = dernier index (40)


def test_detects_off_manifold_outlier():
    data = _collinear_with_outlier()
    errors, top = recon.reconstruction_errors(data, n_components=1)
    # Le point hors de la variété principale a la plus grande erreur.
    assert int(np.argmax(errors)) == data.shape[0] - 1
    # Une feature dominante est attribuée à l'aberrant (explicabilité).
    assert top[-1] in (0, 1)


def test_deterministic():
    data = _collinear_with_outlier()
    e1, _ = recon.reconstruction_errors(data, n_components=1)
    e2, _ = recon.reconstruction_errors(data, n_components=1)
    np.testing.assert_allclose(e1, e2)


def test_normalize_scores_unit_range():
    errors = np.array([0.0, 1.0, 2.0, 4.0])
    scores = recon.normalize_scores(errors)
    assert scores.max() == pytest.approx(1.0)
    assert scores.min() == pytest.approx(0.0)
    assert np.all(scores >= 0.0) and np.all(scores <= 1.0)


def test_normalize_scores_all_zero():
    scores = recon.normalize_scores(np.zeros(5))
    assert np.all(scores == 0.0)


def test_auto_component_selection_runs():
    rng = np.random.default_rng(0)
    data = rng.normal(size=(50, 5))
    errors, top = recon.reconstruction_errors(data)  # k auto
    assert errors.shape == (50,)
    assert top.shape == (50,)
    assert np.all(errors >= 0.0)


def test_single_sample_degenerate():
    errors, top = recon.reconstruction_errors(np.array([[1.0, 2.0, 3.0]]))
    assert errors.shape == (1,)
    assert errors[0] == 0.0
    assert top[0] == -1


def test_single_feature_degenerate():
    errors, top = recon.reconstruction_errors(np.array([[1.0], [2.0], [3.0]]))
    assert np.all(errors == 0.0)
    assert np.all(top == -1)


def test_perfect_reconstruction_no_top_feature():
    # Données parfaitement 1D : reconstruction exacte avec 1 composante → erreurs ~0.
    data = np.column_stack([np.arange(10.0), 3.0 * np.arange(10.0)])
    errors, top = recon.reconstruction_errors(data, n_components=1)
    assert np.allclose(errors, 0.0, atol=1e-9)
    assert np.all(top == -1)


def test_rejects_empty_matrix():
    with pytest.raises(ValueError):
        recon.reconstruction_errors(np.empty((0, 2)))


def test_choose_k_zero_variance():
    # Valeurs singulières nulles → au moins 1 composante.
    assert recon._choose_k(np.zeros(3), None, 0.95, 4) == 1


def test_choose_k_explicit_clamped():
    # k explicite borné à n_features - 1 pour laisser un résidu.
    assert recon._choose_k(np.array([3.0, 2.0, 1.0]), 10, 0.95, 4) == 3
    assert recon._choose_k(np.array([3.0, 2.0, 1.0]), 0, 0.95, 4) == 1
