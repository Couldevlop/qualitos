"""Router tests for the SPC anomaly-detection endpoint.

SPC is pure compute (no provider/Ollama), so no container patching is needed.
"""
from __future__ import annotations

import json
import os

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
