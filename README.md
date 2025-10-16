# AuthX

Full‑stack authentication for Kotlin Multiplatform: Spring Authorization Server + Kotlin Multiplatform SDK + Compose Multiplatform UI.


[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx)

## What is AuthX?
AuthX is a production‑ready auth platform tailored for Kotlin Multiplatform apps.
- Server (Spring Boot): OAuth 2.1 Authorization Server + Resource Server, OIDC userinfo, Google sign‑in, password login, email verification, phone OTP, client registration, JWT with custom claims, MongoDB persistence, OpenAPI docs.
- Shared KMP SDK (artifact: authx): Auth client with token storage and auto‑refresh, Koin DI module, helpers for configuring Ktor Auth.
- Compose Multiplatform UI (artifact: authx-ui): Login flow building blocks (LoginViewModel, OTP UI), KYC/Profile screens, logout handler, CompositionLocals, and platform credential retrieval utilities.

Modules in this repository:
1. shared – Core KMP client library (published as com.vardansoft:authx)
2. ui – Compose Multiplatform UI library (published as com.vardansoft:authx-ui)
3. server – Spring Boot OAuth2/OIDC authorization server and APIs

## Key features
- OAuth 2.1 with Authorization Server and Resource Server
- OIDC userinfo; JWT access tokens with custom claims (e.g., authorities)
- Google OAuth (web/desktop), password login, email verification, phone OTP
- Public client support for mobile/desktop apps
- Optional SMS via Twilio or Samaye; SMTP email for verification/reset
- OpenAPI/Swagger UI for interactive API docs
- Dockerfile for containerized server build/run

## Server (Spring Boot)

### Prerequisites
- Java 17+
- MongoDB (set MONGODB_URI)

### Configure environment
You can use a `.env` file at the project root (loaded by spring-dotenv) or set environment variables directly.

Example .env:
```
APP_NAME=
APP_LOGO_URL=
MONGODB_URI=
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_AUTH_DESKTOP_CLIENT_ID=
GOOGLE_AUTH_DESKTOP_CLIENT_SECRET=
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=
SAMAYE_API_KEY=
TWILIO_ACCOUNT_SID=
TWILIO_VERIFY_SERVICE_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM_NUMBER=
GCP_BUCKET_NAME=
GCP_PROJECT_ID=
SENTRY_DSN=
SENTRY_AUTH_TOKEN=
BASE_URL=
```

### API docs and endpoints
- OpenAPI UI: {BASE_URL}/swagger-ui.html (or {BASE_URL}/swagger-ui/index.html)
- JWKS: GET {BASE_URL}/oauth2/jwks
- Config: GET {BASE_URL}/config?platform=WEB|ANDROID|IOS|DESKTOP
- Standard OAuth 2.1 endpoints exposed by Spring Authorization Server (authorization, token, introspection, etc.).

## Client integration (KMP)
Add dependencies (Gradle):

```kotlin
dependencies {
    // Core KMP client
    implementation("com.vardansoft:authx:x.x.x")

    // Optional Compose Multiplatform UI
    implementation("com.vardansoft:authx-ui:x.x.x")
}
```

Initialize AuthX:
```kotlin
koinApplication {
   modules(
       configureAuthX(
        authUrl = Constants.AUTH_SERVER_URL,
        hosts = listOf(Constants.Server.SERVER_URL)
    )
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
- Current user CompositionLocals and providers
- Logout handler
- OTP ViewModel
- LoginViewModel + platform credential retrieval
- KYC and Profile screens (ui module)
- Utilities: CountryPicker, FilePicker, OTP Field

Examples:
```kotlin
ProvideUserInfo {
    val userInfoState = LocalUserInfoState.current
    // render using userInfoState (LazyState<UserInfo>)
}
```
```kotlin
val logoutHandler = rememberLogoutHandler()
logoutHandler.logout()
```
```kotlin
// Credential Retriever + Login
val credentialRetriever = rememberCredentialRetriever()
val viewModel = koinViewModel<LoginViewModel>()
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    viewModel.onEvent(LoginEvent.Login(credentialResult))
}
```

## Notes
- Java toolchain: 17
- OpenAPI is enabled via springdoc-openapi
- OIDC userinfo can include KYC status when configured

## License
Apache 2.0.
