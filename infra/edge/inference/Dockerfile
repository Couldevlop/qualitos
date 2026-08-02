# =============================================================================
# edge-inference — Edge Gateway local inference component (CLAUDE.md §9.5)
#
# Multi-arch: builds on linux/amd64 (x86 IPC, Intel NUC) and linux/arm64
# (Raspberry Pi / Jetson). Build with buildx for both:
#   docker buildx build --platform linux/amd64,linux/arm64 \
#     -t qualitos/edge-inference:0.1.0 infra/edge/inference --push
#
# Runtime minimal (OWASP A05, §10.2) : ni shell ni gestionnaire de paquets. La
# base est passée de gcr.io/distroless/python3-debian12 à Chainguard, pour la
# même raison que les deux autres images Python du dépôt : distroless n'est pas
# patchable de notre côté et ses reconstructions amont ont pris plusieurs crans
# de retard, ce qui transformait chaque CVE système en exception Trivy à
# reconduire. Chainguard reconstruit quotidiennement.
#
# Ce changement corrige aussi une incohérence : le builder était `python:3.12-slim`
# et le runtime distroless embarque Python 3.11, si bien que l'extra ONNX,
# installé avec `--prefix` dans une arborescence `python3.12`, était invisible
# pour l'interpréteur du runtime. Le cœur fonctionnait par accident, uniquement
# parce que le paquet est aussi copié dans le répertoire de travail. Les deux
# étages partagent maintenant la même base, donc la même mineure de Python.
#
# Le cœur n'a AUCUNE dépendance tierce ; l'extra ONNX (onnxruntime + numpy) n'est
# installé que si ARG WITH_ONNX=true.
# =============================================================================
FROM cgr.dev/chainguard/python:latest-dev AS builder
# Les images Chainguard démarrent en nonroot ; le builder doit écrire dans /deps.
USER root
WORKDIR /build
ARG WITH_ONNX=false
COPY pyproject.toml ./
COPY edge_inference ./edge_inference
# Installation dans un répertoire exposé par PYTHONPATH plutôt que dans un
# préfixe versionné : indépendant de la mineure de Python.
RUN if [ "$WITH_ONNX" = "true" ]; then EXTRA="[onnx]"; else EXTRA=""; fi; \
    python -m pip install --no-cache-dir --target=/deps ".${EXTRA}"

FROM cgr.dev/chainguard/python:latest
WORKDIR /app
COPY --from=builder /deps /deps
COPY edge_inference /app/edge_inference
# Buffer persistence dir (mount a PVC here for store-and-forward across reboots).
ENV EDGE_BUFFER_PATH=/var/lib/edge-inference/queue.jsonl \
    PYTHONPATH="/app:/deps" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
# Déjà l'utilisateur par défaut de l'image ; déclaré explicitement pour que le
# dernier USER du fichier ne soit pas le `root` du builder, et pour qu'un
# changement de défaut en amont ne puisse pas faire tourner ce conteneur en root.
USER nonroot:nonroot

# This image ships the LIBRARY + a self-check entrypoint. The real long-running
# runtime (MQTT subscriber feeding the orchestrator) is wired per deployment;
# the default command verifies the package imports and the selected backend.
# L'entrypoint de la base EST l'interpréteur : on ne passe que les arguments.
CMD ["-c", "import edge_inference; from edge_inference.infrastructure.backend_factory import build_backend; print('edge-inference', edge_inference.__version__, 'backend=', build_backend({}).name)"]
