"""Tests du scoring de risque fournisseur (pondérations transparentes)."""
import pytest

from domain.service import supplier_scoring


class TestSupplierScoring:
    def test_neutral_supplier_is_low_risk(self):
        result = supplier_scoring.score({
            "nc_rate": 5.0,             # pivot
            "late_delivery_rate": 0.10,  # pivot
            "audit_score": 70.0,         # pivot
        })
        assert result.level == "low"
        assert result.score < 30

    def test_bad_supplier_is_high_or_critical(self):
        result = supplier_scoring.score({
            "nc_rate": 25.0,
            "nc_trend": 0.8,
            "late_delivery_rate": 0.45,
            "audit_score": 35.0,
            "days_since_last_audit": 900,
            "open_complaints": 9,
        })
        assert result.level in ("high", "critical")
        assert result.score > 60

    def test_good_supplier_scores_lower_than_bad(self):
        good = supplier_scoring.score({"nc_rate": 0.5, "audit_score": 95, "late_delivery_rate": 0.01})
        bad = supplier_scoring.score({"nc_rate": 20, "audit_score": 40, "late_delivery_rate": 0.4})
        assert good.score < bad.score

    def test_drivers_are_sorted_by_contribution_and_explain_the_score(self):
        result = supplier_scoring.score({"nc_rate": 30.0, "audit_score": 69.0})
        assert result.drivers[0].feature == "nc_rate"
        contributions = [abs(d.contribution) for d in result.drivers]
        assert contributions == sorted(contributions, reverse=True)
        # Chaque driver expose valeur brute + poids (auditabilité §12.3).
        for d in result.drivers:
            assert d.weight > 0
            assert isinstance(d.value, float)

    def test_unknown_feature_rejected(self):
        with pytest.raises(ValueError, match="unknown features"):
            supplier_scoring.score({"couleur_preferee": 3.0})

    def test_score_bounded_0_100(self):
        extreme = supplier_scoring.score({
            "nc_rate": 1e6, "nc_trend": 1.0, "late_delivery_rate": 1.0,
            "audit_score": 0.0, "days_since_last_audit": 1e5, "open_complaints": 1e3,
        })
        assert 0.0 <= extreme.score <= 100.0
