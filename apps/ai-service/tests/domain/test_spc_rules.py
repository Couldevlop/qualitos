"""Tests for the SPC domain service (control limits + the 8 Nelson rules)."""
from __future__ import annotations

from domain.service.spc_rules import analyze, estimate_limits


def _rules_fired(analysis) -> set[str]:
    return {v.rule for v in analysis.violations}


def test_estimate_limits_centre_and_sigma():
    values = [9, 11, 9, 11, 9, 11, 9, 11]
    limits = estimate_limits(values)
    assert abs(limits.center_line - 10.0) < 1e-9
    assert limits.sigma > 0
    assert limits.ucl > limits.center_line > limits.lcl
    assert limits.estimated is True


def test_in_control_series_has_no_violations():
    values = [10, 10.2, 9.8, 10.1, 9.9, 10.05, 9.95, 10.0]
    a = analyze(values, center=10.0, sigma=1.0)
    assert a.out_of_control is False
    assert a.violations == []


def test_rule1_point_beyond_3sigma():
    values = [10, 10, 10, 14, 10, 10]  # one point at +4σ (sigma=1)
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_1" in _rules_fired(a)
    v = next(v for v in a.violations if v.rule == "NELSON_1")
    assert v.point_indices == [3]
    assert v.severity == "high"


def test_rule2_nine_points_same_side():
    values = [10.5] * 9  # 9 points above the centre line
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_2" in _rules_fired(a)
    v = next(v for v in a.violations if v.rule == "NELSON_2")
    assert len(v.point_indices) == 9


def test_rule2_not_fired_with_eight():
    values = [10.5] * 8
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_2" not in _rules_fired(a)


def test_rule3_six_point_increasing_trend():
    values = [1, 2, 3, 4, 5, 6, 7]  # strictly increasing → trend
    a = analyze(values, center=4.0, sigma=1.0)
    assert "NELSON_3" in _rules_fired(a)
    v = next(v for v in a.violations if v.rule == "NELSON_3")
    assert v.point_indices[0] == 0
    assert len(v.point_indices) >= 6


def test_rule3_not_fired_with_five_steps():
    values = [1, 2, 3, 4, 5]  # run length 5 < 6
    a = analyze(values, center=3.0, sigma=1.0)
    assert "NELSON_3" not in _rules_fired(a)


def test_rule4_alternating_fourteen():
    values = [10.4 if i % 2 == 0 else 9.6 for i in range(14)]
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_4" in _rules_fired(a)


def test_rule5_two_of_three_beyond_2sigma():
    values = [10, 12.5, 10, 12.6]  # indices 1 and 3 are >2σ above; window {1,2,3}
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_5" in _rules_fired(a)


def test_rule7_fifteen_within_1sigma():
    values = [10.1, 9.9] * 8  # 16 points all within 1σ
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_7" in _rules_fired(a)


def test_rule8_eight_beyond_1sigma_both_sides():
    values = [11.5, 8.5, 11.5, 8.5, 11.5, 8.5, 11.5, 8.5]
    a = analyze(values, center=10.0, sigma=1.0)
    assert "NELSON_8" in _rules_fired(a)


def test_flat_series_is_safe():
    a = analyze([5, 5, 5, 5])  # no spread → sigma 0 → no findings, no crash
    assert a.out_of_control is False
    assert a.violations == []


def test_empty_values_returns_empty_analysis():
    a = analyze([])
    assert a.n == 0
    assert a.violations == []
