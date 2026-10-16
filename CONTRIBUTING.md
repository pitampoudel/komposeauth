# Contributing to komposeauth

Thanks for helping. Bug reports, fixes, docs and new integrations are all welcome. For anything that
changes how the server behaves for existing deployments (a new required setting, a change to stored
data, a different token or endpoint), please open an issue first so we can agree on the shape before
you write it.

## What's in the repository

| Module | What it is |
|---|---|
| `server/` | The authorization server: Spring Boot, Kotlin, MongoDB, and the admin console's Thymeleaf pages under `server/src/main/resources/templates/admin/`. |
| `shared/` | Kotlin Multiplatform types used by both the server and the client SDK (requests, responses, validators). |
| `client/` | The Kotlin/Compose Multiplatform client SDK. |

The server is organised by feature (`user/`, `kyc/`, `storage/`, `app_config/`, …). A feature's
controller, service, repository, page controller and tests stay in its own package.

## Running the Server Locally

Prerequisites: **JDK 25** and **Docker**.

```bash
# 1. Start MongoDB for development
docker run -d --name komposeauth-mongo -p 27017:27017 mongo:8

# 2. Start the authorization server
export MONGODB_URI=mongodb://localhost:27017
export BASE64_ENCRYPTION_KEY="$(openssl rand -base64 32)"   # keep it while you keep the database
export SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=http://localhost:8080
./gradlew :server:bootRun
```

Open `http://localhost:8080/admin/config`, enter the `BASE64_ENCRYPTION_KEY`, and complete the first-time admin setup.

The build includes Android targets for `shared/` and `client/`. Gradle will use Google's Maven repository and, for Android compilation, requires an Android SDK (configured via `ANDROID_HOME` or standard Android Studio installation).

## Tests

```bash
./gradlew :server:test
```

The integration tests start MongoDB with Testcontainers, so Docker has to be running. A green run
has no failures and nothing skipped.

A few conventions the suite relies on:

- Test names are sentences in backticks that say what must hold, e.g.
  ``fun `an empty secret keeps the stored one`()``.
- Prefer a test through the real HTTP layer (`MockMvc`, see the `*IntegrationTest` classes) for
  anything a browser or client can reach, and a plain unit test for the rest.
- A change to the admin console should keep `TemplateRenderingTest` passing and, where it adds a
  page or a form, add a case there.

## Making a change

1. Fork the repository and branch from `main`.
2. Keep the pull request about one thing. A refactor that a fix needs is fine; unrelated clean-up
   is a separate pull request.
3. Run `./gradlew :server:test` before you push.
4. Describe what someone using the server would notice, before and after. Screenshots help for
   anything in the admin console (light and dark, wide and phone width).
5. If you add a setting, add it to the configuration page's catalogue
   (`app_config/controller/ConfigSections.kt`). `ConfigSectionsTest` fails when a stored setting
   has no place on the page or a secret isn't stored encrypted.

Comments explain why, not what. Match the code around your change.

## Security issues

Please don't open a public issue for a vulnerability. See [SECURITY.md](SECURITY.md).

## License

By contributing you agree that your work is released under the project's
[Apache 2.0 license](LICENSE).
