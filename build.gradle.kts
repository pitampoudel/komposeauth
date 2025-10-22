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

group = (findProperty("group") as String)
version = (findProperty("version") as String)

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()

            pom {
                name = project.name
                inceptionYear = "2025"
                url = "https://github.com/pitampoudel/komposeauth"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "pitampoudel"
                        name = "Pitam Poudel"
                        url = "https://www.pitam.com.np/"
                    }
                }
                scm {
                    url = "https://github.com/pitampoudel/komposeauth"
                    connection = "scm:git:https://github.com/pitampoudel/komposeauth.git"
                    developerConnection = "scm:git:ssh://git@github.com:pitampoudel/komposeauth.git"
                }
            }
        }

        // Disable signing for local publishing
        tasks.withType<Sign>().configureEach {
            onlyIf { !gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") } }
        }
    }
}