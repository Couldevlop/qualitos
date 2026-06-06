"""Tests du service de prévision KPI (OLS + intervalles de prédiction)."""
import pytest

from domain.service import forecasting


class TestForecastNominal:
    def test_rising_series_reaches_at_least_target(self):
        values = [10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
        result = forecasting.forecast(values, target=35, horizon=6, direction="at_least")
        assert result.n == 10
        assert result.slope == pytest.approx(2.0, abs=1e-9)
        assert result.r2 == pytest.approx(1.0, abs=1e-9)
        # Série parfaitement linéaire montante : la cible 35 < forecast(36+12=40)
        assert result.probability > 0.95
        assert len(result.points) == 6
        assert result.points[-1].value == pytest.approx(40.0, abs=1e-6)

    def test_at_most_direction_flips_probability(self):
        values = [10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
        up = forecasting.forecast(values, target=35, horizon=6, direction="at_least")
        down = forecasting.forecast(values, target=35, horizon=6, direction="at_most")
        assert up.probability + down.probability == pytest.approx(1.0, abs=1e-9)

    def test_flat_noisy_series_far_target_is_unlikely(self):
        values = [50, 51, 49, 50, 52, 48, 50, 51, 49, 50]
        result = forecasting.forecast(values, target=80, horizon=6, direction="at_least")
        assert result.probability < 0.05

    def test_prediction_interval_widens_with_horizon(self):
        values = [10, 11, 13, 12, 14, 15, 14, 16]
        result = forecasting.forecast(values, target=20, horizon=8)
        widths = [p.high - p.low for p in result.points]
        assert widths == sorted(widths)

    def test_confidence_levels(self):
        short = forecasting.forecast([1, 2, 3, 4], target=10)
        assert short.confidence == "low"
        long_clean = forecasting.forecast(list(range(20)), target=30)
        assert long_clean.confidence == "high"


class TestForecastValidation:
    def test_too_few_points_rejected(self):
        with pytest.raises(ValueError):
            forecasting.forecast([1, 2, 3], target=10)

    def test_bad_direction_rejected(self):
        with pytest.raises(ValueError):
            forecasting.forecast([1, 2, 3, 4], target=10, direction="sideways")

    def test_bad_horizon_rejected(self):
        with pytest.raises(ValueError):
            forecasting.forecast([1, 2, 3, 4], target=10, horizon=0)
        with pytest.raises(ValueError):
            forecasting.forecast([1, 2, 3, 4], target=10, horizon=61)

    def test_probability_bounded(self):
        result = forecasting.forecast([1, 2, 3, 4, 5, 6], target=-1000)
        assert 0.0 <= result.probability <= 1.0
