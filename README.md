# AuthX

A Kotlin Multiplatform authentication solution: Authorization Server (Spring Boot) + KMP Client Library + Compose Multiplatform UI.

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


### API Overview
Public endpoints:
- POST /oauth2/token – Token endpoint (supports public client and custom Google ID token grant)
- GET /config – Returns { googleClientId: String } for client apps
- GET /login, GET /signup, POST /users – HTML forms and user creation

Protected endpoints (require JWT Access Token with scopes):
- GET /userinfo – OIDC userinfo built from your user data
- GET /users/{id}, GET /users/batch – Requires SCOPE_user.read.any
- GET /credentials – Current user’s credentials
- GET /credentials/{userId} – Requires SCOPE_user.read.any
- POST /credentials/{userId} – Requires SCOPE_user.write.any
- POST /phone-number/update – Initiate phone update (sends OTP)
- POST /phone-number/verify – Verify OTP and finalize update
- /oauth2/clients – Client registration/management (ADMIN role)

CORS is enabled for standard methods. See AuthConfig.kt for details and exact rules.

## Client Integration (KMP)
Add the AuthX dependencies to your project (replace 1.1.3 with the version you need):

```kotlin
// In your build.gradle.kts
dependencies {
    // Core auth module (for shared)
    implementation("com.vardansoft:authx:1.1.3")

    // Optional UI components (for composeApp)
    implementation("com.vardansoft:authx-ui:1.1.3")
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

2) Configure your Ktor HttpClient to use the bearer authenticator with auto-refresh by delegating to AuthX from Koin.

```kotlin
val httpClient = io.ktor.client.HttpClient {
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
composable<Screen.Otp> {
    OtpScreen()
}
```

#### Login
```kotlin
// Platform-specific credential retriever
val credentialRetriever = rememberCredentialRetriever()

// Use in your login flow
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    credentialResult.onSuccess { credential ->
        viewModel.login(credential)
    }
}
```

#### LoginViewModel
```kotlin
// Alternatively, dispatch an event to LoginViewModel
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
