"""Shared pytest fixtures."""

from __future__ import annotations

import io
import os

import pytest
from PIL import Image

# Enable auth bypass for all tests — we don't bring up Keycloak.
os.environ["AUTH_BYPASS"] = "true"


def _make_image(color: tuple[int, int, int], fmt: str = "JPEG") -> bytes:
    img = Image.new("RGB", (320, 240), color)
    buf = io.BytesIO()
    img.save(buf, format=fmt)
    return buf.getvalue()


@pytest.fixture
def jpeg_bytes() -> bytes:
    return _make_image((180, 220, 40), "JPEG")


@pytest.fixture
def png_bytes() -> bytes:
    return _make_image((10, 200, 200), "PNG")


@pytest.fixture
def webp_bytes() -> bytes:
    return _make_image((40, 10, 220), "WEBP")
