import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.plugins.signing.Sign

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.28.0")
        }
    }
}

subprojects {
    // Apply to modules that use vanniktech plugin
    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pom {
                name.set(project.name)
                inceptionYear.set("2025")
                url.set("https://github.com/pitampoudel/komposeauth")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("pitampoudel")
                        name.set("Pitam Poudel")
                        url.set("https://www.pitam.com.np/")
                    }
                }
                scm {
                    url.set("https://github.com/pitampoudel/komposeauth")
                    connection.set("scm:git:https://github.com/pitampoudel/komposeauth.git")
                    developerConnection.set("scm:git:ssh://git@github.com:pitampoudel/komposeauth.git")
                }
            }
        }

        // Skip signing for snapshots
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")
        tasks.withType<Sign>().configureEach {
            onlyIf { !isSnapshot }
        }
    }
}

val versionFromGit: Provider<String> = providers.exec {
    isIgnoreExitValue = true
    commandLine("git", "describe", "--tags", "--exact-match")
}.standardOutput.asText.map { it.trim().removePrefix("v") }
    .orElse(
        providers.exec {
            isIgnoreExitValue = true
            commandLine("git", "describe", "--tags", "--abbrev=0")
        }.standardOutput.asText.map { it.trim().removePrefix("v") }
    )

val resolvedVersion: String = findProperty("version")?.toString()?.takeIf { it != "unspecified" }
    ?: System.getenv("GITHUB_REF_NAME")?.takeIf { it.isNotBlank() }?.removePrefix("v")
    ?: versionFromGit.orNull?.takeIf { it.isNotBlank() }
    ?: "1.0.0-SNAPSHOT"

version = resolvedVersion
allprojects {
    version = resolvedVersion
}