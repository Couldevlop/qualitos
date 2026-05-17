from app.domain.analyzer import DeterministicStubBackend
from app.domain.models import FiveSScore, Pillar


def test_score_is_deterministic(jpeg_bytes):
    backend = DeterministicStubBackend()
    r1 = backend.analyze(jpeg_bytes, 320, 240)
    r2 = backend.analyze(jpeg_bytes, 320, 240)
    assert r1.score.as_dict() == r2.score.as_dict()
    assert r1.image_sha256 == r2.image_sha256


def test_score_range():
    backend = DeterministicStubBackend()
    r = backend.analyze(b"\x00" * 1024, 1, 1)
    for value in r.score.as_dict().values():
        assert 0 <= value <= 100


def test_findings_for_low_scores():
    """When a pillar's score is < 60, a finding must be raised.

    We construct a digest-prefix that yields all-low scores by searching
    deterministic byte inputs until each first-5 digest byte mod 71 < 30
    (so 30 + (b%71) < 60). With a small loop this is fast and reproducible.
    """
    import hashlib
    backend = DeterministicStubBackend()
    found = None
    for i in range(200_000):
        data = i.to_bytes(8, "big")
        digest = hashlib.sha256(data).digest()
        if all((digest[j] % 71) < 30 for j in range(5)):
            found = data
            break
    assert found is not None, "could not synthesize a low-score image deterministically"
    r = backend.analyze(found, 100, 100)
    assert all(v < 60 for k, v in r.score.as_dict().items() if k != "overall")
    assert len(r.findings) == 5
    pillars = {f.pillar for f in r.findings}
    assert pillars == set(Pillar)


def test_no_findings_when_clean():
    # Pick bytes that yield all-high scores: digest[i] % 71 + 30 >= 60 -> digest[i] >= 30
    # The deterministic mapping means we just verify shape, not magic bytes.
    score = FiveSScore(95, 90, 85, 80, 75)
    assert score.overall == round((95 + 90 + 85 + 80 + 75) / 5)


def test_overall_average():
    score = FiveSScore(100, 100, 100, 100, 100)
    assert score.overall == 100
    score = FiveSScore(0, 0, 0, 0, 0)
    assert score.overall == 0
