# komposeauth

Full-stack authentication and authorization solution for Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP), featuring a Spring Boot Authorization Server and multiplatform client SDKs.

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-komposeauth-blue?logo=docker)](https://hub.docker.com/r/pitampoudel/komposeauth)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Quickstart](#quickstart)
  - [1. Run the Authorization Server (Docker)](#1-run-the-authorization-server-docker)
  - [2. Configuration & Admin Console](#2-configuration--admin-console)
  - [3. Environment Variables](#3-environment-variables)
  - [4. Deployment Guides](#4-deployment-guides)
- [Client SDK Setup](#client-sdk-setup)
  - [Installation](#installation)
  - [HTTP Client Configuration](#http-client-configuration)
  - [Usage Examples](#usage-examples)
- [Integration Guide (LLMs & Backends)](#integration-guide-llms--backends)
- [Versioning](#versioning)
- [Building & Publishing](#building--publishing)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

`komposeauth` provides a complete, modern authentication ecosystem across server and client platforms:

- **Auth Server**: Spring Boot OAuth 2.1 & OpenID Connect Authorization Server with administrative management, rate limiting, and encrypted KYC storage.
- **Shared SDK (`komposeauth-shared`)**: Cross-platform Kotlin Multiplatform data models, DTOs, and validation logic.
- **Client SDK (`komposeauth-client`)**: Compose Multiplatform UI components, Ktor authentication plugins, Credential Manager integrations, and ViewModels for Android, iOS, Desktop, and Web.

---

## Features

- 🔐 **Authentication Flows**: Username/Password, Passkeys (WebAuthn / FIDO2), and Federated Sign-In (Google OAuth2).
- 🛡️ **OAuth 2.1 & OpenID Connect**: PKCE, Client ID Metadata Documents (CIMD), and JWT token management.
- 📱 **Multiplatform Client**: Compose Multiplatform UI components, Credential Manager, and automatic Ktor token interceptors.
- ✉️ **Verification & Recovery**: Email verification codes and SMS OTP retriever support.
- 🪪 **KYC Document Management**: Client-side encryption before S3/GCS upload and restricted retrieval endpoints.
- ⚙️ **Admin Console**: Web UI (`/admin/config`) for runtime configuration of OAuth clients, SMTP, SMS providers, CORS, and rate limits.
- 🚦 **Abuse & Rate Limiting**: Distributed rate-limiting backed by MongoDB with configurable edge proxy awareness.
- 📊 **Observability & Docs**: OpenAPI/Swagger UI, Sentry integration, and Actuator health endpoints.

---

## Architecture

```text
┌────────────────────────────────────────────────────────┐
│                   Client Applications                  │
│       (Android, iOS, Desktop, Web / KMP & CMP)         │
└───────────────────────────┬────────────────────────────┘
                            │ (OAuth 2.1 / PKCE / Ktor)
                            ▼
┌────────────────────────────────────────────────────────┐
│            komposeauth Server (Spring Boot)            │
│  - OAuth2 / OIDC Engine       - KYC & File Encryption  │
│  - Admin Console (/admin)     - Rate Limiting Engine   │
└──────────────┬───────────────────────────┬─────────────┘
               │                           │
               ▼                           ▼
      ┌─────────────────┐         ┌──────────────────┐
      │  MongoDB Store  │         │ S3 / GCS Storage │
      └─────────────────┘         └──────────────────┘
```

---

## Quickstart

### 1. Run the Authorization Server (Docker)

Generate an encryption key:
```bash
openssl rand -base64 32
```

Run the server container:
```bash
docker run -d -p 80:8080 \
  --name komposeauth \
  -e MONGODB_URI="mongodb://your-mongo-host:27017" \
  -e BASE64_ENCRYPTION_KEY="<paste-your-base64-key>" \
  -e SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="http://localhost" \
  pitampoudel/komposeauth:latest
```

> **Note**: `MONGODB_URI`, `BASE64_ENCRYPTION_KEY`, and `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` are required. The server will not start without them.

### 2. Configuration & Admin Console

Once the container is running, navigate to the web administration console:
- **URL**: `http://localhost/admin/config`
- **Initial Authentication**: Enter your `BASE64_ENCRYPTION_KEY` into the form, or provide it via the `X-Master-Key` header:

```bash
curl -H "X-Master-Key: <paste-your-base64-key>" http://localhost/admin/config
```

Use the Admin Console to configure SMTP credentials, SMS providers, Google OAuth credentials, allowed CORS origins, and rate limits. Once an admin account with the `SUPER_ADMIN` role is created, you can sign in directly.

### 3. Environment Variables

| Variable | Required | Description | Default / Example |
|---|---|---|---|
| `MONGODB_URI` | **Yes** | MongoDB connection string (database defaults to `komposeauth`). | `mongodb://localhost:27017` |
| `BASE64_ENCRYPTION_KEY` | **Yes** | 32-byte Base64 AES key for encrypting secrets, KYC documents, and initial `/admin/config` access. | Generated via `openssl rand -base64 32` |
| `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` | **Yes** | Public origin URL of the auth server (used for tokens, discovery, and callbacks). Must omit trailing slash. | `https://auth.example.com` or `http://localhost` |
| `PORT` | No | Server HTTP port. | `8080` |
| `FORWARD_HEADERS_STRATEGY` | No | Reverse proxy header strategy (`framework` or `none`). | `framework` |
| `SESSION_TIMEOUT` | No | Admin session timeout duration. | `24h` |
| `SHUTDOWN_GRACE_PERIOD` | No | Graceful shutdown timeout. | `8s` |

#### Issuer Configuration
Set `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` to the public origin URL of your auth server (e.g. `https://auth.example.com`).
- Do not include trailing slashes or subpaths.
- If using Google Sign-In, register `<ISSUER_URL>/login/oauth2/code/google` as the authorized redirect URI in Google Cloud Console.

#### Reverse Proxy & Rate Limiting Configuration
Under **Rate Limiting** in `/admin/config`, configure client IP resolution depending on your deployment:

- **Header-based platforms** (e.g. Railway, Fly.io, Cloudflare): Set `clientIpHeader` to `X-Real-IP`, `Fly-Client-IP`, or `CF-Connecting-IP`.
- **`X-Forwarded-For` platforms**: Set `trustedProxyCount` to the number of reverse proxy hops you control (e.g., `1` for Google Cloud Run, `2` when behind an external load balancer).
- **Direct exposure**: Leave unset and pass `FORWARD_HEADERS_STRATEGY=none`.

Verify your detected IP address at any time using:
```bash
curl https://your-auth-server/admin/client-ip -H "Cookie: <your-session-cookie>"
```

### 4. Deployment Guides

#### Google Cloud Run

```bash
gcloud run deploy komposeauth \
  --image pitampoudel/komposeauth:latest \
  --set-env-vars MONGODB_URI="mongodb+srv://...",BASE64_ENCRYPTION_KEY="<your-base64-key>",SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="https://auth.example.com" \
  --min-instances 0 \
  --concurrency 40 \
  --cpu 1 --memory 1Gi \
  --cpu-boost \
  --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8080,initialDelaySeconds=4,periodSeconds=2,timeoutSeconds=2,failureThreshold=45
```

---

## Client SDK Setup

### Installation

Add the dependencies to your Kotlin Multiplatform `commonMain` source set:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Client SDK (includes UI components, ViewModels, and Ktor plugins)
            implementation("io.github.pitampoudel:komposeauth-client:<latest-version>")
            
            // Optional: Shared DTOs and validators (already transitive in client)
            implementation("io.github.pitampoudel:komposeauth-shared:<latest-version>")
        }
    }
}
```

### HTTP Client Configuration

Install the `KomposeAuth` plugin on your Ktor `HttpClient`:

```kotlin
val httpClient = HttpClient {
    installKomposeAuth(
        authServerUrl = "https://auth.example.com",
        resourceServerUrls = listOf(
            "https://api.example.com"
        )
    )
}

// Initialize the SDK with the configured client
initializeKomposeAuth(
    httpClient = httpClient
)
```

> **Platform Notes**:
> - **Android**: Pass the Android `Context` to store tokens encrypted with Android Keystore.
> - **Desktop**: Pass `appId` (e.g. your application ID) to isolate token storage in Java Preferences.

### Usage Examples

#### 1. Track Authenticated User State

```kotlin
@Composable
fun UserProfileScreen() {
    val userState by rememberAuthenticatedUser()
    
    userState?.let { user ->
        Text("Welcome, ${user.displayName ?: user.email}")
    } ?: Text("Not logged in")
}
```

#### 2. Passkey & Credential Manager Login

```kotlin
@Composable
fun LoginScreen(vm: LoginViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val credentialManager = rememberKmpCredentialManager()

    LaunchedEffect(state.loginConfig) {
        state.loginConfig?.let { config ->
            when (val result = credentialManager.getCredential(config)) {
                is Result.Success -> vm.onEvent(LoginEvent.Login(result.data))
                is Result.Error -> vm.onEvent(LoginEvent.ShowInfoMsg(result.message))
            }
        }
    }
}
```

#### 3. SMS OTP Retriever

```kotlin
@Composable
fun OtpVerificationScreen(vm: OtpViewModel = koinViewModel()) {
    registerSmsOtpRetriever { otpCode ->
        vm.onEvent(OtpEvent.CodeChanged(otpCode))
    }
}
```

#### 4. UI Components & Utilities
The SDK includes ready-to-use Compose Multiplatform UI components and utilities:
- `ScreenStateWrapper` with error/info and loading progress dialogs
- `CountryPicker`, `DateTimeField`, and `OTPTextField`
- `rememberFilePicker` for image/document selection
- String resource mapping helpers for validation errors: `(error).toStringRes()`

---

## Integration Guide (LLMs & Backends)

A comprehensive integration guide for developers and LLM coding assistants covers frontend OAuth 2.1 PKCE flows, JWT claims structure, and backend token validation across multiple platforms (Spring Boot, Node.js, Python, Go, and Ktor):

- [`llms.txt`](llms.txt) — High-level quick reference index ([llms.txt standard](https://llmstxt.org))
- [`docs/llms-full.txt`](docs/llms-full.txt) — Complete integration guide

Both files are also served directly by the server at `https://<your-auth-server>/llms.txt` and `https://<your-auth-server>/llms-full.txt`.

---

## Versioning

The project follows [Semantic Versioning](https://semver.org/) (`vMAJOR.MINOR.PATCH`).

Version resolution works uniformly across both local builds and CI:
1. **Gradle Property**: `-Pversion=X.Y.Z` (explicit override)
2. **CI Tag / Ref**: Environment variable `GITHUB_REF_NAME` (e.g. `v0.2.8` → `0.2.8`)
3. **Git Tag**: Automatically extracted from `git describe --tags` (e.g. `v0.2.8` → `0.2.8`)
4. **Development Default**: `1.0.0-SNAPSHOT`

---

## Building & Publishing

### 1. Server Docker Image

```bash
# Run tests
./gradlew :server:check

# Build Docker image locally
./gradlew :server:bootBuildImage

# Build and tag custom image
./gradlew :server:bootBuildImage --imageName=docker.io/pitampoudel/komposeauth:0.2.8

# Push to Docker Hub
docker push docker.io/pitampoudel/komposeauth:0.2.8
docker tag docker.io/pitampoudel/komposeauth:0.2.8 docker.io/pitampoudel/komposeauth:latest
docker push docker.io/pitampoudel/komposeauth:latest
```

### 2. Client SDK (KMP / CMP)

#### Publish Locally (`mavenLocal`)
```bash
./gradlew :shared:publishToMavenLocal :client:publishToMavenLocal
```

#### Publish to GitHub Packages
```bash
export GITHUB_ACTOR="pitampoudel"
export GITHUB_TOKEN="<your-github-token>"

./gradlew :client:publishAllPublicationsToGitHubPackagesRepository
```

#### Publish to Maven Central
```bash
# Publish snapshot
./gradlew :client:publishAllPublicationsToMavenCentralRepository

# Publish and release tagged release (e.g., v0.2.8)
./gradlew :client:publishAndReleaseToMavenCentral --no-configuration-cache
```

---

## Security

### CSRF & Cross-Site Request Protection
The authorization server enforces browser origin security using the `Sec-Fetch-Site` header:
- **Same-origin requests** are always allowed.
- **Cross-site requests** are restricted to origins explicitly configured under **CORS allowed origins** in `/admin/config`.
- **Non-browser API clients** (mobile apps, KMP SDK, backend servers) authenticate with bearer tokens and are unaffected.

### Encrypted KYC Documents
- Profile pictures are publicly accessible if configured, but sensitive KYC identity scans and selfies are encrypted using `BASE64_ENCRYPTION_KEY` before upload to cloud storage.
- KYC documents are served exclusively via authenticated endpoints (`GET /kyc/{userId}/documents/{front|back|selfie}`) requiring a user bearer token or administrative session.

### Security Disclosures
If you discover a security vulnerability, please email the maintainers or open a private security advisory instead of creating a public issue.

---

## Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository and create your branch (`feature/my-feature`).
2. Run test suites locally before submitting: `./gradlew check`.
3. Open a Pull Request describing your changes.

For major architectural changes, please open an issue first to discuss the proposal.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
