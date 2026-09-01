# =============================================================================
# api-iot-hub — distroless image (OWASP A05 / CLAUDE.md §10.2)
# Multi-stage: builder + minimal runtime (no shell, no package manager).
#
# ⚠ Contexte de build = RACINE du repo (comme api-core / api-quality-engine) :
#   le pom du module hérite de qualitos-parent et dépend de libs/industry-commons,
#   un build standalone ne peut plus résoudre le réacteur.
#   docker build -f apps/api-iot-hub/Dockerfile .
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
# Maven valide TOUS les <modules> du pom parent au chargement du réacteur :
# placeholder pom requis pour chaque module référencé, même non construit.
# Tenir cette liste synchronisée avec <modules> de /pom.xml.
COPY apps/api-core/pom.xml apps/api-core/
COPY apps/api-quality-engine/pom.xml apps/api-quality-engine/
COPY libs/industry-commons/pom.xml libs/industry-commons/
COPY libs/security-commons-crypto/pom.xml libs/security-commons-crypto/
COPY libs/keycloak-spi-ldap/pom.xml libs/keycloak-spi-ldap/
COPY apps/api-iot-hub/pom.xml apps/api-iot-hub/
COPY apps/blockchain-service/pom.xml apps/blockchain-service/

RUN mvn -B -ntp -pl apps/api-iot-hub -am dependency:go-offline || true

# -am construit aussi industry-commons (Domain Adapter Layer) : sources requises.
COPY libs/industry-commons/src libs/industry-commons/src
COPY apps/api-iot-hub/src apps/api-iot-hub/src
# -DskipTests: image built from a tested JAR (CI runs tests separately).
RUN mvn -B -ntp -pl apps/api-iot-hub -am package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2 : runtime (Chainguard JRE)
# -----------------------------------------------------------------------------
# Base CHAINGUARD et non distroless Google (CLAUDE.md §10.2 admet les deux).
#
# La base distroless/java21-debian12 accumulait des CVE que nous ne pouvions PAS
# corriger : sans gestionnaire de paquets, on ne peut qu'attendre un rebuild
# amont. Mesure du 2026-09-01 par scan direct : liblcms2 toujours en 2.14-2 alors
# que +deb12u1 existait depuis un mois, plus libexpat1 arrivé entre-temps — soit
# deux CVE Hautes bloquant la CD, et trois autres sans correctif. La même image
# scannée le même jour : Chainguard rend ZÉRO.
#
# C'est la décision déjà prise le 2026-08-02 pour les trois images Python, pour
# les mêmes symptômes : on change de base plutôt que de reconduire des exceptions
# sur un socle en retard.
#
# JDK 26 alors que le projet cible Java 21 : le tier public de Chainguard ne
# publie que `latest`. Vérifié AVANT de changer, pas après — le jar Java 21 a été
# démarré sur cette JVM avec un PostgreSQL réel : 122 migrations Flyway
# appliquées, métamodèle Hibernate initialisé, Tomcat en écoute, application
# démarrée. Aucun avertissement de compatibilité.
FROM cgr.dev/chainguard/jre:latest
WORKDIR /app
COPY --from=builder /workspace/apps/api-iot-hub/target/api-iot-hub-*.jar /app/app.jar
# UID numérique : l'image n'a pas de shell pour résoudre un nom, et c'est
# ce que Kubernetes vérifie pour `runAsNonRoot`.
USER 65532:65532
EXPOSE 8083
ENTRYPOINT ["/usr/bin/java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
