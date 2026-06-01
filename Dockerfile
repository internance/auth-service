# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Warm the dependency cache first so source-only changes don't re-download.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Build the executable jar (tests run in CI, not in the image build).
# Rename to a fixed filename so the runtime COPY stays deterministic even if
# build/libs ever contains more than one jar.
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test \
	&& mv build/libs/*.jar app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8100
ENTRYPOINT ["java", "-jar", "app.jar"]
