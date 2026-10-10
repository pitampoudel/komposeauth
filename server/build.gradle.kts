import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    alias(libs.plugins.kotlinx.serialization)
}

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
}

extra["sentryVersion"] = "8.27.0"

dependencies {
    //noinspection UseTomlInstead
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.sentry:sentry-spring-boot-4-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.security:spring-security-webauthn")
    implementation("org.springframework.security:spring-security-crypto")
    // Not a reactive stack -- the servlet dispatcher adapts `suspend` controller methods through
    // this bridge, so the handful of them in the codebase stop resolving without it.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.mongodb:mongodb-spring-session:4.0.0")
    implementation(project(":shared"))

    // Google Cloud Platform
    implementation("com.google.cloud:spring-cloud-gcp-storage:7.4.2")
    // The sync client only; the async Netty transport would be a second HTTP stack nothing calls.
    implementation("software.amazon.awssdk:s3:2.55.7") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }

    // kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // HTTP client
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6.1")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.4")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("io.sentry:sentry-bom:${property("sentryVersion")}")
    }
}
kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xmulti-dollar-interpolation"
        )
    }
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // Without this a CI failure reports only the exception type and a line number, which is not
        // enough to tell a wrong assertion from a real defect — the message carries the detail.
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// The LLM integration guide lives at the repository root and in docs/ so it reads well on GitHub, and is
// copied into the static resources so every running server publishes it at /llms.txt and /llms-full.txt.
tasks.processResources {
    from(rootProject.file("llms.txt")) { into("static") }
    from(rootProject.file("docs/llms-full.txt")) { into("static") }
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

