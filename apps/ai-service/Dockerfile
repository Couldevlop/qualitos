# syntax=docker/dockerfile:1.7
#
# Image de l'ai-service sur base Chainguard (cf. ADR 0013, révisé — CLAUDE.md §10.2
# autorise explicitement « distroless (gcr.io/distroless) OU Chainguard »).
#
# POURQUOI CE CHANGEMENT DE BASE. L'image précédente reposait sur
# gcr.io/distroless/python3-debian12. Cette base n'a pas de gestionnaire de
# paquets : quand une CVE tombe sur cpython, krb5 ou libssl, nous ne pouvons rien
# patcher — il faut attendre que Google reconstruise. Au scan du 2026-08-02 elle
# accusait DEUX crans de retard sur cpython (deb12u6 installé, deb12u8 publié) et
# traînait un correctif libssl depuis plus de cinq semaines, alors que la base
# java21 équivalente avait bien été reconstruite. Chaque CVE devenait une
# exception à motiver dans .trivyignore, dont une (CVE-2026-6100, use-after-free
# de décompression) était réellement atteignable via les réponses gzip des
# fournisseurs LLM. Les images Chainguard sont reconstruites quotidiennement :
# le problème est traité à la racine plutôt que reconduit d'exception en exception.
#
# COMPROMIS ASSUMÉ. L'offre gratuite Chainguard ne publie que les étiquettes
# `latest` / `latest-dev` ; les étiquettes figées par version mineure sont
# payantes. La version de Python suivra donc l'amont (3.14 au moment de la
# migration). C'est délibéré : la CI exécute désormais lint et tests DANS
# `latest-dev`, si bien que l'interpréteur testé est exactement celui livré. Une
# montée de version amont fera échouer la CI bruyamment au lieu de livrer en
# silence sur une version jamais testée — c'est précisément le défaut que la
# migration corrige.
#
# Les deux étages partagent la même base, donc la même mineure de Python et la
# même libc (Wolfi, glibc) : les roues manylinux installées dans /deps restent
# ABI-compatibles avec le runtime. Pas de virtualenv : les dépendances sont
# exécutées par l'interpréteur natif via PYTHONPATH.

# -------- build stage (mêmes Python et libc que le runtime) --------
FROM cgr.dev/chainguard/python:latest-dev AS builder
# Les images Chainguard démarrent en nonroot ; l'étage de construction a besoin
# d'écrire dans /deps. Cela ne concerne QUE le builder — le runtime plus bas
# conserve l'utilisateur nonroot par défaut.
USER root
WORKDIR /build

# Dépendances runtime (cf. [project.dependencies] de pyproject.toml). Listées
# explicitement pour éviter l'ambiguïté du flat-layout setuptools — voir ADR 0013
# (dette : figer un requirements.txt via pip-compile/uv).
# NB : presidio-analyzer/anonymizer volontairement EXCLUS de l'image (cf. ADR 0013).
# Presidio charge un modèle spaCy (en_core_web_lg) qu'il tente d'auto-télécharger au
# 1er usage via la CLI spaCy → `SystemExit` non rattrapable dans un runtime sans pip.
# Le filtre PII bascule alors proprement sur l'implémentation heuristique
# (`HeuristicPiiFilter`) — comportement prévu par le code. Réactiver Presidio en
# prod en EMBARQUANT le modèle spaCy au build (suivi ADR 0013).
#
# --only-binary=:all: : le runtime n'a ni compilateur ni en-têtes. Sans ce garde-fou,
# une dépendance sans roue se compilerait silencieusement dans le builder et
# produirait un binaire qu'on ne saurait pas reconstruire ; on préfère un échec de
# build explicite. Vérifié : les onze dépendances ont une roue manylinux.
RUN python -m pip install --no-cache-dir --upgrade pip \
 && python -m pip install --no-cache-dir --only-binary=:all: --target=/deps \
    "fastapi>=0.115" \
    "uvicorn[standard]>=0.32" \
    "python-multipart>=0.0.12" \
    "pydantic>=2.9" \
    "httpx>=0.27" \
    "sqlglot>=25.5" \
    "numpy>=1.26" \
    "pyjwt[crypto]>=2.9" \
    "slowapi>=0.1.9" \
    "qdrant-client>=1.11" \
    "psycopg[binary]>=3.2"

# -------- runtime stage (Chainguard — surface minimale, nonroot, sans shell) ----
FROM cgr.dev/chainguard/python:latest
WORKDIR /app

# site-packages tiers + code applicatif.
COPY --from=builder /deps /deps
COPY . /app

ENV PYTHONPATH="/app:/deps" \
    PYTHONUNBUFFERED=1 \
    QOS_ENV="prod" \
    QOS_LOG_LEVEL="INFO"

EXPOSE 8085

# L'entrypoint de l'image Chainguard python EST l'interpréteur ; on ne passe que
# les arguments (pas de shell dans l'image). L'utilisateur par défaut est déjà
# nonroot (65532) — aucune directive USER n'est nécessaire.
CMD ["-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8085"]
