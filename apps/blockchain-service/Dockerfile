# syntax=docker/dockerfile:1
# Build autonome (hors réacteur qualitos) — porte les deps Fabric Gateway SDK.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Image d'exécution distroless (surface d'attaque minimale, CLAUDE.md §10.2).
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=build /src/target/blockchain-service-*.jar /app/app.jar
EXPOSE 8090
USER nonroot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
