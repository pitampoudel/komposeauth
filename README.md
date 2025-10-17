# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Compose Multiplatform UI.

[![Maven Central (shared)](https://img.shields.io/maven-central/v/com.vardansoft/komposeauth-shared.svg)](https://central.sonatype.com/artifact/com.vardansoft/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/com.vardansoft/komposeauth-client.svg)](https://central.sonatype.com/artifact/com.vardansoft/komposeauth-client)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Overview
* Server: OAuth 2.1/OIDC, Direct Auth (REST API)

* Shared KMP SDK: Shared DTOs and utilities between client and server

* Client CMP SDK: Ktor, ViewModels, CompositionLocals, platform utilities, components.

## Server
- OAuth 2.1 Authorization Server and Direct Auth (REST API)
- Federated authorization with Google, username/password, email verification, phone OTP, KYC, OpenAPI, Sentry.
- OpenAPI/Swagger: https://auth.vardansoft.com/swagger-ui.html

### Setup
`docker pull pitampoudel/komposeauth:latest`

#### Environment Variables
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

## Shared KMP SDK
```kotlin
// Check the badge above for the latest version
implementation("com.vardansoft:komposeauth-shared:x.x.x")
```
### Components
- DTOs shared between client and server 
- RegexUtils, KmpFile, DateTimeUtils, KtorClientUtils, PhoneNumberParser, validators etc

## Client CMP SDK

### Setup
```kotlin
// Check the badge above for the latest version
implementation("com.vardansoft:komposeauth-client:x.x.x")
```

```kotlin
koinApplication {
    modules(
        configureKomposeauth(
            authUrl = "https://your-auth-server",
            hosts = listOf("https://your-api-server")
        )
    )
}
```

#### Setup Ktor Client
```kotlin
val httpClient = HttpClient {
    install(Auth) {
        setupBearerAuth(this)
    }
}
```

### Utilities
  - ScreenStateWrapper(...) with InfoDialog and Progress dialog
  - CountryPicker(...), DateTimeField(...), OTPTextField(...)
  - rememberFilePicker(input, selectionMode, onPicked)
  - rememberCredentialRetriever()
  - registerSmsOtpRetriever(onRetrieved)


### UI Usage
CompositionLocal
```kotlin
ProvideLocalUser {
    val userState = LocalUserState.current
}
```
Login
```kotlin
val vm = koinViewModel<LoginViewModel>()
val credentialRetriever = rememberCredentialRetriever()
LaunchedEffect(Unit) {
    val cred = credentialRetriever.getCredential()
    vm.onEvent(LoginEvent.Login(cred))
}
```
OTP
```kotlin
val vm = koinViewModel<OtpViewModel>()
registerSmsOtpRetriever { code -> 
    // vm.onEvent(OtpEvent.CodeChanged(code))
}
```
Profile
```kotlin
val vm = koinViewModel<ProfileViewModel>()
```
KYC
```kotlin
val vm = koinViewModel<KycViewModel>()
```

# Contributing
- Issues and PRs are welcome.
- Please run ./gradlew build (or gradlew.bat build on Windows) before submitting a PR.
- For larger changes, consider opening an issue first to discuss direction.

# Security
If you discover a security vulnerability, please email the maintainers or open a private security advisory. Avoid filing public issues with sensitive details.

# License
Apache License 2.0. See LICENSE for details.
