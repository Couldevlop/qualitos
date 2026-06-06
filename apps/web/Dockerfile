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

RUN npm run build:prod

# -----------------------------------------------------------------------------
# Stage 2 : runtime nginx
# -----------------------------------------------------------------------------
FROM nginx:1.29-alpine

# Règle §18.2.1 (0 CVE Critique/Haute) : applique les correctifs alpine publiés
# après le build de l'image nginx (ex. libxml2, nghttp2-libs). Sans coût quand
# la base est déjà à jour.
RUN apk upgrade --no-cache

COPY --from=build /workspace/dist/qualitos-web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null || exit 1
