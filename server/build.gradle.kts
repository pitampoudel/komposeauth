import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    alias(libs.plugins.kotlinx.serialization)
}

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
}

dependencies {
    //noinspection UseTomlInstead
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.0")
    "developmentOnly"("org.springframework.boot:spring-boot-devtools")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-webauthn")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Passkeys
    implementation("org.springframework.security:spring-security-web")
    implementation("com.webauthn4j:webauthn4j-core:0.29.7.RELEASE")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0")

    // Sentry for error tracking
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.29.0")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")

    // Google Cloud Platform
    implementation("com.google.cloud:spring-cloud-gcp-storage:7.4.2")

    // OpenAPI documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

    //noinspection UseTomlInstead
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")

}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
