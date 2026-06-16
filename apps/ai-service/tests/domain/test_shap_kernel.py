"""Tests de l'explicabilité Kernel SHAP (NumPy pur)."""
import numpy as np
import pytest

from domain.service import shap_kernel


class TestShapleyValues:
    def test_efficiency_property(self):
        # Σ φ_i = f(x) − E[f(background)] (propriété fondamentale de Shapley).
        rng = np.random.default_rng(0)
        bg = rng.normal(size=(40, 5))
        x = np.array([3.0, -2.0, 0.5, 1.0, -1.0])
        w = np.array([1.0, -2.0, 0.0, 0.5, 3.0])
        predict = lambda X: X @ w  # noqa: E731
        phi, base, fx = shap_kernel.shapley_values(x, bg, predict, seed=1)
        assert phi.sum() == pytest.approx(fx - base, abs=1e-6)

    def test_additive_model_recovers_weighted_offset(self):
        # Pour f(x)=w·x, φ_i ≈ w_i·(x_i − E[bg_i]).
        rng = np.random.default_rng(2)
        bg = rng.normal(size=(200, 4))
        x = np.array([2.0, 0.0, -3.0, 1.0])
        w = np.array([1.5, 2.0, -1.0, 0.5])
        predict = lambda X: X @ w  # noqa: E731
        phi, _, _ = shap_kernel.shapley_values(x, bg, predict, seed=3)
        expected = w * (x - bg.mean(axis=0))
        # Tolérance large (Monte-Carlo sur l'imputation), l'ordre de grandeur et le
        # signe doivent coïncider.
        assert np.allclose(phi, expected, atol=0.25)

    def test_zero_contribution_for_irrelevant_feature(self):
        rng = np.random.default_rng(4)
        bg = rng.normal(size=(100, 3))
        x = np.array([5.0, 5.0, 5.0])
        w = np.array([2.0, 0.0, -1.0])  # feature 1 sans effet
        predict = lambda X: X @ w  # noqa: E731
        phi, _, _ = shap_kernel.shapley_values(x, bg, predict, seed=5)
        assert abs(phi[1]) < 0.2

    def test_deterministic(self):
        bg = np.arange(20.0).reshape(10, 2)
        x = np.array([100.0, -50.0])
        predict = lambda X: X.sum(axis=1)  # noqa: E731
        a = shap_kernel.shapley_values(x, bg, predict, seed=7)
        b = shap_kernel.shapley_values(x, bg, predict, seed=7)
        assert np.array_equal(a[0], b[0])

    def test_single_feature(self):
        bg = np.array([[1.0], [2.0], [3.0]])
        x = np.array([10.0])
        predict = lambda X: (X[:, 0] ** 2)  # noqa: E731
        phi, base, fx = shap_kernel.shapley_values(x, bg, predict, seed=0)
        assert phi[0] == pytest.approx(fx - base, abs=1e-9)

    def test_sampled_path_for_many_features(self):
        # d > seuil exact → échantillonnage des coalitions ; efficacité conservée.
        rng = np.random.default_rng(9)
        bg = rng.normal(size=(60, 16))
        x = rng.normal(size=16)
        w = rng.normal(size=16)
        predict = lambda X: X @ w  # noqa: E731
        phi, base, fx = shap_kernel.shapley_values(x, bg, predict, nsamples=400, seed=11)
        assert phi.sum() == pytest.approx(fx - base, abs=1e-6)

    def test_bad_background_rejected(self):
        with pytest.raises(ValueError):
            shap_kernel.shapley_values(np.array([1.0, 2.0]), np.zeros((0, 2)), lambda X: X.sum(axis=1))

    def test_dimension_mismatch_rejected(self):
        with pytest.raises(ValueError):
            shap_kernel.shapley_values(np.array([1.0, 2.0]), np.ones((5, 3)), lambda X: X.sum(axis=1))
