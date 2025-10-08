# AuthX

Full‑stack authentication for Kotlin Multiplatform: Spring Authorization Server + KMP SDK + Compose Multiplatform UI.

## Maven Central

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx)

## What is AuthX?
AuthX is a production‑ready auth platform tailored for Kotlin Multiplatform apps.
- Server (Spring Boot): OAuth 2.1 Authorization Server + Resource Server, OIDC userinfo, Google sign‑in, password login, email verification, phone OTP, client registration, JWT with custom claims, MongoDB persistence.
- Shared KMP SDK (artifact: authx): Auth client with token storage and auto‑refresh, Koin modules, helpers for configuring Ktor.
- Compose Multiplatform UI (artifact: authx-ui): Login flow, OTP screen, Logout handler, current user CompositionLocal, and platform credential retrieval utilities.

Modules in this repository:
1. shared – Core KMP client library (published as com.vardansoft:authx)
2. ui – Compose Multiplatform UI library (published as com.vardansoft:authx-ui)
3. server – Spring Boot OAuth2/OIDC authorization server and APIs

## Key features
- OAuth 2.1 with Authorization Server and Resource Server
- OIDC userinfo; JWT access tokens with custom claims (authorities, names)
- Google OAuth (web/desktop), password login, email verification, phone OTP
- Public client auth support for mobile/desktop apps
- Config endpoint: GET {BASE_URL}/config returns Google client ID (desktop=true for desktop)
- MongoDB storage; automatic indexes; Testcontainers tests
- Optional SMS via Twilio or Samaye; SMTP email for verification/reset
- Dockerfile for containerized server build/run

## Server (Spring Boot)

### Prerequisites
- Java 17+
- MongoDB (set MONGODB_URI)

### Docker
- Build: docker build -t authx-server .
- Run: docker run -p 8080:8080 --env-file .env authx-server

## Client integration (KMP)
Add dependencies:

```kotlin
dependencies {
    // Core KMP client
    implementation("com.vardansoft:authx:x.x.x")

    // Optional Compose Multiplatform UI
    implementation("com.vardansoft:authx-ui:x.x.x")
}
```

Initialize Koin/AuthX and configure your hosts:
```kotlin
koinConfiguration {
    configureAuthX(
        authUrl = Constants.AUTH_SERVER_URL,
        clientId = Constants.AUTH_CLIENT_ID,
        hosts = listOf(Constants.Server.SERVER_URL)
    )
}
```

Configure Ktor HttpClient with bearer auth auto‑refresh via AuthX:
```kotlin
val httpClient = HttpClient {
    install(Auth) {
        val authX = get<AuthX>()
        authX.configureBearer(this)
    }
}
```

### UI components (Compose Multiplatform)
- Current user Local Composition
```kotlin
ProvideUserInfo {
    val userInfoState = LocalUserInfoState.current
    // render using userInfoState (LazyState<UserInfo>)
}
```
- Logout handler
```kotlin
val logoutHandler = rememberLogoutHandler()
logoutHandler.logout()
```
- OTP screen
```kotlin
OtpScreen()
```
- LoginViewModel + credential retrieval
```kotlin
val credentialRetriever = rememberCredentialRetriever()
val viewModel = koinViewModel<LoginViewModel>()
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    viewModel.onEvent(LoginEvent.Login(credentialResult))
}
```

## Notes
- Java toolchain: 17
- OIDC userinfo includes KYC status when configured

## License
Apache 2.0. See project metadata for details.
