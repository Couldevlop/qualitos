"""Heuristics that pick an ECharts spec from a result set.

Pure function â€” no framework, no I/O. Tested in isolation.
"""
from __future__ import annotations

from typing import Any

from domain.model.nlq import ChartSpec


class ChartInferrer:
    """Picks a chart type from the structure of the result set."""

    @staticmethod
    def infer(rows: list[dict[str, Any]], question: str) -> ChartSpec:
        if not rows:
            return ChartSpec(
                chart_type="kpi",
                title=question[:80],
                series=({"value": 0, "label": "no data"},),
            )
        first = rows[0]
        cols = list(first.keys())

        # KPI: single row, single numeric column.
        if len(rows) == 1 and len(cols) == 1 and _is_numeric(first[cols[0]]):
            return ChartSpec(
                chart_type="kpi",
                title=question[:80],
                series=({"value": first[cols[0]], "label": cols[0]},),
            )

        # Time series: a date/time-like first col + numeric second col -> line.
        if len(cols) == 2 and _looks_like_date(first[cols[0]]) and _is_numeric(first[cols[1]]):
            return ChartSpec(
                chart_type="line",
                title=question[:80],
                x_axis=tuple(str(r[cols[0]]) for r in rows),
                series=(
                    {
                        "name": cols[1],
                        "type": "line",
                        "data": [r[cols[1]] for r in rows],
                    },
                ),
            )

        # 2 cols, string + numeric -> bar
        if len(cols) == 2 and _is_numeric(first[cols[1]]):
            return ChartSpec(
                chart_type="bar",
                title=question[:80],
                x_axis=tuple(str(r[cols[0]]) for r in rows),
                series=(
                    {
                        "name": cols[1],
                        "type": "bar",
                        "data": [r[cols[1]] for r in rows],
                    },
                ),
            )

        # Fallback: table
        return ChartSpec(
            chart_type="table",
            title=question[:80],
            series=tuple({"row": r} for r in rows[:100]),
        )


def _is_numeric(v: Any) -> bool:
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _looks_like_date(v: Any) -> bool:
    s = str(v)
    return any(sep in s for sep in ("-", "/", "T")) and any(c.isdigit() for c in s)
