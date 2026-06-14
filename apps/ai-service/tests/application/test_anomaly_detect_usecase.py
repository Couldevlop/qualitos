"""Tests du use case AnomalyDetectUseCase (validation + branches de drapeau)."""
from __future__ import annotations

from uuid import uuid4

import pytest

from application.usecase import AnomalyDetectUseCase
from domain.model.tenant import TenantContext

_TENANT = TenantContext(tenant_id=uuid4(), issuer="https://kc.test/realms/qos")


def _uc() -> AnomalyDetectUseCase:
    return AnomalyDetectUseCase()


def _cloud_with_outlier() -> list[list[float]]:
    rows = [[float(i % 3), float((i * 2) % 3)] for i in range(30)]
    rows.append([50.0, -50.0])
    return rows


def test_isolation_forest_result_shape():
    res = _uc().execute(_cloud_with_outlier(), _TENANT, seed=1, sample_size=32)
    assert res.method == "isolation_forest"
    assert res.n == 31 and res.n_features == 2
    assert res.has_anomalies
    assert res.points[30].is_anomaly is True
    assert res.points[30].top_feature is None


def test_reconstruction_top_feature_attributed():
    rows = [[float(i), 2.0 * i] for i in range(20)]
    rows.append([5.0, -30.0])
    res = _uc().execute(rows, _TENANT, method="reconstruction", n_components=1)
    assert res.points[20].is_anomaly is True
    assert res.points[20].top_feature in (0, 1)


def test_explicit_threshold_used():
    res = _uc().execute(_cloud_with_outlier(), _TENANT, threshold=0.55, seed=1)
    assert res.threshold == 0.55
    assert all(p.is_anomaly == (p.score >= 0.55) for p in res.points)


def test_rejects_unknown_method():
    with pytest.raises(ValueError):
        _uc().execute([[1.0, 2.0]], _TENANT, method="nope")


def test_rejects_bad_contamination():
    with pytest.raises(ValueError):
        _uc().execute([[1.0, 2.0], [3.0, 4.0]], _TENANT, contamination=0.0)
    with pytest.raises(ValueError):
        _uc().execute([[1.0, 2.0], [3.0, 4.0]], _TENANT, contamination=0.6)


def test_rejects_empty_matrix():
    with pytest.raises(ValueError):
        _uc().execute([], _TENANT)


def test_rejects_empty_rows():
    with pytest.raises(ValueError):
        _uc().execute([[], []], _TENANT)


def test_rejects_ragged_rows():
    with pytest.raises(ValueError):
        _uc().execute([[1.0, 2.0], [3.0]], _TENANT)


def test_rejects_non_finite():
    with pytest.raises(ValueError):
        _uc().execute([[1.0, float("nan")], [3.0, 4.0]], _TENANT)


def test_rejects_too_many_features():
    row = [0.0] * 201
    with pytest.raises(ValueError):
        _uc().execute([row, row], _TENANT)


def test_rejects_too_many_samples(monkeypatch):
    import application.usecase.anomaly_detect as mod
    monkeypatch.setattr(mod, "_MAX_SAMPLES", 3)
    rows = [[1.0, 2.0]] * 4
    with pytest.raises(ValueError):
        _uc().execute(rows, _TENANT)


def test_rejects_non_numeric_cells():
    with pytest.raises(ValueError):
        _uc().execute([["a", "b"], ["c", "d"]], _TENANT)  # type: ignore[list-item]
