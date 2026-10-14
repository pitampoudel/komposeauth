# Changelog

What changed for people running the server or using the SDK. Versions follow
[Semantic Versioning](https://semver.org/). Releases up to 0.2.9 are described on the
[GitHub releases page](https://github.com/pitampoudel/komposeauth/releases).

## Unreleased

### Added
- **First-run admin.** While nobody holds `SUPER_ADMIN`, the configuration page opened with the
  master key offers *Create the first admin*, so a fresh server no longer needs a database edit to
  get into the console. (#69)
- **Docker Compose.** `docker compose up -d` runs the server with its own MongoDB; `.env.example`
  explains the settings. (#70)
- **Deploy to Render** button (`render.yaml`). With no issuer set on Render, the server uses its own
  `onrender.com` address. (#70)
- **Alerts through any chat webhook**: Slack, Discord, Mattermost, Google Chat or Rocket.Chat. A
  Slack app still works, alone or alongside. (#71)

### Changed
- **The configuration page is now Connections and Settings.** Each service (Google, Apple, email,
  text messages, error reporting, alerts, identity verification, storage) shows whether it works and
  what it still needs, opens in its own dialog with setup guidance, and has a test button where one
  makes sense. Settings are saved one section at a time, and a saved secret is never shown again;
  leaving its field empty keeps it. (#68)
- The Configuration link is shown only to super admins, who are the only ones it opens for. (#68)
- A saved Sentry DSN takes effect without a restart. (#68)

### Security
- Spring Boot 4.0.1 → 4.0.8, with Tomcat 11.0.25, Jackson 3.1.7 / 2.21.7 and OpenTelemetry 1.62.0
  ahead of it. The server's dependencies had 100 published advisories (Tomcat, Spring Framework,
  Spring Security, Jackson, Thymeleaf, Logback and others) and now have none.

### Fixed
- The overview could call text messages "On" while the provider was missing a required key; it now
  shows the same status as the configuration page. (#68)
