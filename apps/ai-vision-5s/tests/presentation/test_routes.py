from fastapi.testclient import TestClient

from app.main import create_app

client = TestClient(create_app())


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "UP"


def test_security_headers_present():
    r = client.get("/health")
    assert r.headers["x-content-type-options"] == "nosniff"
    assert r.headers["x-frame-options"] == "DENY"
    assert "default-src 'none'" in r.headers["content-security-policy"]


def test_analyze_happy(jpeg_bytes):
    files = {"image": ("plant.jpg", jpeg_bytes, "image/jpeg")}
    r = client.post("/v1/vision/5s/analyze", files=files)
    assert r.status_code == 200, r.text
    body = r.json()
    assert len(body["image_sha256"]) == 64
    assert "overall" in body["score"]


def test_analyze_rejects_garbage():
    files = {"image": ("evil.bin", b"not an image really", "image/jpeg")}
    r = client.post("/v1/vision/5s/analyze", files=files)
    assert r.status_code == 400


def test_score_endpoint(png_bytes):
    files = {"image": ("p.png", png_bytes, "image/png")}
    r = client.post("/v1/vision/5s/score", files=files)
    assert r.status_code == 200
    body = r.json()
    for pillar in ("seiri", "seiton", "seiso", "seiketsu", "shitsuke", "overall"):
        assert 0 <= body[pillar] <= 100
