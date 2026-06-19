"""Router tests for the predictive endpoints (§6.5).

Pure compute (no provider/Ollama) — no container patching needed.
"""
from __future__ import annotations

import importlib.util
import json
import os

os.environ["QOS_DEV_AUTH"] = "true"

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from presentation.app import create_app  # noqa: E402


def _absent(module: str) -> bool:
    return importlib.util.find_spec(module) is None

UUID_TENANT = "11111111-1111-1111-1111-111111111111"
UUID_USER = "22222222-2222-2222-2222-222222222222"


def _dev_header() -> dict[str, str]:
    return {
        "X-Dev-Claims": json.dumps(
            {"sub": UUID_USER, "tid": UUID_TENANT, "roles": ["analyst"]}
        ),
    }


# ---- auth ------------------------------------------------------------------------

def test_predict_endpoints_require_auth():
    with TestClient(create_app()) as client:
        assert client.post("/v1/ai/predict/kpi",
                           json={"values": [1, 2, 3, 4], "target": 5}).status_code == 401
        assert client.post("/v1/ai/predict/supplier-risk",
                           json={"features": {"nc_rate": 1}}).status_code == 401
        assert client.post("/v1/ai/predict/nc-clusters",
                           json={"texts": ["a b", "a b"]}).status_code == 401


# ---- KPI forecast ----------------------------------------------------------------

def test_kpi_forecast_nominal():
    with TestClient(create_app()) as client:
        body = {"values": [10, 12, 14, 16, 18, 20, 22, 24], "target": 30, "horizon": 4}
        r = client.post("/v1/ai/predict/kpi", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["n"] == 8
        assert data["probability"] > 0.9
        assert len(data["points"]) == 4
        assert data["confidence"] in ("low", "medium", "high")


def test_kpi_forecast_rejects_short_series():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/predict/kpi",
                        json={"values": [1, 2, 3], "target": 5}, headers=_dev_header())
        assert r.status_code == 422


# ---- supplier risk ---------------------------------------------------------------

def test_supplier_risk_nominal_with_drivers():
    with TestClient(create_app()) as client:
        body = {"features": {"nc_rate": 22.0, "audit_score": 45.0, "late_delivery_rate": 0.3}}
        r = client.post("/v1/ai/predict/supplier-risk", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert 0 <= data["score"] <= 100
        assert data["level"] in ("low", "medium", "high", "critical")
        assert len(data["drivers"]) == 3
        assert all("contribution" in d for d in data["drivers"])


def test_supplier_risk_unknown_feature_is_422():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/predict/supplier-risk",
                        json={"features": {"hacker_field": 1.0}}, headers=_dev_header())
        assert r.status_code == 422


# ---- NC clustering ---------------------------------------------------------------

def test_nc_clusters_nominal():
    with TestClient(create_app()) as client:
        body = {"texts": [
            "Fuite d'huile presse hydraulique",
            "Fuite huile presse hydraulique ligne 2",
            "Erreur logicielle écran opérateur",
        ]}
        r = client.post("/v1/ai/predict/nc-clusters", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["n"] == 3
        assert len(data["clusters"]) == 1
        assert data["clusters"][0]["size"] == 2
        assert data["clusters"][0]["top_terms"]


def test_nc_clusters_rejects_single_text():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/predict/nc-clusters",
                        json={"texts": ["seul"]}, headers=_dev_header())
        assert r.status_code == 422


# ---- backends ML opt-in (ADR 0031) ----------------------------------------------

def test_kpi_forecast_default_model_is_holt():
    with TestClient(create_app()) as client:
        body = {"values": [10, 12, 14, 16, 18, 20], "target": 30}
        r = client.post("/v1/ai/predict/kpi", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        assert r.json()["model"].startswith("holt")


def test_kpi_forecast_rejects_unknown_model():
    with TestClient(create_app()) as client:
        body = {"values": [10, 12, 14, 16], "target": 30, "model": "xgboost"}
        r = client.post("/v1/ai/predict/kpi", json=body, headers=_dev_header())
        assert r.status_code == 422  # rejeté par le pattern du schéma


@pytest.mark.skipif(not _absent("prophet"), reason="prophet installé")
def test_kpi_forecast_prophet_backend_unavailable_is_422_with_clear_message():
    # prophet n'est pas installé en CI → 422 « extra ml », jamais un faux résultat.
    with TestClient(create_app()) as client:
        body = {"values": [10, 12, 14, 16, 18, 20], "target": 30, "model": "prophet"}
        r = client.post("/v1/ai/predict/kpi", json=body, headers=_dev_header())
        assert r.status_code == 422, r.text
        assert "extra ml" in r.json()["detail"]


@pytest.mark.skipif(not _absent("torch"), reason="torch installé")
def test_kpi_forecast_lstm_backend_unavailable_is_422():
    with TestClient(create_app()) as client:
        body = {"values": [10, 12, 14, 16, 18, 20], "target": 30, "model": "lstm"}
        r = client.post("/v1/ai/predict/kpi", json=body, headers=_dev_header())
        assert r.status_code == 422, r.text
        assert "extra ml" in r.json()["detail"]


@pytest.mark.skipif(not _absent("hdbscan"), reason="hdbscan installé")
def test_nc_clusters_hdbscan_backend_unavailable_is_422():
    with TestClient(create_app()) as client:
        body = {"texts": ["fuite huile", "fuite huile presse", "erreur ecran"],
                "method": "hdbscan"}
        r = client.post("/v1/ai/predict/nc-clusters", json=body, headers=_dev_header())
        assert r.status_code == 422, r.text
        assert "extra ml" in r.json()["detail"]
