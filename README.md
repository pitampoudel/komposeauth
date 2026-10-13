# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Client SDK

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
[![Docker](https://img.shields.io/badge/Docker%20Hub-komposeauth-blue?logo=docker)](https://hub.docker.com/r/pitampoudel/komposeauth)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-42a5f5)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## Overview

- Server: Spring Boot Authorization Application
- Shared KMP SDK: Shared DTOs and utilities to be used by client and server
- Client CMP SDK: Ktor, ViewModels, platform utilities, and reusable UI components

## Features

- Federated authorization with Google
- username/password
- passkey
- email verification
- Phone OTP
- KYC
- Sentry
- Swagger/OpenAPI

## Quickstart

### 1) Run the Server (Docker)

```bash
// BASE64_ENCRYPTION_KEY generator
openssl rand -base64 32
```

```bash
docker pull pitampoudel/komposeauth:latest
# Quick start
docker run -p 80:8080 \
  -e MONGODB_URI="mongodb://your-mongo-host:27017" \
  -e BASE64_ENCRYPTION_KEY="<paste-your-base64-key>" \
  -e SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="http://localhost" \
  pitampoudel/komposeauth:latest
```

All three are required; the server refuses to start without any of them. The issuer is the address
you reach the server at, `http://localhost` here — see
[Tell the server its public address](#tell-the-server-its-public-address).

- The data lives in a database named `komposeauth`, fixed in `application.yml`, so `MONGODB_URI`
  does not name one.
- After the container is running, open the configuration page to set up everything else:
    - http://localhost/admin/config

  and enter the master key: the same `BASE64_ENCRYPTION_KEY` you started the container with. It is
  needed because no account exists yet and this page reads and writes every secret the server holds —
  SMTP password, SMS provider token, OAuth client secrets — so it is never open to an unauthenticated
  visitor, not even on a fresh install. Once you have created an account and given it the `SUPER_ADMIN`
  role, signing in is enough and the key is no longer required.

  The key is read from the page's own form or from a header, never from the address, where it would
  end up in browser history and proxy logs:

  ```bash
  curl -H "X-Master-Key: <paste-your-base64-key>" http://localhost/admin/config
  ```

#### Tell the server its public address

```bash
-e SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER="https://auth.example.com"
```

Required. This is the issuer your apps validate (`iss`, and the discovery document's `issuer`), and it
is also the address every emailed link (password reset, email verification), the KYC callback and the
Google sign-in's redirect URI point at. Write it exactly as your apps check it: `https://` (or
`http://` for local development), the host, a port if it is not the default, and nothing after that —
no trailing slash, path, query or fragment. The server refuses to start without it, or with anything
else, and never falls back to a request's `Host` and `X-Forwarded-*` headers, which the caller writes:
a reset link built from them could be made to point anywhere.

If you sign in with Google, the authorized redirect URI to register in the Google console is this
address followed by `/login/oauth2/code/google`.

#### Tell the server how it is reached

The abuse limits count per client address (the code endpoints count a signed-in caller by account
instead), and the server can only work out which address that is if it knows what stands between it
and the internet. `X-Forwarded-For` is written by the caller as much as by any proxy, so entries are
trustworthy only from the right-hand end inwards — and only as far in as the proxies you actually
run. `trustedProxyCount` is how many that is.

Both settings below are fields under **Rate Limiting** on the `/admin/config` page, not environment
variables, so they can be corrected on a running server without a redeploy.

Hosting platforms do this in one of two ways, and they need opposite settings.

**Some edges publish the client address under a header of their own.** Name it and it's used as-is —
no counting, nothing of the caller's mixed in. Prefer this wherever it's offered:

| Platform          | `clientIpHeader`   |
|-------------------|--------------------|
| Railway           | `X-Real-IP`        |
| Fly.io            | `Fly-Client-IP`    |
| Behind Cloudflare | `CF-Connecting-IP` |

**Other edges append to `X-Forwarded-For`**, leaving whatever the caller sent to the left of their
own entries. There, count hops in from the right:

| Deployment                                      | `trustedProxyCount`                                                   |
|-------------------------------------------------|-----------------------------------------------------------------------|
| Google Cloud Run, at its own `run.app` URL      | `1`                                                                   |
| Behind a GCP external Application Load Balancer | `2`                                                                   |
| Your own nginx / Caddy in front                 | `1`, plus one per extra hop                                           |
| Exposed directly, as in the quickstart above    | leave both unset, and set the env var `FORWARD_HEADERS_STRATEGY=none` |

Count only proxies you control. Guessing **too high** is the safe direction — the server falls back
to the connection's own peer address. Guessing **too low** attributes every request to your proxy, so
one shared budget covers all your users and the limits refuse them together.

Getting this wrong is not cosmetic: trust a header the edge does *not* overwrite and callers simply
nominate who gets counted, so the limits stop working while still appearing to be on. The server logs
a warning naming the relevant setting when it can tell something is off, but it cannot detect every
case.

##### Checking it, rather than trusting the table

Providers change, they disagree with their own documentation, and putting a CDN in front changes the
answer again. Once deployed, sign in as an admin and call:

```bash
curl https://your-auth-server/admin/client-ip -H "Cookie: <your session>"
```

It reports the address the limits are currently counting you as, how that was decided, and every
client-address header the request actually carried. Call it from a phone on mobile data — somewhere
the public address is unmistakably yours — and set `clientIpHeader` to whichever header came back
holding it. If instead `X-Forwarded-For` *ends* with your address, count its position from the right
and use `trustedProxyCount`.

The last row is the only one that should turn off `FORWARD_HEADERS_STRATEGY`. Everywhere else it must
stay at its default of `framework`, because that is what tells the server it was reached over HTTPS —
without it, session cookies lose `Secure` and cross-site sign-in stops working.

##### Cloud Run

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

Then set `trustedProxyCount` to `1` on the config page, which is what Cloud Run needs: its front end
appends the caller's address as the last `X-Forwarded-For` entry, which is the one this server reads,
and sets `X-Forwarded-Proto: https` for the default `framework` strategy to pick up. Use `2` instead
if you front the service with an external Application Load Balancer, which appends both the client
address and its own forwarding rule.

Scaling to several instances is already accounted for — sessions, OAuth2 authorizations and the
abuse counters all live in MongoDB rather than in one container's memory, so limits hold across
instances and survive cold starts.

### 2) Add the SDK to your KMP project

Shared module (optional and also included already on client module)

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

On Android the overload takes a `Context` too, and the tokens are encrypted with an Android Keystore key. On
desktop, also pass `appId` (your package name, say) so that your app's sign-in is kept apart from other apps';
desktop tokens are stored unencrypted in the user's Java preferences.

Initialize SDK

```kotlin
initializeKomposeAuth(
    httpClient = httpClient
)
```

## Integrating with your apps (guide for LLMs and developers)

A complete integration guide, written so an LLM coding assistant can follow it, covers frontend sign-in (OAuth 2.1
authorization code + PKCE for SPAs, server-rendered and mobile apps, or the first-party `/login` API), the token
claims, and resource-server token validation for Spring Boot, Node.js, Python, Go and Ktor.

- [`llms.txt`](llms.txt): short index in the [llms.txt](https://llmstxt.org) format
- [`docs/llms-full.txt`](docs/llms-full.txt): the full guide

Every running server also publishes both, without authentication, at `https://your-auth-server/llms.txt` and
`https://your-auth-server/llms-full.txt`. Point your assistant at one of those URLs.

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
val userState = rememberAuthenticatedUser()
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

## Building & Publishing Docker Image

To build and publish the server image directly to Docker Hub:

```bash
# 1. Run tests (optional but recommended)
./gradlew :server:check

# 2. Build image (automatically tags with the latest Git release tag or 'latest')
./gradlew :server:bootBuildImage

# 3. Push image to Docker Hub
TAG=$(git describe --tags --abbrev=0 | sed 's/^[vV]//')
# Make latest
docker tag "docker.io/pitampoudel/komposeauth:$TAG" docker.io/pitampoudel/komposeauth:latest
docker push docker.io/pitampoudel/komposeauth:$TAG
```

## Contributing

- Issues and PRs are welcome
- Please run `./gradlew build` before submitting a PR
- For larger changes, consider opening an issue first to discuss direction

## Security

### Cross-site requests

There are no CSRF tokens. Instead, the browser's `Sec-Fetch-Site` header decides:

- **Same-origin requests** (this server's own pages, including the admin console) are always
  allowed, whatever proxy sits in front of the server.
- **Cross-site requests** (anything a browser sends from another site, including a plain form post)
  are allowed only from origins listed under **CORS allowed origins** on the configuration page.
  With the list empty, no other site can send a cookie-authenticated write to this server.
- **Requests from outside a browser** (the KMP SDK, mobile apps, backends) do not carry the header
  and are unaffected. Anything sending `Authorization: Bearer` is not forgeable anyway.

So a browser app on another origin that calls this server with `credentials: "include"` only needs
its origin in that list.

### KYC documents

Profile photos and KYC uploads go to the same bucket, and a profile photo is meant to be readable by
anyone with its address. KYC documents (the document scans and the selfie) are not: they are
encrypted with `BASE64_ENCRYPTION_KEY` before they are uploaded, so the stored copy is unreadable,
and a KYC response's `documentFrontUrl`, `documentBackUrl` and `selfieUrl` point at this server
(`GET /kyc/{userId}/documents/{front|back|selfie}`), which serves them only to that user and to
admins. Fetch them with the user's bearer token or session cookie, not as a bare `<img src>` from
another site. Files uploaded by an earlier version are encrypted where they are, in the background,
once a server with this version starts.

Keep the key: a server started with a different one cannot open the documents already stored. And
if your bucket keeps old object versions (GCS object versioning, S3 versioning), delete the old
versions under `kyc/`, which are the unencrypted originals.

### Reporting

If you discover a security vulnerability, please email the maintainers or open a private security
advisory. Avoid filing public issues with sensitive details.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
