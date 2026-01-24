import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidLibrary {
        namespace = "komposeauth.client"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
        androidResources {
            enable = true
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
                api(project(":shared"))
                // koin
                api(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                api(libs.koin.compose.viewmodel)

                // ktor client
                api(libs.ktor.client.core)
                api(libs.ktor.client.contentNegotiation)
                api(libs.ktor.client.logging)
                api(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)

                // settings
                implementation(libs.multiplatformSettings.core)
                implementation(libs.multiplatformSettings.noArg)
                implementation(libs.multiplatformSettings.coroutines)
                implementation(libs.multiplatformSettings.serialization)
                implementation(libs.multiplatformSettings.makeObservable)

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

                // JetBrains Lifecycle
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.jetbrains.lifecycle.viewmodel)

                // Cryptography
                implementation(libs.cryptography.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling.preview)
                // CREDENTIAL MANAGER API
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                // Google sign In
                implementation(libs.googleid)
                // OTP code retriever
                implementation(libs.play.services.auth)
                implementation(libs.play.services.auth.api.phone)

                // Country Code Picker
                implementation(libs.ccp)

                // Android secure storage
                implementation(libs.androidx.security.crypto)

                // Ktor engine for Android
                api(libs.ktor.client.okhttp)
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

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "komposeauth-client",
        version = project.version.toString()
    )
    pom {
        name.set("komposeauth client")
        description.set("Client library for komposeauth")
    }
}
