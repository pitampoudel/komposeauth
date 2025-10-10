import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // kotlin
                api(libs.kotlinx.datetime)
                // koin
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                // ktor client
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                api(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)
                // settings
                implementation(libs.multiplatformSettings.noArg)
                implementation(libs.multiplatformSettings.coroutines)
                implementation(libs.multiplatform.settings.serialization)
                // lifecycle viewmodel
                api(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.libphonenumber.kotlin)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmMain by getting {

        }
        val androidMain by getting {
         
        }

    }
}

android {
    namespace = "com.vardansoft.authx"
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
    coordinates(group.toString(), "authx", version.toString())

    pom {
        name = "AuthX"
        description = "Kotlin Multiplatform authentication library"
    }
}
