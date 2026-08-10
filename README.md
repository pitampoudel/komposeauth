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

The server needs to know whether anything stands between it and the internet, because
`X-Forwarded-For` is sent by the caller and means nothing unless a proxy you control wrote it.

**Behind a reverse proxy** (nginx, Cloudflare, a cloud load balancer) — set the number of hops:

```bash
  -e TRUSTED_PROXY_COUNT="1" \
```

That is how many proxies of your own a request passes through. Leave it at the default of `0` while
proxied and every request looks like it came from the proxy, so one shared abuse budget locks all
your users out at once — a mistake that announces itself. Count only proxies you control.

**Exposed directly**, as in the quickstart above — leave `TRUSTED_PROXY_COUNT` unset and also turn
off forwarded-header trust, which otherwise lets a caller choose the scheme and hostname the server
believes it was reached at, and so the links it puts in verification emails:

```bash
  -e FORWARD_HEADERS_STRATEGY="none" \
```

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
