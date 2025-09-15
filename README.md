# AuthX

A Kotlin Multiplatform authentication solution: Authorization Server (Spring Boot) + KMP Client Library + Compose Multiplatform UI.

## Maven Central

[![Maven Central](https://maven-badges.sml.io/maven-central/com.vardansoft/authx/badge.svg)](https://search.maven.org/artifact/com.vardansoft/authx)

## Overview

AuthX provides end-to-end authentication for Kotlin Multiplatform apps.
- Server: OAuth 2.1 Authorization Server with OIDC userinfo, Google sign-in grant, user management, phone OTP, email verification, credentials, and client registration
- Shared (KMP) library: exposes AuthX, AuthClient, LoginPreferences (with token refresh utilities and Koin modules)
- UI library (Compose Multiplatform): provides LoginViewModel, LogoutHandler, OTP screen, and a Local Composition to access the current user; plus platform-specific credential retrieval

Modules in this repository:
1. shared (artifact: authx) – Core auth client for KMP
2. ui (artifact: authx-ui) – UI components for Compose Multiplatform
3. server – Spring Boot authorization server

## Server (Spring Boot)

### Configuration (Environment Variables)
You can set real environment variables or place them in a `.env` file in the project root (or the `server` module directory) for local development.

For convenience, copy `.env.example` to `.env` and fill in your environment-specific values.

## Client Integration (KMP)
Add the AuthX dependencies to your project:

```kotlin
// In your build.gradle.kts
dependencies {
    // Core auth module (for shared)
    implementation("com.vardansoft:authx:x.x.x")

    // Optional UI components (for composeApp)
    implementation("com.vardansoft:authx-ui:x.x.x")
}
```

1) Initialize with your AUTH_URL, public client id, and hosts list:
```kotlin
koinConfiguration {
    configureAuthX(
        authUrl = Constants.AUTH_SERVER_URL,
        clientId = Constants.AUTH_CLIENT_ID,
        hosts = listOf(
            Constants.Server.SERVER_URL
        )
    )
}

```

2) Configure your Ktor HttpClient to use the bearer authenticator with auto-refresh by delegating to AuthX.

```kotlin
val httpClient = HttpClient {
    install(Auth) {
        val authX = get<AuthX>()
        authX.configureBearer(this)
    }
}
```

Use provided view models and screens (e.g., OtpScreen, and a Login flow with rememberCredentialRetriever).

### UI Components

#### Current user Local Composition
```kotlin
ProvideUserInfo {
    val userInfoState = LocalUserInfoState.current
    // use userInfoState (LazyState<UserInfo>) to render UI
}
```

#### LogoutHandler
```kotlin
val logoutHandler = rememberLogoutHandler()
// e.g., inside a Button onClick:
logoutHandler.logout()
```

#### OTP Screen
```kotlin
   OtpScreen()
```

#### LoginViewModel
```kotlin
val credentialRetriever = rememberCredentialRetriever()
val viewModel = koinViewModel<LoginViewModel>()
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    viewModel.onEvent(LoginEvent.Login(credentialResult))
}
```

## Testing
- Run all tests: ./gradlew :server:test
- Recommended: keep a local Mongo instance running for integration tests that use Testcontainers (otherwise they pull automatically).

## Notes & Tips
- Java/JVM: Server uses Java 17 toolchain.
- Scopes: Some endpoints require SCOPE_user.read.any or SCOPE_user.write.any.
- Config endpoint: GET {BASE_URL}/config returns the Google OAuth client ID for your client to use.
- Docker: The provided Dockerfile builds the server and runs it with sensible JVM defaults for containers.

## License
Apache 2.0. See the project POM metadata for details.
