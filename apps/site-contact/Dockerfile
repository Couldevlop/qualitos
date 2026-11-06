# syntax=docker/dockerfile:1.7
#
# Formulaire de contact du site : un service, une route.
#
# Il est public et sans authentification. Tout ce qu'il ne fait pas — base de
# données, session, fichier, page de documentation — est une surface qu'il
# n'offre pas. C'est pourquoi il vit à part de l'API du produit plutôt que d'y
# ajouter une adresse ouverte.
#
# BASE CHAINGUARD, MÊME MOTIF QUE apps/ai-service (CLAUDE.md §10.2 autorise
# « distroless OU Chainguard »), et pour une raison mesurée plutôt que par
# cohérence de façade. La première version de ce fichier partait de
# `python:3.13-alpine` et installait les dépendances avec le pip de l'image. Le
# scan Trivy de la CD a alors bloqué sur HUIT CVE HIGH dont cinq ne venaient pas
# du tout de notre code : setuptools 70.3.0, msgpack 1.1.2 et urllib3 2.7.0 sont
# les paquets que pip EMBARQUE (`pip/_vendor`) et qui restent dans l'image une
# fois l'installation finie. On ne peut pas les corriger sans corriger pip, et
# on n'a aucun besoin de pip à l'exécution.
#
# La construction en deux étages règle la cause : les dépendances sont
# installées dans /deps par l'étage de construction, et l'étage d'exécution ne
# reçoit que /deps — ni pip, ni setuptools, ni leurs paquets embarqués. Les deux
# étages partagent la même base, donc la même mineure de Python et la même libc
# (Wolfi, glibc) : les roues restent compatibles. Pas de virtualenv, les
# dépendances sont vues par PYTHONPATH.
#
# Empreintes IDENTIQUES à celles de apps/ai-service : même interpréteur, à
# relever ensemble. L'offre gratuite Chainguard ne publie que `latest` /
# `latest-dev` ; l'épinglage par empreinte est donc le seul moyen de figer une
# version, et le job « Formulaire de contact (pytest) » de la CI exécute les
# tests dans la même image — l'interpréteur testé est celui qui est livré.

# -------- étage de construction --------
FROM cgr.dev/chainguard/python:latest-dev@sha256:29ab100be9f9be1ec5e31d7b7c384134171ee8f052e24e2285e697538c199aa6 AS builder
# Les images Chainguard démarrent en nonroot ; cet étage doit écrire dans /deps.
# Cela ne concerne QUE le builder — l'étage d'exécution ci-dessous garde son
# utilisateur nonroot.
USER root
WORKDIR /build

COPY requirements.txt ./
# --only-binary=:all: : l'étage d'exécution n'a ni compilateur ni en-têtes. Sans
# ce garde-fou, une dépendance sans roue se compilerait ici et produirait un
# binaire qu'on ne saurait pas reconstruire ; on préfère un échec de build net.
RUN python -m pip install --no-cache-dir --upgrade pip \
 && python -m pip install --no-cache-dir --only-binary=:all: --target=/deps \
      -r requirements.txt

# -------- étage d'exécution (sans pip, sans shell, nonroot) --------
FROM cgr.dev/chainguard/python:latest@sha256:31dd69d84fe5235fe18f8ceb5b76620e6f44a0ce1f9b0d3a46c491576f8b453b
WORKDIR /app

COPY --from=builder /deps /deps
COPY main.py ./
COPY app ./app

# PYTHONDONTWRITEBYTECODE : le conteneur tourne avec un système de fichiers
# racine en lecture seule (cf. infra/k8s/site/site.yaml). Sans cette variable,
# l'interpréteur tente d'écrire des __pycache__ à chaque import et journalise
# un échec par module — du bruit, pour un service qui doit rester lisible.
ENV PYTHONPATH="/app:/deps" \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# L'utilisateur par défaut de l'image EST nonroot (65532) ; on le déclare quand
# même, pour deux raisons. D'abord parce que le dernier USER explicite du
# fichier serait sinon le `root` du builder — trompeur à la lecture, et signalé
# comme tel par l'analyse statique. Ensuite parce qu'un changement de défaut en
# amont ne doit pas pouvoir faire basculer ce conteneur en root en silence.
USER nonroot:nonroot

EXPOSE 8080

# Pas de HEALTHCHECK : l'image n'a pas de shell, et la forme shell de la
# directive en exige un. La vitalité est tenue là où elle est réellement
# observée, par les sondes readiness/liveness de infra/k8s/site/site.yaml, qui
# interrogent la même route /healthz.

# Un seul worker : le service ne fait qu'attendre un relais SMTP quelques
# dizaines de fois par jour. En ajouter n'ajouterait que des compteurs de débit
# indépendants, c'est-à-dire autant de fois la limite réelle.
#
# L'entrypoint de l'image Chainguard EST l'interpréteur : on ne passe que les
# arguments.
CMD ["-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "1", "--proxy-headers"]
