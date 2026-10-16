# komposeauth

Full-stack authentication and authorization solution for Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP), featuring a Spring Boot Authorization Server and multiplatform client SDKs.

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-komposeauth-blue?logo=docker)](https://hub.docker.com/r/pitampoudel/komposeauth)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/pitampoudel/komposeauth?style=social)](https://github.com/pitampoudel/komposeauth/stargazers)

**Your own auth server and a Compose Multiplatform login SDK, in one repo.** Run one Docker image,
add one Gradle dependency, and your Android, iOS, desktop and web apps sign users in with passwords,
passkeys, Google, Apple or one-time codes, against a standard OAuth 2.1 / OpenID Connect server you
host and own. No per-user pricing, and your users' data stays in your MongoDB.

```bash
git clone https://github.com/pitampoudel/komposeauth && cd komposeauth
cp .env.example .env && echo "BASE64_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
docker compose up -d   # then open http://localhost:8080/admin/config
```

```kotlin
// commonMain
implementation("io.github.pitampoudel:komposeauth-client:<latest-version>")

val httpClient = HttpClient {
    installKomposeAuth(
        authServerUrl = "https://auth.example.com",
        resourceServerUrls = listOf("https://api.example.com") // your APIs get the bearer token
    )
}
```

If komposeauth is useful to you, a ⭐ helps other Kotlin developers find it.

## Why komposeauth?

Most auth options for a Kotlin Multiplatform app make you pick between a hosted service that bills
per active user and a generic identity server you have to wire into Compose yourself. komposeauth
gives you both halves, built for each other:

| | komposeauth | Hosted auth (Firebase Auth, Auth0, Clerk) | Keycloak and similar |
|---|---|---|---|
| Who hosts it | You, from one Docker image | The vendor | You |
| Cost as you grow | Your server and database | Grows with monthly active users on paid tiers | Your server and database |
| Where user data lives | Your MongoDB | The vendor's cloud | Your database |
| KMP / Compose Multiplatform SDK | Ktor plugin, ViewModels and Compose UI for Android, iOS, desktop and web | Mostly per-platform SDKs, or community wrappers | None; you write the client side |
| Standards | OAuth 2.1, OpenID Connect, PKCE, JWKS | Proprietary APIs plus OIDC | OAuth 2.0, OpenID Connect, SAML |
| Any backend can verify tokens | Yes: RS256 JWTs checked against `/oauth2/jwks` | Yes | Yes |

Pick komposeauth when you are building a Kotlin Multiplatform or Compose Multiplatform app, want to
own your users, and would rather not glue a generic identity server to your client by hand. Pick
something else if you need SAML, enterprise federation, or a fully managed service you never operate.

---

## Table of Contents

- [Why komposeauth?](#why-komposeauth)
- [Overview & Architecture](#overview--architecture)
- [Features](#features)
- [Quickstart (Docker)](#quickstart-docker)
- [Server Configuration & Deployments](#server-configuration--deployments)
  - [Admin Console & Connections](#admin-console--connections)
  - [File & Asset Storage](#file--asset-storage)
  - [Environment Variables](#environment-variables)
  - [Production Deployments](#production-deployments)
- [Client SDK Setup](#client-sdk-setup)
  - [Installation](#installation)
  - [HTTP Client Configuration](#http-client-configuration)
  - [Usage Examples](#usage-examples)
- [Integration Guide (LLMs & Backends)](#integration-guide-llms--backends)
- [Security & Architecture Details](#security--architecture-details)
- [Building, Publishing & Versioning](#building-publishing--versioning)
- [Contributing & License](#contributing--license)

---

## Overview & Architecture

`komposeauth` provides a complete, modern authentication ecosystem across server and client platforms:

- **Auth Server**: Spring Boot OAuth 2.1 & OpenID Connect Authorization Server with administrative management, rate limiting, and encrypted KYC storage.
- **Shared SDK (`komposeauth-shared`)**: Cross-platform Kotlin Multiplatform data models, DTOs, and validation logic.
- **Client SDK (`komposeauth-client`)**: Compose Multiplatform UI components, Ktor authentication plugins, Credential Manager integrations, and ViewModels for Android, iOS, Desktop, and Web.

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

## Features

- 🔐 **Authentication Flows**: Email or phone with password, Passkeys (WebAuthn / FIDO2), Sign in with Google and Apple, and one-time codes by SMS, WhatsApp or email.
- 🤖 **Works with MCP clients**: Client ID Metadata Documents (CIMD) let MCP clients such as Claude sign in to your APIs without manual registration.
- 🛡️ **OAuth 2.1 & OpenID Connect**: PKCE (S256), discovery (`/.well-known/openid-configuration`), and RS256 JWT token issuance validated via JWKS.
- 📱 **Multiplatform Client**: Compose Multiplatform UI components, Credential Manager, and automatic Ktor token interceptors.
- ✉️ **Verification & Recovery**: Email verification codes and SMS OTP retriever support.
- 🪪 **KYC Document Management**: Client-side encryption before S3/GCS upload and restricted retrieval endpoints.
- ⚙️ **Admin Console**: Web UI (`/admin/config`) for runtime configuration of OAuth clients, SMTP, SMS providers, CORS, and rate limits.
- 🚦 **Abuse & Rate Limiting**: Distributed rate-limiting backed by MongoDB with configurable edge proxy awareness.
- 📊 **Observability & Docs**: OpenAPI/Swagger UI, Sentry integration, and Actuator health endpoints.

---

## Quickstart (Docker)

The fastest way to get started is using Docker Compose, which starts the server alongside a MongoDB instance:

```bash
git clone https://github.com/pitampoudel/komposeauth && cd komposeauth
cp .env.example .env
echo "BASE64_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
docker compose up -d
```

Open `http://localhost:8080/admin/config`, enter the `BASE64_ENCRYPTION_KEY` from `.env`, and create the initial super admin account. Keep this key safe: it encrypts every secret the server stores.

To run the standalone Docker image against an existing MongoDB instance:
```bash
docker run -d -p 8080:8080 \
  --name komposeauth \
  -e MONGODB_URI="mongodb://your-mongo-host:27017" \
  -e BASE64_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
  -e SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="http://localhost:8080" \
  pitampoudel/komposeauth:latest
```

---

## Server Configuration & Deployments

### Admin Console & Connections

Once the container is running, navigate to the web administration console:
- **URL**: `http://localhost:8080/admin/config`
- **Initial Authentication**: Enter your `BASE64_ENCRYPTION_KEY` into the form, or provide it via the `X-Master-Key` header:

```bash
curl -H "X-Master-Key: <paste-your-base64-key>" http://localhost:8080/admin/config
```

When no super admin exists yet (`SUPER_ADMIN`), the page opens with **Create the first admin**: enter an email and password (or the address of an account that already exists) to grant that account super admin privileges. Afterwards, sign in at `/admin` using your admin credentials instead of the master key. The initial setup form disappears once a super admin exists, and additional roles can be assigned from the **Users** management page.

Use the Admin Console to configure email (SMTP), SMS providers (Twilio, Infobip, WhatsApp), Google and Apple sign-in, error reporting and chat alert webhooks, custom branding, CORS origins, and rate limits. All third-party integrations are optional.

### File & Asset Storage

Profile photos, organization logos, and KYC documents are stored in buckets configured under **Storage** (`/admin/storage`) in the admin console. Any admin can view storage settings; only a `SUPER_ADMIN` can add or modify storage buckets. Note that asset uploads require an active storage bucket.

Supported storage backends include Google Cloud Storage (GCS) and any S3-compatible provider (Amazon S3, Cloudflare R2, MinIO, Backblaze B2, etc.):
- For non-AWS S3 providers, specify the custom endpoint URL. Path-style addressing (`{endpoint}/{bucket}/{name}`) is supported.
- Set a custom CDN or domain in front of the bucket as its public URL if desired.
- Leave credentials blank to use runtime ambient IAM credentials (e.g., Cloud Run service accounts, ECS task roles), or supply an API/service account key (stored securely with AES encryption).
- Use the **Test** button in the console to verify write, read, and delete operations.

**Public vs. Private Access**:
- Profile photos and organization logos are loaded directly by client browsers and require public read permissions on the `users/` and `organization_logos/` path prefixes.
- KYC documents remain private and are accessible only via authenticated endpoints (see [Encrypted KYC Documents](#encrypted-kyc-documents)).

**Bucket Migration**:
- Active uploads are directed to whichever bucket is designated as active ("Send new files here").
- Existing files remain accessible at their original URLs without requiring automatic server-side data migration.
- When migrating to a new bucket, use sync tools (`rclone`, `aws s3 sync`, or `gcloud storage rsync`) and link the old bucket entry using **Files copied to…** to preserve backward compatibility.

### Environment Variables

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

### Production Deployments

Every option runs the same image and keeps everything in MongoDB, so moving between them is a matter of pointing the new one at the same database with the same key and issuer. Where there is no MongoDB alongside, a [MongoDB Atlas](https://www.mongodb.com/atlas) cluster works; the free M0 is enough to start. Allow the host's outbound addresses in Atlas's network access, or `0.0.0.0/0` on hosts whose addresses change.

| Platform | Deployment Method | Scales to zero | Memory Profile |
|---|---|---|---|
| **Render** | [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/pitampoudel/komposeauth) (`render.yaml`) | Yes (Free plan) | 512 MB |
| **Railway** | [![Deploy on Railway](https://railway.com/button.svg)](https://railway.com/template/deploy?template=https://github.com/pitampoudel/komposeauth) (`railway.json`) | Sleeps on idle (Hobby) | 512 MB |
| **Fly.io** | `fly launch` / `fly deploy` (`fly.toml`) | Yes (auto stop/start) | 512 MB |
| **Google Cloud Run** | `gcloud run deploy` command below | Yes | 1 GiB |
| **Self-hosted Docker** | `docker compose up -d` ([Quickstart](#quickstart-docker)) | No | 512 MB+ |

#### Render
Deploy via the Blueprint button above or use `render.yaml`. The blueprint creates a web service from the published image and generates `BASE64_ENCRYPTION_KEY`; it asks only for `MONGODB_URI`. With no issuer set, the server uses its own `onrender.com` address. To serve it from your own domain, add the domain in Render and set `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` to it.

The blueprint sets `BPL_JVM_THREAD_COUNT=50` and `JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M` so the JVM fits in 512 MB.

#### Railway
Deploy using the Railway button above or connect your GitHub repo using `railway.json`:
1. Add a MongoDB plugin or connect an external MongoDB Atlas cluster (`MONGODB_URI`).
2. Add environment variables:
   - `BASE64_ENCRYPTION_KEY`: A 32-byte Base64 key (`openssl rand -base64 32`).
   - `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER`: `https://${{RAILWAY_PUBLIC_DOMAIN}}` or your custom domain.
3. Under `/admin/config` $\rightarrow$ **Rate Limiting**, set `clientIpHeader` to `X-Real-IP`.

#### Fly.io
Deploy to Fly.io using the included `fly.toml`:
```bash
fly launch --no-deploy
fly secrets set \
  MONGODB_URI="mongodb+srv://..." \
  BASE64_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
  SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="https://<your-app>.fly.dev"
fly deploy
```
Under `/admin/config` $\rightarrow$ **Rate Limiting**, set `clientIpHeader` to `Fly-Client-IP`.

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
Under `/admin/config` $\rightarrow$ **Rate Limiting**, set `trustedProxyCount` to `1`.

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
`latest`, and a GitHub release `vX.Y.Z` publishes `X.Y.Z` and `X.Y`.

From IntelliJ, the shared run configurations in `.run/` run the same commands:

- **Publish server image**: tests, then `bootBuildImage --publishImage` to `main` and `latest`. Sign
  in to Docker Hub once (Docker Desktop, or `docker login`).
- **Deploy to Cloud Run**: publishes, then rolls the existing `komposeauth` service onto the new
  image, keeping its environment. It deploys to your `gcloud` default project and asks for a region
  if `run/region` isn't set.

### 2. Client SDK (KMP / CMP)

CI publishes the SDK (`.github/workflows/library-publish.yml`) to GitHub Packages and Maven Central.
From IntelliJ, **publishToMavenLocal** installs it into `~/.m2` for a local app, and **Release SDK
to Maven Central** tests and releases the version of the tag you are on (`v0.2.8` → `0.2.8`), with
`mavenCentralUsername`, `mavenCentralPassword` and the `signingInMemoryKey*` properties in
`~/.gradle/gradle.properties`.

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
