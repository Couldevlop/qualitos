"""Router tests for the two SPC endpoints: analyse, and breach forecast.

SPC is pure compute (no provider/Ollama), so no container patching is needed.
"""
from __future__ import annotations

import json
import os

import pytest

os.environ["QOS_DEV_AUTH"] = "true"

from fastapi.testclient import TestClient  # noqa: E402

from presentation.app import create_app  # noqa: E402

UUID_TENANT = "11111111-1111-1111-1111-111111111111"
UUID_USER = "22222222-2222-2222-2222-222222222222"


def _dev_header() -> dict[str, str]:
    return {
        "X-Dev-Claims": json.dumps(
            {"sub": UUID_USER, "tid": UUID_TENANT, "roles": ["analyst"]}
        ),
    }


def test_spc_requires_auth():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/analyze", json={"values": [1, 2, 3]})
        assert r.status_code == 401


def test_spc_detects_out_of_control():
    with TestClient(create_app()) as client:
        body = {"values": [10, 10, 10, 14, 10, 10], "center": 10.0, "sigma": 1.0}
        r = client.post("/v1/ai/spc/analyze", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["out_of_control"] is True
        assert any(v["rule"] == "NELSON_1" for v in data["violations"])
        assert data["limits"]["estimated"] is False


def test_spc_estimates_limits_when_not_provided():
    with TestClient(create_app()) as client:
        body = {"values": [10, 10.2, 9.8, 10.1, 9.9, 10.05]}
        r = client.post("/v1/ai/spc/analyze", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        assert r.json()["limits"]["estimated"] is True


def test_spc_rejects_partial_baseline():
    with TestClient(create_app()) as client:
        body = {"values": [1, 2, 3], "center": 2.0}  # sigma missing
        r = client.post("/v1/ai/spc/analyze", json=body, headers=_dev_header())
        assert r.status_code == 422


def test_spc_rejects_empty_values():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/analyze", json={"values": []}, headers=_dev_header())
        assert r.status_code == 422


# ---- POST /v1/ai/spc/forecast : quand le procede sortira-t-il ? --------------

def _derive(n: int = 30, pas: float = 0.5) -> list[float]:
    return [10.0 + pas * i + (0.1 if i % 2 else -0.1) for i in range(n)]


def test_spc_forecast_requires_auth():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/forecast", json={"values": _derive()})
        assert r.status_code == 401


def test_spc_forecast_dates_the_breach():
    with TestClient(create_app()) as client:
        body = {"values": _derive(), "horizon": 12}
        r = client.post("/v1/ai/spc/forecast", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["will_breach"] is True
        assert 1 <= data["first_breach_step"] <= 12
        assert data["side"] == "upper"
        assert len(data["points"]) == 12
        # L'explicabilite voyage avec le chiffre (§12.3).
        assert data["model"] in ("holt_linear", "holt_winters_additive")
        assert data["confidence"] in ("low", "medium", "high")


def test_spc_forecast_says_nothing_on_a_stable_process():
    with TestClient(create_app()) as client:
        stable = [10.0 + (0.2 if i % 2 else -0.2) for i in range(30)]
        r = client.post("/v1/ai/spc/forecast", json={"values": stable},
                        headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["will_breach"] is False
        assert data["first_breach_step"] is None
        assert data["side"] is None


def test_spc_forecast_uses_the_supplied_baseline():
    with TestClient(create_app()) as client:
        body = {"values": _derive(pas=0.1), "center": 10.0, "sigma": 1.0, "horizon": 6}
        r = client.post("/v1/ai/spc/forecast", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        limits = r.json()["limits"]
        assert limits["estimated"] is False
        assert limits["ucl"] == pytest.approx(13.0)


def test_spc_forecast_rejects_partial_baseline():
    with TestClient(create_app()) as client:
        body = {"values": _derive(), "center": 10.0}  # sigma manquant
        r = client.post("/v1/ai/spc/forecast", json=body, headers=_dev_header())
        assert r.status_code == 422


def test_spc_forecast_rejects_a_series_too_short_to_trend():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/forecast", json={"values": [1, 2, 3]},
                        headers=_dev_header())
        assert r.status_code == 422


def test_spc_forecast_rejects_an_out_of_bounds_horizon():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/forecast", json={"values": _derive(), "horizon": 61},
                        headers=_dev_header())
        assert r.status_code == 422


def test_spc_forecast_rejects_an_out_of_bounds_threshold():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/spc/forecast", json={"values": _derive(), "threshold": 0},
                        headers=_dev_header())
        assert r.status_code == 422
