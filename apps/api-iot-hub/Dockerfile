# =============================================================================
# api-iot-hub — distroless image (OWASP A05 / CLAUDE.md §10.2)
# Multi-stage: builder + minimal runtime (no shell, no package manager).
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./
COPY src ./src
# -DskipTests: image built from a tested JAR (CI runs tests separately).
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=builder /build/target/api-iot-hub-*.jar /app/app.jar
USER nonroot:nonroot
EXPOSE 8083
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
