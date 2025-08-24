# Auth KMP

A Kotlin Multiplatform project for authentication and user management in mobile and web applications.

## Overview

Auth KMP provides a complete authentication solution for Kotlin Multiplatform projects. It offers two main artifacts:

1. **auth** - Core authentication module

2. **auth-ui** - UI components for authentication flows

## Installation

Add the Auth KMP dependencies to your project:

```kotlin
// In your build.gradle.kts
dependencies {
    // Core auth module
    implementation("com.vardansoft:auth:x.y.z")
    
    // Optional UI components
    implementation("com.vardansoft:auth-ui:x.y.z")
}
```

## Setup

### Server (Spring Boot)

Configure via environment variables:
- BASE_URL: Public base URL where the auth server is reachable (e.g., https://auth.example.com).
- MONGODB_URI: Mongo connection string.
- GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET: For Google sign-in (optional if unused).
- SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM: For email features (optional if unused).
- SMS_API_KEY: For SMS OTP (optional if unused).

application.yml already references these variables with sensible defaults. Run locally:
- Gradle: ./gradlew :server:bootRun
- Docker: use Dockerfile and set env vars.

### Core Auth Library (KMP)

1) Add Koin modules providing the auth base URL used by the shared library:

```kotlin
val AUTH_URL = "https://auth.yourdomain.com"

startKoin {
    modules(
        com.vardansoft.auth.di.authSharedModule(authUrl = AUTH_URL)
    )
}
```

2) Configure your Ktor HttpClient to use the bearer authenticator that refreshes tokens automatically. The helper needs:
- Koin scope to access LoginPreferences
- Your public OAuth client ID (registered on the auth server)
- The auth server base URL
- Hosts list to which the Authorization header should be sent

```kotlin
val AUTH_URL = "https://auth.yourdomain.com"
val PUBLIC_CLIENT_ID = "your-public-client-id"

val httpClient = io.ktor.client.HttpClient {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        io.ktor.serialization.kotlinx.json.json()
    }
    install(io.ktor.client.plugins.auth.Auth) {
        com.vardansoft.auth.data.utils.applyAuthBearer(
            scope = org.koin.core.context.GlobalContext.get().koin.scopeRegistry.rootScope,
            clientId = PUBLIC_CLIENT_ID,
            authUrl = AUTH_URL,
            hosts = listOf(java.net.URI(AUTH_URL).host)
        )
    }
}
```

### UI Module (Compose Multiplatform)

Initialize both auth and UI modules with the same AUTH_URL:

```kotlin
val AUTH_URL = "https://auth.yourdomain.com"

startKoin {
    modules(com.vardansoft.auth.ui.core.di.getAuthModules(authUrl = AUTH_URL))
}
```

Use provided view models and screens (e.g., OtpScreen, Login flow with rememberCredentialRetriever).


### UI Components

#### OTP Screen

```kotlin
composable<Screen.Otp> {
    val vm = koinViewModel<OtpViewModel>()
    val state = vm.state.collectAsState().value
    OtpScreen(
        state = state,
        onEvent = vm::onEvent,
        popBackStack = navController::popBackStack,
        uiEvents = vm.uiEvents
    )
}
```

#### Login

```kotlin
// Platform-specific credential retriever
val credentialRetriever = rememberCredentialRetriever(clientId = "your-client-id")

// Use in your login flow
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    credentialResult.onSuccess { credential ->
        viewModel.login(credential)
    }
}
```

## API Reference

### UI Module

- **OtpScreen** - UI component for OTP verification
- **CredentialRetriever** - Platform-specific interface for retrieving credentials
- **ProvideUserInfo** - Composable for providing user information to the UI
- **LocalUserInfo** - CompositionLocal for accessing user information

## Platform Support

- Android
- iOS
- JVM (Desktop)
- Web (JS)
