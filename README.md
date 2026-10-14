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

The fastest way is Docker Compose, which starts the server and a MongoDB beside it:

```bash
git clone https://github.com/pitampoudel/komposeauth && cd komposeauth
cp .env.example .env
echo "BASE64_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
docker compose up -d
```

Open `http://localhost:8080/admin/config`, enter the key from `.env`, and create the first admin. Keep that key safe: it decrypts every secret the server stores.

To run the image on its own against a MongoDB you already have:
```bash
docker run -d -p 8080:8080 \
  --name komposeauth \
  -e MONGODB_URI="mongodb://your-mongo-host:27017" \
  -e BASE64_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
  -e SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="http://localhost:8080" \
  pitampoudel/komposeauth:latest
```

To deploy it somewhere reachable, see [Deployment Guides](#4-deployment-guides).

> **Note**: `MONGODB_URI`, `BASE64_ENCRYPTION_KEY`, and `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` are required. The server will not start without them.

### 2. Configuration & Admin Console

Once the container is running, navigate to the web administration console:
- **URL**: `http://localhost:8080/admin/config`
- **Initial Authentication**: Enter your `BASE64_ENCRYPTION_KEY` into the form, or provide it via the `X-Master-Key` header:

```bash
curl -H "X-Master-Key: <paste-your-base64-key>" http://localhost:8080/admin/config
```

While nobody holds `SUPER_ADMIN`, the page opens with **Create the first admin**: enter an email and password (or the address of an account that already exists) and that account becomes a super admin. From then on, sign in at `/admin` with it instead of the master key; the form disappears once a super admin exists, and further roles are granted from the **Users** page.

Use the Admin Console to connect email, SMS, Google and Apple sign-in, error reporting and alerts, and to set branding, CORS origins and rate limits. Every connection is optional.

#### Where files are kept

Profile photos, organization logos and KYC documents are kept in a bucket listed on the admin
console's **Storage** page (`/admin/storage`). Any admin can see it; a `SUPER_ADMIN` adds and
changes buckets. Until one is listed, uploads fail, so add one once you have signed in as a
`SUPER_ADMIN`.

A bucket is a Google Cloud Storage one or one on anything that speaks the S3 API: Amazon S3,
Cloudflare R2, MinIO, Backblaze B2 and so on. For anything but AWS, give its endpoint; those buckets
are addressed path-style (`{endpoint}/{bucket}/{name}`). Put a CDN or a domain of your own in front
as the bucket's public address and new files are handed out under it. Leave the credentials empty to
use the server's own identity (a Cloud Run service account, an ECS task role), or paste an access key
or a service account key, which is stored encrypted like the configuration's secrets. **Test** writes,
reads and deletes a small file, which is everything the server ever does in a bucket.

Profile photos and organization logos are loaded by browsers straight from the bucket, so it has to
let anyone read the `users/` and `organization_logos/` prefixes. KYC documents stay private (see
[Encrypted KYC Documents](#encrypted-kyc-documents)).

One bucket takes new files. Every other file stays where it was written and opens from the address
saved for it, so choosing another bucket for new files moves nothing already stored. The server
copies no file itself; moving to another bucket, in the same cloud or another one, is:

1. Add the new bucket on the Storage page and test it.
2. Press **Send new files here** on it, and wait a minute for every server to switch.
3. Copy everything from the old bucket into the new one under the same names: `rclone copy` between
   any two providers, or `gcloud storage rsync` / `aws s3 sync` within one. Run it once more after
   that minute, so nothing written meanwhile is left behind.
4. On the old bucket, press **Files copied to…** and choose the new one. From then on the old
   bucket's saved addresses open the copies, so the old bucket needs no credentials any more. Open a
   few old photos to check.

Keep the old entry listed: it is how the server recognises the addresses saved for its files, which
are never rewritten. **Open from its own bucket** undoes step 4. Going back is the same steps the
other way, starting with that button on the bucket you return to.

Upgrading from a release that set the buckets on the configuration page: the first start lists them
on the Storage page, with the same choice of where new files go and, after a GCS-to-S3 copy, the
same link between the two. If both a GCS and an S3 bucket were set and `storageProvider` named
neither, no bucket takes new files until you choose one there. The old fields are left in the stored
configuration so the release before can still be rolled back to, but nothing reads them.

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

Every option runs the same image and keeps everything in MongoDB, so moving between them is a matter of pointing the new one at the same database with the same key and issuer. Where there is no MongoDB alongside, a [MongoDB Atlas](https://www.mongodb.com/atlas) cluster works; the free M0 is enough to start. Allow the host's outbound addresses in Atlas's network access, or `0.0.0.0/0` on hosts whose addresses change.

| Where | How | Scales to zero |
|---|---|---|
| Google Cloud Run | the `gcloud` command below | Yes |
| Render | [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/pitampoudel/komposeauth) (`render.yaml`) | On the free plan |
| AWS ECS Fargate | [`deploy/aws`](deploy/aws/README.md), one CloudFormation stack | No |
| Any machine with Docker | `docker compose up -d` ([Quickstart](#1-run-the-authorization-server-docker)) | No |

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

#### Render

The button creates a web service from the published image and generates `BASE64_ENCRYPTION_KEY`; it asks only for `MONGODB_URI`. With no issuer set, the server uses its own `onrender.com` address. To serve it from your own domain, add the domain in Render and set `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` to it before any app starts using the server. The key Render generated is under the service's **Environment**; you need it to open the admin console the first time.

The blueprint sets `BPL_JVM_THREAD_COUNT=50` and `JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M` so the JVM fits in 512 MB. Set the same two on any other host with that little memory: the image sizes the JVM for 250 threads by default and won't start in less.

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

CI publishes the image (`.github/workflows/docker-publish.yml`): a push to `main` publishes `main` and
`latest`, and a GitHub release `vX.Y.Z` publishes `X.Y.Z` and `X.Y`. To publish again without a new
commit, run the workflow by hand (**Actions → Publish Docker Image → Run workflow**).

```bash
# Run tests
./gradlew :server:check

# Build the image into the local Docker daemon
./gradlew :server:bootBuildImage

# Build and push by hand, after `docker login` (what CI runs; -PimageTags adds tags to the same image)
./gradlew :server:bootBuildImage --publishImage \
  --imageName=docker.io/pitampoudel/komposeauth:0.2.8 \
  -PimageTags=docker.io/pitampoudel/komposeauth:0.2
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
Please report vulnerabilities privately, as described in [SECURITY.md](SECURITY.md), rather than in a public issue.

---

## Contributing

Contributions are welcome. [CONTRIBUTING.md](CONTRIBUTING.md) covers running the server locally, the tests, and what a pull request needs. For changes that affect existing deployments, please open an issue first. What changed in each release is in [CHANGELOG.md](CHANGELOG.md).

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
