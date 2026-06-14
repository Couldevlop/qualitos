"""Tests router de la détection d'anomalies multivariées.

Calcul pur (aucun provider/Ollama) : aucun patch de container nécessaire. Couvre
aussi les branches du use case via l'API (méthodes, seuil, validation, dégénérés).
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


def _cloud_with_outlier() -> list[list[float]]:
    """Petit nuage régulier + 1 aberrant net en dernière position."""
    rows = [[float(i % 3), float((i * 2) % 3)] for i in range(30)]
    rows.append([50.0, -50.0])  # aberrant index 30
    return rows


def test_requires_auth():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/anomaly/detect", json={"samples": [[1.0, 2.0]]})
        assert r.status_code == 401


def test_isolation_forest_flags_injected_outlier():
    with TestClient(create_app()) as client:
        body = {"samples": _cloud_with_outlier(), "method": "isolation_forest",
                "contamination": 0.1, "seed": 1}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["method"] == "isolation_forest"
        assert data["n"] == 31 and data["n_features"] == 2
        assert data["has_anomalies"] is True
        # Le dernier point (aberrant injecté) est marqué anormal.
        last = next(p for p in data["points"] if p["index"] == 30)
        assert last["is_anomaly"] is True
        # Isolation Forest n'attribue pas de feature dominante.
        assert last["top_feature"] is None


def test_reconstruction_flags_outlier_with_top_feature():
    with TestClient(create_app()) as client:
        # Données quasi 1D (x2 = 2·x1) + aberrant hors de la droite.
        rows = [[float(i), 2.0 * i] for i in range(20)]
        rows.append([5.0, -30.0])
        body = {"samples": rows, "method": "reconstruction",
                "contamination": 0.1, "n_components": 1}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["method"] == "reconstruction"
        last = next(p for p in data["points"] if p["index"] == 20)
        assert last["is_anomaly"] is True
        assert last["top_feature"] in (0, 1)


def test_explicit_threshold():
    with TestClient(create_app()) as client:
        body = {"samples": _cloud_with_outlier(), "method": "isolation_forest",
                "threshold": 0.6, "seed": 1}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 200, r.text
        assert r.json()["threshold"] == 0.6


def test_rejects_unknown_method():
    with TestClient(create_app()) as client:
        body = {"samples": [[1.0, 2.0], [3.0, 4.0]], "method": "deep_magic"}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 422


def test_rejects_ragged_matrix():
    with TestClient(create_app()) as client:
        body = {"samples": [[1.0, 2.0], [3.0]]}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 422


def test_rejects_empty_samples():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/anomaly/detect", json={"samples": []},
                        headers=_dev_header())
        assert r.status_code == 422


def test_rejects_bad_contamination():
    with TestClient(create_app()) as client:
        body = {"samples": [[1.0, 2.0], [3.0, 4.0]], "contamination": 0.9}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 422


def test_rejects_empty_rows():
    with TestClient(create_app()) as client:
        body = {"samples": [[], []]}
        r = client.post("/v1/ai/anomaly/detect", json=body, headers=_dev_header())
        assert r.status_code == 422
