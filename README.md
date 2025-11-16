# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Client SDK

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
[![CI](https://github.com/pitampoudel/komposeauth/actions/workflows/ci.yml/badge.svg)](https://github.com/pitampoudel/komposeauth/actions/workflows/ci.yml)
[![Docker](https://img.shields.io/badge/GHCR-komposeauth-blue?logo=docker)](https://ghcr.io/pitampoudel/komposeauth)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Overview

- Server: Spring Boot Application
- Shared KMP SDK: Shared DTOs and utilities used by client and server
- Client CMP SDK: Ktor, ViewModels, platform utilities, and reusable UI components

## Features

- Federated authorization with Google
- username/password
- passkey
- email verification
- Phone OTP,
- KYC
- Sentry, Swagger/OpenAPI (eg: https://auth.vardansoft.com/swagger-ui.html)

## Shared KMP SDK

- DTOs shared between client and server
- RegexUtils, KmpFile, DateTimeUtils, KtorClientUtils, PhoneNumberParser, validators etc

## Quickstart

### 1) Run the Auth Server (Docker)

```bash
docker pull pitampoudel/komposeauth:latest
# Quick start: only MONGODB_URI is required to start with
docker run -p 8080:8080 -e MONGODB_URI="mongodb://your-mongo-host:27017/auth" -e BASE64_ENCRYPTION_KEY="" pitampoudel/komposeauth:latest
```

- After the container is running, open: http://localhost:8080/setup to configure the rest of the settings in a guided web UI.
- You can still pass other env vars up-front if you prefer automation; environment variables always take precedence over values saved via the Setup UI (database).

### Environment variables

```
# Database
MONGODB_URI=                 # Required. Mongo DB connection string

# Security
BASE64_ENCRYPTION_KEY=       # Required

# App metadata
APP_NAME=                    # Display name in emails/UI. Default: "komposeauth"
APP_LOGO_URL=                # Public URL for logo used in emails/UI
SELF_BASE_URL=               # Default: http://<local-ip>:8080

# Google OAuth (Sign in with Google)
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_AUTH_DESKTOP_CLIENT_ID=
GOOGLE_AUTH_DESKTOP_CLIENT_SECRET=

# SMTP
SMTP_HOST=
SMTP_PORT=                   # Default: 587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM_EMAIL=

# Twilio
TWILIO_ACCOUNT_SID=
TWILIO_VERIFY_SERVICE_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM_NUMBER=

# External services
SAMAYE_API_KEY=              # No Need
SENTRY_DSN=                  # Enables Sentry reporting

# File uploads
GCP_BUCKET_NAME=             # If set, uses Google Cloud Storage
EXPECTED_GCP_PROJECT_ID=     # Verifies it matches the active GCP project

# sccess control
ALLOWED_ANDROID_SHA_256_LIST=
ALLOWED_CORS_ORIGIN_LIST
```

```kotlin
// BASE64_ENCRYPTION_KEY generator
fun main() {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256) // or 128/192
    val key = keyGen.generateKey()
    val base64Key = Base64.getEncoder().encodeToString(key.encoded)
    println(base64Key)
}
```
### 2) Add the SDKs to your KMP project

Shared module

```kotlin
// Check the badge above for the latest version
implementation("io.github.pitampoudel:komposeauth-shared:x.x.x")
```

Client module

```kotlin
// Check the badge above for the latest version
implementation("io.github.pitampoudel:komposeauth-client:x.x.x")
```

HttpClient example (at each platform)
```kotlin
val httpClient = HttpClient {
    installKomposeAuth(
        authServerUrl = "https://your-auth-server",
        resourceServerUrls = listOf(
            "https://your-resource-server"
        )
    )
}
```

Initialize SDK
```kotlin
initializeKomposeAuth(
    httpClient = httpClient
)
```

## Usage snippets (Client)

Utilities

- ScreenStateWrapper(...) with InfoDialog and Progress dialog
- CountryPicker(...), DateTimeField(...), OTPTextField(...)
- rememberFilePicker(input, selectionMode, onPicked)
- rememberKmpCredentialManager()
- registerSmsOtpRetriever(onRetrieved)
- (ENUM, GeneralValidationError).toStringRes()

Current user

```kotlin
val userState = rememberCurrentUser()
```

Login with Credential Manager

```kotlin
val vm = koinViewModel<LoginViewModel>()
val state = vm.state.collectAsStateWithLifecycle().value
val credentialManager = rememberKmpCredentialManager()
LaunchedEffect(state.loginConfig) {
    state.loginConfig?.let {
        when (val result = credentialManager.getCredential(it)) {
            is Result.Error -> vm.onEvent(LoginEvent.ShowInfoMsg(result.message))
            is Result.Success<Credential> -> vm.onEvent(LoginEvent.Login(result.data))
        }
    }
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

## Contributing

- Issues and PRs are welcome
- Please run `./gradlew build` before submitting a PR
- For larger changes, consider opening an issue first to discuss direction

## Security

If you discover a security vulnerability, please email the maintainers or open a private security
advisory. Avoid filing public issues with sensitive details.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
