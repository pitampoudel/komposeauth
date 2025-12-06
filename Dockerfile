FROM gradle:8.14.1-jdk21 AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle . .
RUN chmod +x ./gradlew

RUN ./gradlew :server:bootJar --no-daemon

FROM gcr.io/distroless/java21:nonroot
WORKDIR /app

LABEL org.opencontainers.image.title="komposeauth" \
      org.opencontainers.image.description="Spring Boot auth server for komposeauth (Kotlin Multiplatform full-stack auth)" \
      org.opencontainers.image.url="https://github.com/pitampoudel/komposeauth" \
      org.opencontainers.image.source="https://github.com/pitampoudel/komposeauth" \
      org.opencontainers.image.licenses="Apache-2.0"

COPY --from=build /home/gradle/src/server/build/libs/app.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-Dserver.port=${PORT} -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV TZ=UTC

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
