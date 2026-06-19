"""Tests router de transcription audio (Whisper opt-in — ADR 0031)."""
from __future__ import annotations

import importlib.util
import json
import os

os.environ["QOS_DEV_AUTH"] = "true"

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from presentation.app import create_app  # noqa: E402

UUID_TENANT = "11111111-1111-1111-1111-111111111111"
UUID_USER = "22222222-2222-2222-2222-222222222222"


def _hdr() -> dict[str, str]:
    return {"X-Dev-Claims": json.dumps(
        {"sub": UUID_USER, "tid": UUID_TENANT, "roles": ["analyst"]})}


def test_requires_auth():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/transcribe", files={"file": ("a.wav", b"data", "audio/wav")})
        assert r.status_code == 401


def test_rejects_empty_audio():
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/transcribe",
                        files={"file": ("a.wav", b"", "audio/wav")}, headers=_hdr())
        assert r.status_code == 422


def test_rejects_oversized_audio():
    with TestClient(create_app()) as client:
        big = b"\x00" * (25 * 1024 * 1024 + 1)
        r = client.post("/v1/ai/transcribe",
                        files={"file": ("a.wav", big, "audio/wav")}, headers=_hdr())
        assert r.status_code == 413
        assert "too large" in r.json()["detail"]


@pytest.mark.skipif(importlib.util.find_spec("whisper") is not None, reason="whisper installé")
def test_whisper_unavailable_is_501_with_clear_message():
    # whisper n'est pas installé en CI → 501 « extra ml », jamais une fausse transcription.
    with TestClient(create_app()) as client:
        r = client.post("/v1/ai/transcribe",
                        files={"file": ("a.wav", b"\x00\x01\x02fake", "audio/wav")},
                        headers=_hdr())
        assert r.status_code == 501, r.text
        assert "extra ml" in r.json()["detail"]
