# Build Stage
FROM gradle:8.14.1-jdk17 AS server-build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x ./gradlew
# Build without tests since they're already run in CI
RUN ./gradlew server:build -x test --no-daemon

# Runtime Stage
FROM openjdk:17-jdk-alpine
ENV TZ=UTC

# Create app directory and non-root user
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring /app
USER spring:spring

# Copy fat JAR
COPY --from=server-build /home/gradle/server/build/libs/app.jar app.jar

# Optimize JVM settings for containerized environment
# - XX:+UseContainerSupport: Enable container support
# - XX:MaxRAMPercentage: Use 75% of available memory
# - XX:InitialRAMPercentage: Start with 50% of available memory
# - XX:+UseG1GC: Use G1 garbage collector
# - XX:MaxGCPauseMillis: Target max GC pause time
# - XX:+DisableExplicitGC: Disable explicit GC calls
# - Xlog:gc: Enable GC logging
# - XX:+ExitOnOutOfMemoryError: Exit on OOM to allow container restart
# - Djava.security.egd: Use /dev/urandom for faster startup
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=100", \
            "-XX:+DisableExplicitGC", \
            "-Xlog:gc:stderr:time", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-Djava.security.egd=file:/dev/urandom", \
            "-jar", "app.jar"]
