# syntax=docker/dockerfile:1

FROM eclipse-temurin:21.0.11_10-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8100

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8100/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]