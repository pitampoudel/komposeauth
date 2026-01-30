plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
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

val tag: String? = System.getenv("GITHUB_REF_NAME")
val versionFromTag = tag?.takeIf { it.startsWith("v") }?.removePrefix("v") ?: "1.0.0-SNAPSHOT"
version = versionFromTag
allprojects {
    version = versionFromTag
}