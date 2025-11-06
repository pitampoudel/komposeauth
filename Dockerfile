# Build stage: build only the server module
FROM gradle:8.14.1-jdk17 AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle . .
RUN chmod +x ./gradlew

RUN ./gradlew :server:bootJar --no-daemon

# Runtime stage: distroless Java 17, recommended for Cloud Run
FROM gcr.io/distroless/java17:nonroot
# Distroless already uses a nonroot user
WORKDIR /app

# OCI labels for better metadata
LABEL org.opencontainers.image.title="komposeauth" \
      org.opencontainers.image.description="Spring Boot auth server for komposeauth (Kotlin Multiplatform full-stack auth)" \
      org.opencontainers.image.url="https://github.com/pitampoudel/komposeauth" \
      org.opencontainers.image.source="https://github.com/pitampoudel/komposeauth" \
      org.opencontainers.image.licenses="Apache-2.0"

# Copy fat JAR from builder
COPY --from=build /home/gradle/src/server/build/libs/app.jar /app/app.jar

# Cloud Run provides PORT env var; Spring Boot respects server.port when set.
# Use JAVA_TOOL_OPTIONS so the app binds to the provided port without further config.
ENV JAVA_TOOL_OPTIONS="-Dserver.port=${PORT} -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV TZ=UTC

# Expose is optional for Cloud Run but helpful for local runs
EXPOSE 8080

# Launch application
ENTRYPOINT ["java","-jar","/app/app.jar"]
