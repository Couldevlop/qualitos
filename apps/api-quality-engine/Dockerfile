# Multi-stage : build Maven puis distroless pour la runtime.
# Conforme CLAUDE.md §10.2 (images distroless, surface d'attaque minimale).

# -----------------------------------------------------------------------------
# Stage 1 : build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY apps/api-core/pom.xml apps/api-core/
COPY apps/api-quality-engine/pom.xml apps/api-quality-engine/
# Maven valide TOUS les <modules> du pom parent au chargement du réacteur :
# chaque module référencé doit avoir son pom présent (placeholder suffisant
# pour les modules non construits). Tenir cette liste synchronisée avec
# <modules> de /pom.xml.
COPY libs/industry-commons/pom.xml libs/industry-commons/
COPY libs/security-commons-crypto/pom.xml libs/security-commons-crypto/
COPY apps/api-iot-hub/pom.xml apps/api-iot-hub/
COPY apps/blockchain-service/pom.xml apps/blockchain-service/

RUN mvn -B -ntp -pl apps/api-quality-engine -am dependency:go-offline || true

# -am construit aussi les dépendances réacteur du module : security-commons-crypto
# (signatures hybrides ML-DSA) a besoin de ses sources.
COPY libs/security-commons-crypto/src libs/security-commons-crypto/src
COPY apps/api-quality-engine/src apps/api-quality-engine/src
RUN mvn -B -ntp -pl apps/api-quality-engine -am package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2 : runtime (distroless Java 21)
# -----------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=build /workspace/apps/api-quality-engine/target/*.jar /app/app.jar

EXPOSE 8082
USER nonroot:nonroot

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
