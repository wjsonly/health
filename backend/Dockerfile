# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system health \
    && useradd --system --gid health --home-dir /app --shell /usr/sbin/nologin health \
    && mkdir -p /app/data/uploads \
    && chown -R health:health /app

COPY --from=build /workspace/target/health-backend-0.1.0.jar /app/health-backend.jar

USER health
EXPOSE 8080

ENV HEALTH_UPLOAD_DIRECTORY=/app/data/uploads

ENTRYPOINT ["java", "-jar", "/app/health-backend.jar"]
