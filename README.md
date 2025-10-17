# AuthX

Full‑stack authentication for Kotlin Multiplatform: Spring Authorization Server + Kotlin Multiplatform SDK + Compose Multiplatform UI.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

AuthX is a production‑ready, open‑source authentication platform tailored for Kotlin Multiplatform apps. It consists of:
- Server (Spring Boot): OAuth 2.1 Authorization Server + Direct Auth Server, OIDC userinfo, Google sign‑in, password login, email, phone OTP, KYC flows, OpenAPI docs, Sentry logging.
- Shared KMP SDK (artifact: com.vardansoft:authx): Core APIs, helpers for configuring Ktor Auth with automatic token refresh, and common utilities.
- Compose Multiplatform UI (artifact: com.vardansoft:authx-ui): Login, OTP, KYC, Profile, logout handler, CompositionLocals, and platform credential retrieval utilities.

This repository hosts all three modules and can be used end‑to‑end or as individual components.


Table of Contents
- Features
- Architecture & Modules
- Getting Started
  - Server Setup
  - Client SDK (KMP)
  - UI (Compose Multiplatform)
- API Overview
- Development
- Contributing
- Security
- License


Features
- OAuth 2.1 Authorization Server + Direct Auth Server
- OIDC userinfo; JWT access tokens with custom claims (e.g., authorities)
- Google OAuth (web/desktop), password login, email verification, phone OTP
- Public client support for mobile/desktop apps
- Optional SMS via Twilio or Samaye; SMTP email for verification/reset
- OpenAPI/Swagger UI for interactive API docs
- Dockerfile for containerized server build/run
- Koin DI module for easy setup across platforms


Architecture & Modules
- shared — Core KMP client library (published as com.vardansoft:authx)
  - Key class: AuthX
  - DI: authXSharedModule(authUrl, serverUrls)
  - Ktor Auth integration: AuthX.configureBearer(AuthConfig)
- ui — Compose Multiplatform UI library (published as com.vardansoft:authx-ui)
  - DI: KoinApplication.configureAuthX(authUrl, hosts)
  - ProvideAuthX composition local for user state
  - ViewModels: LoginViewModel, OtpViewModel, KycViewModel, ProfileViewModel
  - UI utilities: CountryPicker, OTP field, file picker helpers
- server — Spring Boot OAuth2/OIDC authorization server and APIs
  - Spring Authorization Server + springdoc-openapi
  - MongoDB persistence
  - Sentry, SMTP, optional Twilio/Samaye integrations


Getting Started

Server Setup
Prerequisites
- Java 17+
- MongoDB (set MONGODB_URI)

Configuration
You can use a .env file at the project root (loaded by spring-dotenv) or set environment variables directly.
```
Example .env
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

Run locally
- From IDE: run main class com.vardansoft.authx.AuthApplication
- Gradle: ./gradlew :server:bootRun
- Docker: docker build -t authx-server . then docker run -p 8080:8080 --env-file .env authx-server

API doc
- OpenAPI UI: {BASE_URL}/swagger-ui.html (or {BASE_URL}/swagger-ui/index.html)


Client SDK (KMP)
Add dependencies (Gradle)
dependencies {
    // Core KMP client
    implementation("com.vardansoft:authx:x.x.x")

    // Optional Compose Multiplatform UI
    implementation("com.vardansoft:authx-ui:x.x.x")
}

Dependency Injection (Koin)
// Configure shared + UI modules
koinApplication {
modules(
    configureAuthX(
        authUrl = "https://your-auth-server",
        hosts = listOf("https://your-api-server")
    )
)
}

Configure Ktor HttpClient with bearer auth auto‑refresh via AuthX:
```kotlin
val httpClient = HttpClient {
    install(Auth) {
        val authX = get<AuthX>()
        authX.configureBearer(this)
    }
}
```


UI (Compose Multiplatform)
- Screen state wrapper with progress and info dialogs
```kotlin

```
- Logout handler
```kotlin
val logoutHandler = rememberLogoutHandler()
logoutHandler.logout()
```
- OTP ViewModel
- LoginViewModel + platform credential retrieval
```kotlin
val viewModel = koinViewModel<LoginViewModel>()
val credentialRetriever = rememberCredentialRetriever()
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    viewModel.onEvent(LoginEvent.Login(credentialResult))
}
```
- Profile Viewmodel (current profile, update, deactivate)
- KYC and Profile screens (ui module)
- Utilities: CountryPicker, FilePicker, OTP Field
- Composition Local for current user
```kotlin
ProvideAuthX {
    val userState = LocalUserState.current // LazyState<UserInfoResponse>
    // render UI with userState
}
```


Development
Project layout
- :shared — common KMP library code
- :ui — Compose Multiplatform UI components & ViewModels
- :server — Spring Boot application

Build
- Build all: ./gradlew build
- Run server: ./gradlew :server:bootRun
- Run tests: ./gradlew test

Publishing (artifacts)
Artifacts are published under the com.vardansoft group. Replace x.x.x with the latest version from Maven Central.
- com.vardansoft:authx — KMP SDK
- com.vardansoft:authx-ui — Compose Multiplatform UI

Contributing
- Issues and PRs are welcome.
- Please run ./gradlew build before submitting a PR.
- For larger changes, consider opening an issue first to discuss direction.

Security
If you discover a security vulnerability, please email the maintainers or open a private security advisory. Avoid filing public issues with sensitive details.

License
Apache License 2.0. See LICENSE for details.
