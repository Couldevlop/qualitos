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


class TestHoltWinters:
    def test_default_model_is_holt_linear(self):
        result = forecasting.forecast([10, 12, 14, 16, 18, 20], target=30)
        assert result.model == "holt_linear"
        assert result.seasonal_period == 0

    def test_linear_series_exactly_reproduced(self):
        # Holt reproduit exactement une droite : tendance et prévision restent exactes.
        values = list(range(10))  # 0..9, pente 1
        r = forecasting.forecast(values, target=20, horizon=5, direction="at_least")
        assert r.slope == pytest.approx(1.0, abs=1e-9)
        assert r.points[-1].value == pytest.approx(14.0, abs=1e-6)  # 9 + 5*1

    def test_seasonal_period_engages_holt_winters(self):
        # Série saisonnière additive (période 4) sur tendance douce, 4 cycles.
        base = [10.0, 14.0, 9.0, 13.0]
        values = [base[i % 4] + 0.5 * i for i in range(16)]
        r = forecasting.forecast(values, target=30, horizon=4, seasonal_period=4)
        assert r.model == "holt_winters_additive"
        assert r.seasonal_period == 4
        assert len(r.points) == 4
        # La saisonnalité réapparaît : les 4 pas projetés ne sont pas monotones.
        vals = [p.value for p in r.points]
        assert max(vals) - min(vals) > 1.0

    def test_seasonal_period_ignored_when_series_too_short(self):
        # Moins de 2 périodes → retombe sur Holt linéaire (pas de saisonnalité fiable).
        r = forecasting.forecast([1, 2, 3, 4, 5], target=10, seasonal_period=4)
        assert r.model == "holt_linear"
        assert r.seasonal_period == 0

    def test_seasonal_forecast_recovers_pattern(self):
        # Saisonnalité pure (pas de tendance) : le creux/pic du cycle est restitué.
        cycle = [0.0, 5.0, 0.0, -5.0]
        values = cycle * 5  # 20 points, période 4
        r = forecasting.forecast(values, target=100, horizon=4, seasonal_period=4)
        vals = [p.value for p in r.points]
        # Le motif projeté garde une amplitude proche du cycle d'origine.
        assert max(vals) - min(vals) > 5.0
