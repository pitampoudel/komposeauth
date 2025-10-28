import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.sentry.jvm.gradle") version "5.12.1"
    alias(libs.plugins.kotlinx.serialization)
}

sentry {
    includeSourceContext = true
    org = "vardan-soft-pvt-ltd"
    projectName = "komposeauth"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    "developmentOnly"(libs.spring.boot.devtools)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.crypto)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.mail)

    // Passkeys
    implementation(libs.spring.security.web)
    implementation(libs.webauthn4j.core)

    implementation(libs.kotlinx.datetime)

    // kotlinx serialization
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.kotlinx.serialization.core.jvm)

    // Load .env into Spring Environment so application.yml can use ${VAR}
    implementation(libs.spring.dotenv)

    // Sentry for error tracking
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.spring.boot.starter.jakarta)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.mockito.kotlin)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)

    // Google Cloud Platform
    implementation(libs.spring.cloud.gcp.storage)

    // OpenAPI documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.testcontainers)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mongodb)

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
