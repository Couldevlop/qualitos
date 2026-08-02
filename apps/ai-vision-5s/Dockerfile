# =============================================================================
# ai-vision-5s — image sur base Chainguard (OWASP A05 + CLAUDE.md §10.2)
#
# POURQUOI CE CHANGEMENT DE BASE. Comme l'ai-service, ce service reposait sur
# gcr.io/distroless/python3-debian12, base sans gestionnaire de paquets dont les
# reconstructions amont ont pris plusieurs crans de retard sur cpython, krb5 et
# libssl (constaté par scan direct le 2026-08-02). Chainguard reconstruit
# quotidiennement : les CVE système se résorbent d'elles-mêmes au lieu de
# s'accumuler en exceptions Trivy.
#
# CE CHANGEMENT CORRIGE AUSSI UN DÉFAUT LATENT. Le builder était
# `python:3.13-slim` alors que le runtime distroless embarque Python 3.11, et les
# paquets étaient installés avec `--prefix=/install` puis copiés dans /usr/local
# — donc dans `/usr/local/lib/python3.13/site-packages`, un répertoire que
# l'interpréteur 3.11 du runtime ne regarde même pas. L'image ne pouvait pas
# importer fastapi. Les deux étages partagent désormais la même base, donc la
# même mineure de Python et la même libc, et les dépendances sont exposées par
# PYTHONPATH plutôt que déposées dans une arborescence versionnée.
# =============================================================================

# -------- build stage (mêmes Python et libc que le runtime) --------
FROM cgr.dev/chainguard/python:latest-dev AS builder
# Les images Chainguard démarrent en nonroot ; le builder doit écrire dans /deps
# et poser des paquets système. Ne concerne QUE cet étage.
USER root
WORKDIR /build

# libmagic : le sniffing de type MIME de `image_safety.sanitize` s'appuie dessus.
# L'import est protégé et retombe sur la détection Pillow, mais libmagic est le
# contrôle le plus fort — on le conserve plutôt que de dégrader silencieusement.
RUN apk add --no-cache libmagic

COPY requirements.txt ./
# --only-binary=:all: : le runtime n'a ni compilateur ni en-têtes. Une dépendance
# sans roue doit faire échouer le build de façon visible, pas se compiler en
# douce. Vérifié pour l'ensemble des pins de requirements.txt.
RUN python -m pip install --no-cache-dir --only-binary=:all: --target=/deps -r requirements.txt

# -------- runtime stage (Chainguard — surface minimale, nonroot, sans shell) ----
FROM cgr.dev/chainguard/python:latest
WORKDIR /app

COPY --from=builder /deps /deps
COPY --from=builder /usr/lib/libmagic.so.1* /usr/lib/
COPY --from=builder /usr/share/misc/magic.mgc /usr/share/misc/
COPY app /app/app

ENV APP_PROFILE=prod \
    AUTH_BYPASS=false \
    PYTHONPATH="/app:/deps" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Déjà l'utilisateur par défaut de l'image ; déclaré explicitement pour que le
# dernier USER du fichier ne soit pas le `root` du builder, et pour qu'un
# changement de défaut en amont ne puisse pas faire tourner ce conteneur en root.
USER nonroot:nonroot

EXPOSE 8090

# L'entrypoint de l'image Chainguard python EST l'interpréteur ; on ne passe que
# les arguments.
CMD ["-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
