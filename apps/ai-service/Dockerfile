# syntax=docker/dockerfile:1.7

# -------- build stage (compile wheels with full toolchain) --------
FROM python:3.13-slim AS builder
WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential gcc \
 && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./
RUN python -m venv /opt/venv \
 && /opt/venv/bin/pip install --upgrade pip \
 && /opt/venv/bin/pip install --no-cache-dir .

# Copy app source — done AFTER deps so layer cache is friendly.
COPY . /build/app

# -------- runtime stage (distroless — minimal attack surface) -----
FROM gcr.io/distroless/python3-debian12:nonroot
WORKDIR /app

COPY --from=builder /opt/venv /opt/venv
COPY --from=builder /build/app /app

ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONPATH="/app" \
    QOS_ENV="prod" \
    QOS_LOG_LEVEL="INFO"

USER nonroot:nonroot
EXPOSE 8085

# Distroless has no shell — use the python binary directly.
ENTRYPOINT ["/opt/venv/bin/python", "-m", "uvicorn"]
CMD ["main:app", "--host", "0.0.0.0", "--port", "8085"]
