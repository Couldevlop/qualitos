# =============================================================================
# ai-vision-5s — distroless image (OWASP A05 + CLAUDE.md §10.2)
# Builder stage installs deps; runtime is the minimal distroless Python image.
# =============================================================================
FROM python:3.13-slim AS builder
WORKDIR /build
# libmagic + build deps for Pillow
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libmagic1 libjpeg-dev zlib1g-dev && rm -rf /var/lib/apt/lists/*
COPY requirements.txt ./
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM gcr.io/distroless/python3-debian12:nonroot
WORKDIR /app
COPY --from=builder /install /usr/local
COPY --from=builder /usr/lib/x86_64-linux-gnu/libmagic* /usr/lib/x86_64-linux-gnu/
COPY --from=builder /usr/share/misc/magic* /usr/share/misc/
COPY app /app/app
ENV APP_PROFILE=prod \
    AUTH_BYPASS=false \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
USER nonroot:nonroot
EXPOSE 8090
ENTRYPOINT ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
