import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinx.serialization)
}

group = "com.vardansoft"
version = "1.0.7"

buildkonfig {
    packageName = "com.vardansoft.auth"
    defaultConfigs {
        listOf(
            "AUTH_GOOGLE_ID",
            "AUTH_URL"
        ).forEach { key ->
            buildConfigField(
                type = FieldSpec.Type.STRING,
                name = key,
                value = project.properties[key] as? String ?: ""
            )
        }
    }
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // kotlin
                api(libs.kotlinx.datetime)
                // koin
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                api(libs.koin.compose.viewmodel)
                // ktor client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)
                // datastore preferences
                implementation(libs.androidx.datastore.preferences.core)
                // settings
                implementation(libs.multiplatformSettings)
                // lifecycle viewmodel
                api(libs.jetbrains.lifecycle.viewmodel)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.vardansoft.auth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates(group.toString(), "auth", version.toString())

    pom {
        name = "Auth"
        description = "Authentication library for VardanSoft"
    }
}
