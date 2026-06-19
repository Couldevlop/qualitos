# =============================================================================
# edge-inference — Edge Gateway local inference component (CLAUDE.md §9.5)
#
# Multi-arch: builds on linux/amd64 (x86 IPC, Intel NUC) and linux/arm64
# (Raspberry Pi / Jetson). Build with buildx for both:
#   docker buildx build --platform linux/amd64,linux/arm64 \
#     -t qualitos/edge-inference:0.1.0 infra/edge/inference --push
#
# Runtime is distroless/nonroot (OWASP A05, §10.2): no shell, no package manager,
# minimal attack surface. The core has ZERO third-party deps; the optional ONNX
# extra (onnxruntime + numpy) is installed only when ARG WITH_ONNX=true.
# =============================================================================
FROM python:3.12-slim AS builder
WORKDIR /build
ARG WITH_ONNX=false
COPY pyproject.toml ./
COPY edge_inference ./edge_inference
# Install the package (and optionally the onnx extra) into a relocatable prefix.
RUN if [ "$WITH_ONNX" = "true" ]; then EXTRA="[onnx]"; else EXTRA=""; fi; \
    pip install --no-cache-dir --prefix=/install ".${EXTRA}"

FROM gcr.io/distroless/python3-debian12:nonroot
WORKDIR /app
COPY --from=builder /install /usr/local
COPY edge_inference /app/edge_inference
# Buffer persistence dir (mount a PVC here for store-and-forward across reboots).
ENV EDGE_BUFFER_PATH=/var/lib/edge-inference/queue.jsonl \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
USER nonroot:nonroot
# This image ships the LIBRARY + a self-check entrypoint. The real long-running
# runtime (MQTT subscriber feeding the orchestrator) is wired per deployment;
# the default command verifies the package imports and the selected backend.
ENTRYPOINT ["python", "-c", "import edge_inference; from edge_inference.infrastructure.backend_factory import build_backend; print('edge-inference', edge_inference.__version__, 'backend=', build_backend({}).name)"]
