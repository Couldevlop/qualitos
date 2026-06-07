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

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=builder /workspace/apps/api-iot-hub/target/api-iot-hub-*.jar /app/app.jar
USER nonroot:nonroot
EXPOSE 8083
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
