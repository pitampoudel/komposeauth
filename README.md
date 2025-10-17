# AuthX

Full-stack auth for Kotlin Multiplatform: Spring Authorization Server + KMP SDK + Compose Multiplatform UI.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.vardansoft/authx)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Overview
- Server: OAuth 2.1/OIDC, federated authorization with Google, username/password, email, phone OTP, KYC, OpenAPI, Sentry.
- Shared KMP SDK: core APIs, Ktor Auth integration.
- Compose Multiplatform UI: ViewModels, CompositionLocals, platform utilities, components.

## Server
### Features
- OAuth 2.1 Authorization Server and Direct Auth endpoints
- Google OAuth, password login, email verification, phone OTP
- Public client support (mobile/desktop)
- SMS via Twilio or Samaye (optional); SMTP email
- OpenAPI/Swagger (https://auth.vardansoft.com/swagger-ui.html)


### Setup
Environment Variables
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

## Client
Install (Gradle)
- implementation("com.vardansoft:authx:x.x.x")
- implementation("com.vardansoft:authx-ui:x.x.x")

Setup AuthX
```kotlin
koinApplication {
    modules(
        configureAuthX(
            authUrl = "https://your-auth-server",
            hosts = listOf("https://your-api-server")
        )
    )
}
```

Setup (Ktor Auth)
```kotlin
val httpClient = HttpClient {
    install(Auth) {
        val authX = get<AuthX>()
        authX.configureBearer(this)
    }
}
```

UI Usage
- Composition Local
```kotlin
ProvideAuthX {
    val userState = LocalUserState.current // LazyState<UserInfoResponse>
}
```
- Login
```kotlin
val vm = koinViewModel<LoginViewModel>()
val credentialRetriever = rememberCredentialRetriever()
LaunchedEffect(Unit) {
    val cred = credentialRetriever.getCredential()
    vm.onEvent(LoginEvent.Login(cred))
}
```
- OTP
```kotlin
val vm = koinViewModel<OtpViewModel>()
registerSmsOtpRetriever { code -> vm.onEvent(OtpEvent.CodeChanged(code)) }
```
- Profile
```kotlin
val vm = koinViewModel<ProfileViewModel>()
```
- KYC
```kotlin
val vm = koinViewModel<KycViewModel>()
```
- Components and utilities
  - CountryPicker(...)
  - rememberFilePicker(input, selectionMode, onPicked)
  - rememberCredentialRetriever()
  - registerSmsOtpRetriever(onRetrieved)

Public Utility classes (shared module)
- Result<T>, InfoMessage, KmpFile, EncodedData etc

UI Components (ui module)
- rememberCredentialRetriever(), registerSmsOtpRetriever(onRetrieved), CountryPicker(...), rememberFilePicker(...)

Contributing
- Issues and PRs are welcome.
- Please run ./gradlew build before submitting a PR.
- For larger changes, consider opening an issue first to discuss direction.

Security
If you discover a security vulnerability, please email the maintainers or open a private security advisory. Avoid filing public issues with sensitive details.

License
Apache License 2.0. See LICENSE for details.
