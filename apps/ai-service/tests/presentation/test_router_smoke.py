"""FastAPI smoke tests — health + auth gate."""
from __future__ import annotations

import os

from fastapi.testclient import TestClient

# Enable the dev token validator for these tests only.
os.environ["QOS_DEV_AUTH"] = "true"

from presentation.app import create_app  # noqa: E402


def test_healthz_open():
    app = create_app()
    with TestClient(app) as client:
        r = client.get("/healthz")
        assert r.status_code == 200
        assert r.json()["status"] == "ok"


def test_complete_requires_auth():
    app = create_app()
    with TestClient(app) as client:
        r = client.post(
            "/v1/ai/complete",
            json={"user_prompt": "hello", "provider": "ollama"},
        )
        # Without X-Dev-Claims, the dev validator returns 401.
        assert r.status_code == 401
