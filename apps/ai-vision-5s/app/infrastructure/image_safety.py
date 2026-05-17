"""OWASP-safe image handling for the Vision 5S service.

Defenses:
- 10 MB cap (CLAUDE.md §11 / OWASP LLM04 model DoS).
- libmagic content-type sniffing — refuse anything not JPEG / PNG / WebP.
- EXIF strip — never expose user GPS / device serial back to the API.
- NEVER call pickle.load on uploaded content.
"""

from __future__ import annotations

import io
import logging
from dataclasses import dataclass

from PIL import Image, ImageOps

# python-magic provides libmagic-based MIME sniffing. Optional at runtime so
# tests can stub it; if missing we fall back to PIL detection.
try:  # pragma: no cover - import guard
    import magic as _magic_lib
except Exception:  # pragma: no cover
    _magic_lib = None

_LOG = logging.getLogger(__name__)

MAX_BYTES = 10 * 1024 * 1024  # 10 MB
ALLOWED_MIMES = frozenset({"image/jpeg", "image/png", "image/webp"})


class ImageRejected(Exception):
    """Raised when an uploaded image is rejected for safety reasons."""


@dataclass(frozen=True)
class SafeImage:
    bytes: bytes
    mime: str
    width: int
    height: int


def sanitize(content: bytes) -> SafeImage:
    """Validate, strip EXIF, return a sanitized image suitable for the analyzer."""
    if not content:
        raise ImageRejected("Empty file")
    if len(content) > MAX_BYTES:
        raise ImageRejected(f"File exceeds {MAX_BYTES} bytes")

    mime = _detect_mime(content)
    if mime not in ALLOWED_MIMES:
        raise ImageRejected(f"Unsupported media type: {mime}")

    # Pillow will raise UnidentifiedImageError on garbage.
    try:
        img = Image.open(io.BytesIO(content))
        img.load()
    except Exception as exc:
        raise ImageRejected("Cannot decode image") from exc

    # Strip EXIF / metadata + apply orientation transpose.
    img = ImageOps.exif_transpose(img)
    width, height = img.size

    # Re-encode WITHOUT any metadata.
    out_buf = io.BytesIO()
    fmt = "JPEG" if mime == "image/jpeg" else "PNG" if mime == "image/png" else "WEBP"
    img_to_save = img.convert("RGB") if fmt in ("JPEG", "WEBP") else img
    img_to_save.save(out_buf, format=fmt, optimize=False)
    clean = out_buf.getvalue()

    return SafeImage(bytes=clean, mime=mime, width=width, height=height)


def _detect_mime(content: bytes) -> str:
    if _magic_lib is not None:
        try:
            m = _magic_lib.Magic(mime=True)
            return m.from_buffer(content[:8192])
        except Exception as exc:  # pragma: no cover
            _LOG.debug("libmagic detection failed, falling back to PIL: %s", exc)

    # Fallback: trust PIL's format detection (cannot pre-validate, but Pillow.open
    # will throw on non-images anyway).
    try:
        with Image.open(io.BytesIO(content)) as img:
            fmt = (img.format or "").upper()
    except Exception:
        return "application/octet-stream"
    return {
        "JPEG": "image/jpeg",
        "PNG": "image/png",
        "WEBP": "image/webp",
    }.get(fmt, "application/octet-stream")
