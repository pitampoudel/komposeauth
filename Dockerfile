# Build stage: build only the server module
FROM gradle:8.14.1-jdk17 AS build
WORKDIR /home/gradle/src

# Leverage Docker layer caching for dependencies
COPY --chown=gradle:gradle settings.gradle.kts build.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle server/build.gradle.kts ./server/
RUN gradle --version >/dev/null 2>&1 || true
RUN ./gradlew --version >/dev/null 2>&1 || true
RUN ./gradlew :server:dependencies -x test --no-daemon || true

# Copy sources last to avoid busting cache on every change
COPY --chown=gradle:gradle . .
RUN chmod +x ./gradlew
# Build without tests (assumed run in CI)
RUN ./gradlew :server:bootJar -x test --no-daemon

# Runtime stage: distroless Java 17, recommended for Cloud Run
FROM gcr.io/distroless/java17:nonroot
# Distroless already uses a nonroot user
WORKDIR /app

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
