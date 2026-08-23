# komposeauth

Full-stack auth for Kotlin Multiplatform: Spring Auth Server + KMP SDK + Client SDK

[![Maven Central (shared)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-shared.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-shared)
[![Maven Central (client)](https://img.shields.io/maven-central/v/io.github.pitampoudel/komposeauth-client.svg)](https://central.sonatype.com/artifact/io.github.pitampoudel/komposeauth-client)
[![Docker](https://img.shields.io/badge/GHCR-komposeauth-blue?logo=docker)](https://ghcr.io/pitampoudel/komposeauth)
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
  -e MONGODB_URI="mongodb://your-mongo-host:27017/db-name" \
  -e BASE64_ENCRYPTION_KEY="<paste-your-base64-key>" \
  pitampoudel/komposeauth:latest
```

- After the container is running, open the configuration page to set up everything else:
  - http://localhost/admin/config?key=&lt;paste-your-base64-key&gt;

  The `key` is the same `BASE64_ENCRYPTION_KEY` you started the container with. It is needed because
  no account exists yet and this page reads and writes every secret the server holds — SMTP
  password, SMS provider token, OAuth client secrets — so it is never open to an unauthenticated
  visitor, not even on a fresh install. Once you have created an account and given it the `ADMIN`
  role, signing in is enough and the key is no longer required.

  To keep the key out of your browser history and any proxy logs, you can send it as a header
  instead:

  ```bash
  curl -H "X-Master-Key: <paste-your-base64-key>" http://localhost/admin/config
  ```

#### Tell the server how it is reached

The abuse limits count per client address, and the server can only work out which address that is if
it knows what stands between it and the internet. `X-Forwarded-For` is written by the caller as much
as by any proxy, so entries are trustworthy only from the right-hand end inwards — and only as far
in as the proxies you actually run. `TRUSTED_PROXY_COUNT` is how many that is.

Hosting platforms do this in one of two ways, and they need opposite settings.

**Some edges publish the client address under a header of their own.** Name it and it's used as-is —
no counting, nothing of the caller's mixed in. Prefer this wherever it's offered:

| Platform | Setting |
|---|---|
| Railway | `CLIENT_IP_HEADER=X-Real-IP` |
| Fly.io | `CLIENT_IP_HEADER=Fly-Client-IP` |
| Behind Cloudflare | `CLIENT_IP_HEADER=CF-Connecting-IP` |

**Other edges append to `X-Forwarded-For`**, leaving whatever the caller sent to the left of their
own entries. There, count hops in from the right:

| Deployment | Setting |
|---|---|
| Google Cloud Run, at its own `run.app` URL | `TRUSTED_PROXY_COUNT=1` |
| Behind a GCP external Application Load Balancer | `TRUSTED_PROXY_COUNT=2` |
| Your own nginx / Caddy in front | `TRUSTED_PROXY_COUNT=1`, plus one per extra hop |
| Exposed directly, as in the quickstart above | leave both unset, and set `FORWARD_HEADERS_STRATEGY=none` |

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
the public address is unmistakably yours — and set `CLIENT_IP_HEADER` to whichever header came back
holding it. If instead `X-Forwarded-For` *ends* with your address, count its position from the right
and use `TRUSTED_PROXY_COUNT`.

The last row is the only one that should turn off `FORWARD_HEADERS_STRATEGY`. Everywhere else it must
stay at its default of `framework`, because that is what tells the server it was reached over HTTPS —
without it, session cookies lose `Secure`, cross-site sign-in stops working, and verification emails
carry `http://` links.

##### Cloud Run

```bash
gcloud run deploy komposeauth \
  --image pitampoudel/komposeauth:latest \
  --set-env-vars MONGODB_URI="mongodb+srv://...",BASE64_ENCRYPTION_KEY="<your-base64-key>",TRUSTED_PROXY_COUNT=1 \
  --min-instances 0 \
  --concurrency 40 \
  --cpu 1 --memory 1Gi \
  --cpu-boost \
  --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8080,initialDelaySeconds=4,periodSeconds=2,timeoutSeconds=2,failureThreshold=45
```

`scripts/deploy-cloud-run.sh` does all of this from `deploy-targets.json`, which is worth using once
you have more than one target.

`TRUSTED_PROXY_COUNT=1` is what Cloud Run needs: its front end appends the caller's address as the
last `X-Forwarded-For` entry, which is the one this server reads, and sets `X-Forwarded-Proto: https`
for the default `framework` strategy to pick up. Use `2` instead if you front the service with an
external Application Load Balancer, which appends both the client address and its own forwarding
rule.

Scaling to several instances is already accounted for — sessions, OAuth2 authorizations and the
abuse counters all live in MongoDB rather than in one container's memory, so limits hold across
instances and survive cold starts.

###### Scaling to zero

`--min-instances 0` costs nothing while nobody is signing in, at the price of a cold start on the
next request. The rest of the flags above are there to keep that cold start short, and two of them
carry most of the weight:

- **`--cpu-boost`** grants extra CPU until the container reports ready. That window is exactly what a
  JVM spends classloading and refreshing a Spring context, and it is the largest single improvement
  available here.
- **`--startup-probe`** on `/actuator/health/readiness` replaces the default, which is a TCP check
  against the port. Tomcat binds the port partway through startup, well before the schema migrations
  and the app-config warm-up have run, so the default check reports ready while the instance still
  cannot answer — and the first request of every cold start then queues behind the rest of startup.
  `/actuator/health/readiness` turns green on `ApplicationReadyEvent`, which is after both.

The health endpoint is the one part of Actuator reachable without signing in, because a probe has no
credentials to offer. It reports a bare `{"status":"UP"}`: no component breakdown, and nothing else
under `/actuator` is exposed.

`--concurrency` should stay at or below `TOMCAT_MAX_THREADS` (40 by default). Set it higher and
requests queue inside the container, where Cloud Run cannot see them and so will not scale out.

Two things happen inside the image for the same reason. Indexes are created once per database by a
recorded migration rather than re-asserted on every context refresh, which is what
`spring.data.mongodb.auto-index-creation` would do — about thirty round trips to your cluster on each
cold start. And the MongoDB connection pool opens `app.mongo.min-pool-size` sockets during startup,
while the CPU boost is still in effect, so the first request does not pay for a TLS handshake and
authentication against a cluster in another region.

If you deploy somewhere without an equivalent of `--cpu-boost`, or your cluster is far from your
region, measure before assuming zero is right. `minInstances: 1` in `deploy-targets.json` is the
escape hatch, and it is the only one of these settings that costs money.

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

### CSRF, and when you need to think about it

State-changing requests must carry a CSRF token whenever they authenticate with a cookie, because a
cookie is sent by the browser whether or not the page asking for it is yours.

Most callers never notice:

- **The KMP SDK, and anything else sending `Authorization: Bearer`** — exempt. A browser will not
  attach that header to a cross-site request on its own, so there is nothing to forge.
- **Pages this server renders**, including the admin console — the token is already in the form or
  the page's `<meta name="_csrf">`.
- **Signing in and resetting a password** — exempt, so a client can do these before it holds a token.

You need to do something in one case: **a browser app on your own origin that authenticates with the
access-token cookie.** Fetch a token once, then echo it back on every write:

```js
const { token, headerName } = await (
  await fetch("https://your-auth-server/csrf", { credentials: "include" })
).json();

await fetch("https://your-auth-server/update-profile", {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json", [headerName]: token },
  body: JSON.stringify({ givenName: "Ada" }),
});
```

Your app's origin must be listed under **CORS allowed origins** on the configuration page, or the
browser will not let it read the token. Apps served from a subdomain of your configured relying party
ID can also read the `XSRF-TOKEN` cookie directly and skip the fetch.

### Reporting

If you discover a security vulnerability, please email the maintainers or open a private security
advisory. Avoid filing public issues with sensitive details.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
