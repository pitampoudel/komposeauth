import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.2")
        }
    }
}

group = property("group") as String
version = property("version") as String

subprojects {
    // Apply to modules that use vanniktech plugin
    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pom {
                name.set(project.name)
                description.set("A Kotlin Multiplatform authentication system.")
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
        tasks.withType<Sign>().configureEach {
            onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
        }
    }
}
