# AuthX

A Kotlin Multiplatform authentication solution: Authorization Server (Spring Boot) + KMP Client Library + Compose Multiplatform UI.

## Overview

AuthX provides end-to-end authentication for Kotlin Multiplatform apps.
- Server: OAuth 2.1 Authorization Server with OIDC userinfo, Google sign-in grant, user management, phone OTP, email verification, credentials, and client registration
- Shared (KMP) library: typed client, token refresh utilities, Koin modules
- UI library (Compose Multiplatform): drop-in OTP and Login flows with platform-specific credential retrieval

Modules in this repository:
1. shared (artifact: authx) – Core auth client for KMP
2. ui (artifact: authx-ui) – UI components for Compose Multiplatform
3. server – Spring Boot authorization server

## Server (Spring Boot)

### Configuration (Environment Variables)
Set the following variables before running the server:
- BASE_URL: Public base URL where the auth server is reachable (e.g., https://auth.example.com)
- MONGODB_URI: Mongo connection URI
- GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET: Enable Google sign-in (optional)
- SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM: Enable email features (optional)
- SMS_API_KEY: Enable SMS OTP (optional)
- APP_NAME: App display name (default: AuthX)
- GCP_BUCKET_NAME: GCP storage bucket name
- GCP_PROJECT_ID: Expected GCP Project ID
- APP_LOGO_URL: Full URL to app logo

These map to Spring configuration (see server/src/main/resources/application.yml) and AppProperties.


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
    // Core auth module
    implementation("com.vardansoft:authx:1.1.3")

    // Optional UI components
    implementation("com.vardansoft:authx-ui:1.1.3")
}
```

### Shared library setup
1) Provide Koin modules with your auth base URL:
```kotlin
val AUTH_URL = "https://auth.yourdomain.com"

startKoin {
    modules(
        com.vardansoft.authx.di.authSharedModule(authUrl = AUTH_URL)
    )
}
```

2) Configure your Ktor HttpClient to use the bearer authenticator with auto-refresh. The helper needs:
- Koin scope to access LoginPreferences
- Your public OAuth client ID (registered on the auth server)
- The auth server base URL
- Hosts list that should receive the Authorization header

```kotlin
val AUTH_URL = "https://auth.yourdomain.com"
val PUBLIC_CLIENT_ID = "your-public-client-id"

val httpClient = io.ktor.client.HttpClient {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        io.ktor.serialization.kotlinx.json.json()
    }
    install(io.ktor.client.plugins.auth.Auth) {
        com.vardansoft.authx.data.utils.applyAuthXBearer(
            scope = org.koin.core.context.GlobalContext.get().koin.scopeRegistry.rootScope,
            clientId = PUBLIC_CLIENT_ID,
            authUrl = AUTH_URL,
            hosts = listOf(java.net.URI(AUTH_URL).host)
        )
    }
}
```

The shared module also publishes these endpoint constants you can use:
```kotlin
com.vardansoft.authx.EndPoints.TOKEN         // "oauth2/token"
com.vardansoft.authx.EndPoints.USER_INFO     // "userinfo"
com.vardansoft.authx.EndPoints.UPDATE_PHONE_NUMBER // "phone-number/update"
com.vardansoft.authx.EndPoints.VERIFY_PHONE_NUMBER // "phone-number/verify"
com.vardansoft.authx.EndPoints.CONFIG        // "config"
```

## UI Module (Compose Multiplatform)
Initialize UI modules with your AUTH_URL:
```kotlin
val AUTH_URL = "https://auth.yourdomain.com"

startKoin {
    modules(com.vardansoft.authx.ui.core.di.getAuthModules(authUrl = AUTH_URL))
}
```

Use provided view models and screens (e.g., OtpScreen, and a Login flow with rememberCredentialRetriever).

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

## Testing
- Run all tests: ./gradlew server:test
- Recommended: keep a local Mongo instance running for integration tests that use Testcontainers (otherwise they pull automatically).

## Notes & Tips
- Java/JVM: Server uses Java 17 toolchain.
- Scopes: Some endpoints require SCOPE_user.read.any or SCOPE_user.write.any.
- Config endpoint: GET {BASE_URL}/config returns the Google OAuth client ID for your client to use.
- Docker: The provided Dockerfile builds the server and runs it with sensible JVM defaults for containers.

## License
Apache 2.0. See the project POM metadata for details.
