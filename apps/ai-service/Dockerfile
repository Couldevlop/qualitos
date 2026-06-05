# syntax=docker/dockerfile:1.7
#
# Image distroless de l'ai-service alignée sur Python 3.11 (cf. ADR 0013).
# Le builder DOIT utiliser la même version mineure (3.11) et la même base
# (Debian bookworm) que le runtime distroless `python3-debian12`, afin que les
# wheels compilées (pydantic-core, psycopg, spacy…) soient ABI-compatibles.
# Pas de virtualenv : les dépendances sont installées dans /deps et exécutées
# par l'interpréteur natif du distroless via PYTHONPATH (évite le lien
# d'interpréteur cassé sur distroless).

# -------- build stage (3.11, bookworm — aligné sur le runtime) --------
FROM python:3.11-slim-bookworm AS builder
WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential gcc \
 && rm -rf /var/lib/apt/lists/*

# Dépendances runtime (cf. [project.dependencies] de pyproject.toml). Listées
# explicitement pour éviter l'ambiguïté du flat-layout setuptools — voir ADR 0013
# (dette : figer un requirements.txt via pip-compile/uv).
# NB : presidio-analyzer/anonymizer volontairement EXCLUS de l'image (cf. ADR 0013).
# Presidio charge un modèle spaCy (en_core_web_lg) qu'il tente d'auto-télécharger au
# 1er usage via la CLI spaCy → `SystemExit` non rattrapable dans le runtime distroless
# (pas de pip). Le filtre PII bascule alors proprement sur l'implémentation
# heuristique (`HeuristicPiiFilter`) — comportement prévu par le code. Réactiver
# Presidio en prod en EMBARQUANT le modèle spaCy au build (suivi ADR 0013).
RUN python -m pip install --no-cache-dir --upgrade pip \
 && python -m pip install --no-cache-dir --target=/deps \
    "fastapi>=0.115" \
    "uvicorn[standard]>=0.32" \
    "pydantic>=2.9" \
    "httpx>=0.27" \
    "sqlglot>=25.5" \
    "numpy>=1.26" \
    "pyjwt[crypto]>=2.9" \
    "slowapi>=0.1.9" \
    "qdrant-client>=1.11" \
    "psycopg[binary]>=3.2"

# -------- runtime stage (distroless — surface d'attaque minimale, nonroot) -----
FROM gcr.io/distroless/python3-debian12:nonroot
WORKDIR /app

# site-packages tiers + code applicatif.
COPY --from=builder /deps /deps
COPY . /app

ENV PYTHONPATH="/app:/deps" \
    PYTHONUNBUFFERED=1 \
    QOS_ENV="prod" \
    QOS_LOG_LEVEL="INFO"

USER nonroot:nonroot
EXPOSE 8085

# L'entrypoint par défaut de l'image distroless python3 EST l'interpréteur Python ;
# on ne passe que les arguments (pas de shell dans distroless).
CMD ["-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8085"]
