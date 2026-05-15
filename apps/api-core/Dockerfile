# Multi-stage : build Maven puis distroless pour la runtime.
# Conforme CLAUDE.md §10.2 (images distroless, surface d'attaque minimale).

# -----------------------------------------------------------------------------
# Stage 1 : build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copie du parent + sous-modules (cache des deps Maven optimal).
COPY pom.xml ./
COPY apps/api-core/pom.xml apps/api-core/
COPY apps/api-quality-engine/pom.xml apps/api-quality-engine/

# Pré-télécharge les dépendances pour profiter du cache Docker.
RUN mvn -B -ntp -pl apps/api-core -am dependency:go-offline || true

# Copie des sources et build du seul module api-core.
COPY apps/api-core/src apps/api-core/src
RUN mvn -B -ntp -pl apps/api-core -am package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2 : runtime (distroless Java 21)
# -----------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=build /workspace/apps/api-core/target/*.jar /app/app.jar

EXPOSE 8081
USER nonroot:nonroot

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
