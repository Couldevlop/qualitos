"""Tests router de l'analyse NLP des réclamations."""
from __future__ import annotations

import json
import os

os.environ["QOS_DEV_AUTH"] = "true"

from fastapi.testclient import TestClient  # noqa: E402

from presentation.app import create_app  # noqa: E402

UUID_TENANT = "11111111-1111-1111-1111-111111111111"
UUID_USER = "22222222-2222-2222-2222-222222222222"


def _hdr() -> dict[str, str]:
    return {"X-Dev-Claims": json.dumps(
        {"sub": UUID_USER, "tid": UUID_TENANT, "roles": ["analyst"]})}


def test_requires_auth():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/complaints/analyze", json={"texts": ["test"]})
        assert r.status_code == 401


def test_analyze_returns_sentiment_category_criticality():
    with TestClient(create_app()) as client:
        body = {"texts": [
            "Produit dangereux, risque de blessure, rappel urgent",
            "Livraison rapide, service parfait, merci",
        ]}
        r = client.post("/v1/ai/complaints/analyze", json=body, headers=_hdr())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["n"] == 2 and data["critical_count"] == 1
        assert data["insights"][0]["critical"] is True
        assert data["insights"][0]["category"] == "securite"
        assert data["insights"][1]["sentiment_label"] == "positive"


def test_custom_categories():
    with TestClient(create_app()) as client:
        body = {"texts": ["la chambre était sale"],
                "categories": {"hygiene": ["sale", "hygiene"]}}
        r = client.post("/v1/ai/complaints/analyze", json=body, headers=_hdr())
        assert r.status_code == 200, r.text
        assert r.json()["insights"][0]["category"] == "hygiene"


def test_rejects_empty():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/complaints/analyze", json={"texts": []}, headers=_hdr())
        assert r.status_code == 422
