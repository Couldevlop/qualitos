# syntax=docker/dockerfile:1
# Build autonome (hors réacteur qualitos) — porte les deps Fabric Gateway SDK.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Image d'exécution Chainguard (surface d'attaque minimale, CLAUDE.md §10.2).
#
# Migré avec les trois autres services Java le 2026-09-01 : la base
# distroless/java21-debian12 accumulait des CVE non corrigeables faute de
# gestionnaire de paquets, et son rebuild amont accusait un mois de retard.
# Ce service n'est pas déployé en préproduction (il n'est construit que par
# `release.yml`) : il aurait donc echoué au premier tag, sur un défaut déjà
# connu ailleurs. Sa migration n'a PAS été éprouvée par un démarrage réel,
# faute de réseau Fabric — contrairement aux trois autres.
FROM cgr.dev/chainguard/jre:latest
WORKDIR /app
COPY --from=build /src/target/blockchain-service-*.jar /app/app.jar
EXPOSE 8090
# UID numérique : image sans shell, et exigence Kubernetes `runAsNonRoot`.
USER 65532:65532
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/app.jar"]
