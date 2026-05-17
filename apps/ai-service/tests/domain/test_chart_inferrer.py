"""Pure ChartInferrer tests."""
from __future__ import annotations

from domain.service.chart_inferrer import ChartInferrer


def test_empty_rows_returns_kpi_zero():
    spec = ChartInferrer.infer([], "what is X?")
    assert spec.chart_type == "kpi"


def test_single_numeric_returns_kpi():
    spec = ChartInferrer.infer([{"total": 42}], "total?")
    assert spec.chart_type == "kpi"
    assert spec.series[0]["value"] == 42


def test_two_cols_string_numeric_returns_bar():
    rows = [{"site": "A", "count": 10}, {"site": "B", "count": 5}]
    spec = ChartInferrer.infer(rows, "by site")
    assert spec.chart_type == "bar"


def test_date_first_col_returns_line():
    rows = [
        {"day": "2026-05-01", "count": 1},
        {"day": "2026-05-02", "count": 3},
    ]
    spec = ChartInferrer.infer(rows, "trend")
    assert spec.chart_type == "line"


def test_many_cols_returns_table():
    rows = [{"a": 1, "b": "x", "c": "y", "d": 2}]
    spec = ChartInferrer.infer(rows, "raw")
    assert spec.chart_type == "table"
