import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
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
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Auth library dependency
                api(project(":shared"))

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.datetime)

                // Coil 3 (Multiplatform Compose image loading)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.network.ktor3)

                // Koin for Compose
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // JetBrains Lifecycle
                implementation(libs.lifecycle.runtime.compose)

                // Cryptography
                implementation(libs.cryptography.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling.preview)
                // CREDENTIAL MANAGER API
                implementation(libs.androidx.credentials)
                // optional - needed for credentials support from play services, for devices running
                // Android 13 and below.
                implementation(libs.androidx.credentials.playServicesAuth)
                // Google sign In
                implementation(libs.googleid)
                // OTP code retriever
                implementation(libs.play.services.auth)
                implementation(libs.play.services.auth.api.phone)

                // Country Code Picker
                implementation(libs.ccp)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.cryptography.provider.webcrypto)
            }
        }
    }
}

android {
    namespace = "com.vardansoft.authx.ui"
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
    coordinates(group.toString(), "authx-ui", version.toString())

    pom {
        name = "AuthX UI"
        description = "Compose Multiplatform UI components for the AuthX library"
    }
}
