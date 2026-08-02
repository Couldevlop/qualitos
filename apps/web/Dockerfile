# Multi-stage : build Angular puis nginx pour servir les assets.

# -----------------------------------------------------------------------------
# Stage 1 : build Angular
# -----------------------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /workspace

COPY package.json ./
# ngsw-config.json : requis par le build prod depuis l'ajout du Service Worker
# PWA (a77df3a) — sans lui, `ng build` échoue ("could not be found").
COPY angular.json tsconfig.json tsconfig.app.json tsconfig.spec.json karma.conf.js ngsw-config.json ./
# package-lock.json est facultatif (cas du bootstrap initial où il n'existe pas).
COPY package-lock.json* ./

RUN if [ -f package-lock.json ]; then \
      npm ci --no-audit --no-fund; \
    else \
      npm install --no-audit --no-fund; \
    fi

COPY src ./src

# Build multilingue : produit dist/qualitos-web/browser/{fr,en,es,ar,ja,zh}/,
# chaque locale avec son <base href="/<locale>/"> et son ngsw propre.
RUN npm run build:i18n

# -----------------------------------------------------------------------------
# Stage 2 : runtime nginx
# -----------------------------------------------------------------------------
# Variante NON PRIVILÉGIÉE de l'image officielle. L'image `nginx` classique
# démarre son processus maître en root ; le chart impose `runAsNonRoot`, si bien
# que le conteneur était refusé par le kubelet et ne pouvait pas démarrer sur le
# cluster. Cette variante tourne sous l'UID 101 de bout en bout et écoute sur
# 8080, un port non privilégié — c'est le correctif de fond, préférable à un
# assouplissement de la politique de sécurité pour ce seul service.
FROM nginxinc/nginx-unprivileged:1.29-alpine

# Règle §18.2.1 (0 CVE Critique/Haute) : applique les correctifs alpine publiés
# après le build de l'image nginx (ex. libxml2, nghttp2-libs). Sans coût quand
# la base est déjà à jour. Nécessite root, d'où le retour explicite à l'UID 101
# juste après : c'est le dernier USER du fichier qui détermine l'utilisateur
# d'exécution.
USER root
RUN apk upgrade --no-cache
USER 101

# Copie les 6 sous-dossiers de locale (fr/ en/ es/ ar/ ja/ zh/).
COPY --from=build /workspace/dist/qualitos-web/browser /usr/share/nginx/html

# La conf nginx est un TEMPLATE : l'entrypoint officiel de l'image nginx applique
# envsubst sur /etc/nginx/templates/*.template au démarrage et écrit le résultat
# dans /etc/nginx/conf.d/. `NGINX_ENVSUBST_FILTER` restreint la substitution aux
# variables préfixées QOS_ — sans ce filtre, envsubst dévorerait aussi les
# variables nginx du fichier ($http_accept_language, $qos_locale, $is_args$args…).
COPY nginx.conf /etc/nginx/templates/default.conf.template
ENV NGINX_ENVSUBST_FILTER="^QOS_"

# Origines autorisées par la CSP (connect-src). Valeur par défaut = stack locale
# docker-compose. En production, surcharger cette variable d'environnement avec les
# URLs publiques (API + Keycloak) — aucun rebuild d'image nécessaire.
# Configuration runtime de la SPA (servie en assets/config.json, cf. nginx.conf).
# Valeurs par défaut = pile docker-compose locale. En déploiement, on surcharge
# ces trois variables — aucune reconstruction d'image n'est nécessaire.
ENV QOS_API_BASE_URL="http://localhost:8082"     QOS_KEYCLOAK_ISSUER="http://localhost:8080/realms/qualitos"     QOS_KEYCLOAK_CLIENT_ID="qualitos-web"

ENV QOS_CONNECT_SRC="'self' http://localhost:8080 http://localhost:8181 http://localhost:8082"

EXPOSE 8080
# La racine `/` répond désormais 302 vers /<locale>/ (négociation Accept-Language).
# `wget` suit les redirections par défaut → atterrit sur /fr/index.html (200) et
# le HEALTHCHECK reste sain. `--max-redirect=5` borne la chaîne par prudence.
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --max-redirect=5 -O- http://localhost:8080/ >/dev/null || exit 1
