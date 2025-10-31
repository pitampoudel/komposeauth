# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Client SDK

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
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
# Example run (configure env vars as needed)
docker run -p 8080:8080 --env-file .env pitampoudel/komposeauth:latest
```

### Environment variables

```
# App metadata
APP_NAME=                    # Optional. Display name in emails/UI. Default: "komposeauth"
APP_LOGO_URL=                # Optional. Public URL for logo used in emails/UI
SELF_BASE_URL=               # Optional. In prod set to https://auth.example.com
                              # If empty, server derives http://<local-ip>:8080 automatically

# Database
MONGODB_URI=                 # Required. Default: mongodb://localhost:27017 (db: auth)
                              # NOTE: In Docker, localhost points to the container; you must
                              #       set this to a reachable MongoDB URI.

# Google OAuth (Sign in with Google)
GOOGLE_OAUTH_CLIENT_ID=      # Optional. If unset, Google login endpoints stay disabled
GOOGLE_OAUTH_CLIENT_SECRET=  # Optional. If unset, Google login endpoints stay disabled
GOOGLE_AUTH_DESKTOP_CLIENT_ID=      # Optional. Needed for desktop login flows
GOOGLE_AUTH_DESKTOP_CLIENT_SECRET=  # Optional. Needed for desktop login flows

# Email (SMTP)
SMTP_HOST=                   # Optional. Default: smtp.gmail.com
SMTP_PORT=                   # Optional. Default: 587
SMTP_USERNAME=               # Optional. Required only if you want to send emails
SMTP_PASSWORD=               # Optional. Required only if you want to send emails
SMTP_FROM=                   # Optional. Recommended when sending emails

# Phone OTP (Twilio)
TWILIO_ACCOUNT_SID=          # Optional. Required only if phone OTP is needed
TWILIO_VERIFY_SERVICE_SID=   # Optional. Required only if phone OTP is needed
TWILIO_AUTH_TOKEN=           # Optional. Required only if phone OTP is needed
TWILIO_FROM_NUMBER=          # Optional. Required only if phone OTP is needed

# External services / integrations
SAMAYE_API_KEY=              # Optional
SENTRY_DSN=                  # Optional. Enables Sentry reporting

# File uploads / key storage
GCP_BUCKET_NAME=             # Optional. If set, uses Google Cloud Storage
EXPECTED_GCP_PROJECT_ID=     # Required if at the gcp environment. Verifies it matches the active GCP project

# Android App Links / Digital Asset Links
ASSET_LINKS_JSON=            # Optional. JSON array; default: []
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

Initialize client

```kotlin
initializeKomposeAuth(
  authUrl = "https://your-auth-server",
  hosts = listOf("https://your-resource-server")
)
```
Ktor client with Bearer auth

```kotlin
val httpClient = HttpClient {
  install(Auth) {
    setupBearerAuth(this)
  }
  // required to to make public key authentication work
  install(HttpCookies) {
    storage = AcceptAllCookiesStorage()
  }
}
```

```kotlin
initializeKomposeAuthViewModels(
  httpClient=httpClient
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
val credentialManager = rememberCredentialManager()
LaunchedEffect(Unit) {
    val cred = credentialManager.getCredential(state.options)
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

## Contributing

- Issues and PRs are welcome
- Please run `./gradlew build` before submitting a PR
- For larger changes, consider opening an issue first to discuss direction

## Security

If you discover a security vulnerability, please email the maintainers or open a private security
advisory. Avoid filing public issues with sensitive details.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
