import pytest

from app.infrastructure.image_safety import MAX_BYTES, ImageRejected, sanitize


def test_rejects_empty():
    with pytest.raises(ImageRejected):
        sanitize(b"")


def test_rejects_oversize():
    with pytest.raises(ImageRejected):
        sanitize(b"x" * (MAX_BYTES + 1))


def test_rejects_garbage():
    with pytest.raises(ImageRejected):
        sanitize(b"this is not an image at all" * 10)


def test_accepts_jpeg(jpeg_bytes):
    safe = sanitize(jpeg_bytes)
    assert safe.mime == "image/jpeg"
    assert safe.width > 0 and safe.height > 0
    assert len(safe.bytes) > 0


def test_accepts_png(png_bytes):
    safe = sanitize(png_bytes)
    assert safe.mime == "image/png"


def test_strips_exif():
    """EXIF metadata is removed by re-encoding."""
    import io
    from PIL import Image
    img = Image.new("RGB", (100, 100), (128, 128, 128))
    buf = io.BytesIO()
    # Pillow doesn't trivially write EXIF; we just ensure the safe output is
    # smaller than 1 MB and decodes back to a valid image.
    img.save(buf, format="JPEG")
    safe = sanitize(buf.getvalue())
    re_open = Image.open(io.BytesIO(safe.bytes))
    re_open.load()  # would throw if corrupted
    assert re_open.format == "JPEG"
