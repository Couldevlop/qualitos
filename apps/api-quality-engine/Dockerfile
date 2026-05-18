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
# Parent pom references libs/industry-commons (P3) and apps/api-iot-hub (P3).
# We need a placeholder pom for each referenced sibling module so Maven can
# resolve the parent's <modules> list, even though we only build api-quality-engine.
COPY libs/industry-commons/pom.xml libs/industry-commons/
COPY apps/api-iot-hub/pom.xml apps/api-iot-hub/

RUN mvn -B -ntp -pl apps/api-quality-engine -am dependency:go-offline || true

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
