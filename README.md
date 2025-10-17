# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Compose Multiplatform UI.

[![Maven Central (shared)](https://img.shields.io/maven-central/v/com.vardansoft/komposeauth-shared.svg)](https://central.sonatype.com/artifact/com.vardansoft/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/com.vardansoft/komposeauth-client.svg)](https://central.sonatype.com/artifact/com.vardansoft/komposeauth-client)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Overview

- Server: OAuth 2.1/OIDC Authorization Server + Direct Auth (REST API)
- Shared KMP SDK: Shared DTOs and utilities used by client and server
- Client CMP SDK: Ktor, ViewModels, platform utilities, and reusable UI components

## Server

- OAuth 2.1 Authorization Server and Direct Auth (REST API)
- Federated authorization with Google, username/password, email verification, phone OTP, KYC,
  OpenAPI, Sentry.
- Swagger/OpenAPI: https://localhost:8080/swagger-ui.html

## Shared KMP SDK

- DTOs shared between client and server
- RegexUtils, KmpFile, DateTimeUtils, KtorClientUtils, PhoneNumberParser, validators etc

## Quickstart

### 1) Run the Auth Server (Docker)

```bash
docker pull pitampoudel/komposeauth:latest
# Example run (configure env vars as needed)
docker run -p 8080:8080 --env-file .env pitampoudel/komposeauth:latest
```

### Environment variables

```
APP_NAME=                    # Display name used in emails and UI
APP_LOGO_URL=                # Public URL for logo used in emails/UI
MONGODB_URI=                 # MongoDB connection string
GOOGLE_OAUTH_CLIENT_ID=      # Google OAuth web client id
GOOGLE_OAUTH_CLIENT_SECRET=  # Google OAuth web client secret
GOOGLE_AUTH_DESKTOP_CLIENT_ID=      # Google OAuth desktop client id
GOOGLE_AUTH_DESKTOP_CLIENT_SECRET=  # Google OAuth desktop client secret
SMTP_USERNAME=               # SMTP username for sending emails
SMTP_PASSWORD=               # SMTP password
SMTP_FROM=                   # Sender email address
SAMAYE_API_KEY=              
TWILIO_ACCOUNT_SID=         
TWILIO_VERIFY_SERVICE_SID=  
TWILIO_AUTH_TOKEN=          
TWILIO_FROM_NUMBER=         
GCP_BUCKET_NAME=             # For file uploads
GCP_PROJECT_ID=              # GCP project id
SENTRY_DSN=                  # Sentry DSN
SENTRY_AUTH_TOKEN=           # Sentry auth token
BASE_URL=                    # Public base URL of this server
```

### 2) Add the SDKs to your KMP project

Shared module

```kotlin
// Check the badge above for the latest version
implementation("com.vardansoft:komposeauth-shared:x.x.x")
```

Client module

```kotlin
// Check the badge above for the latest version
implementation("com.vardansoft:komposeauth-client:x.x.x")
```

Initialize client

```kotlin
koinApplication {
    modules(
        configureKomposeauth(
            authUrl = "https://your-auth-server",
            hosts = listOf("https://your-resource-server")
        )
    )
}
```

Ktor client with Bearer auth

```kotlin
val httpClient = HttpClient {
    install(Auth) {
        setupBearerAuth(this)
    }
}
```

## Usage snippets (Client)

Utilities

- ScreenStateWrapper(...) with InfoDialog and Progress dialog
- CountryPicker(...), DateTimeField(...), OTPTextField(...)
- rememberFilePicker(input, selectionMode, onPicked)
- rememberCredentialRetriever()
- registerSmsOtpRetriever(onRetrieved)

Current user

```kotlin
val userState = rememberCurrentUser()
```

Login with Credential Manager

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

Profiles and KYC

```kotlin
val profileVm = koinViewModel<ProfileViewModel>()
val kycVm = koinViewModel<KycViewModel>()
```

## Development

- Build everything: `./gradlew build` (or `gradlew.bat build` on Windows)

## Contributing

- Issues and PRs are welcome
- Please run `./gradlew build` before submitting a PR
- For larger changes, consider opening an issue first to discuss direction

## Security

If you discover a security vulnerability, please email the maintainers or open a private security
advisory. Avoid filing public issues with sensitive details.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
