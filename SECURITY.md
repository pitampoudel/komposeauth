# Security policy

komposeauth holds people's passwords, sessions and identity documents, so we treat security
reports as the most urgent thing in the queue.

## Reporting a vulnerability

Report it privately through GitHub:
**[Report a vulnerability](https://github.com/pitampoudel/komposeauth/security/advisories/new)**
(the **Security** tab, then **Report a vulnerability**). Only the maintainers can see it.

Please don't open a public issue, discussion or pull request for it until a fix is released.

A useful report says what an attacker can do, the version or commit, and the steps or a request
that shows it. You don't need a fix.

## What happens next

- We acknowledge the report within a few days.
- We confirm or rule it out, and agree a disclosure date with you.
- The fix ships in a release, and the advisory is published with credit to you unless you'd rather
  not be named.

## Supported Versions

Security updates and bug fixes are applied to the `main` branch and released in the latest version tags. We recommend running the latest official Docker image:
- Latest release: `pitampoudel/komposeauth:latest`
- Minor version pin: `pitampoudel/komposeauth:X.Y` (tracks patch updates for version `X.Y`)

## Out of scope

- Findings that need the deployment's own `BASE64_ENCRYPTION_KEY` or database access.
- Missing hardening on a deployment the operator configured differently from the README (for
  example, `FORWARD_HEADERS_STRATEGY=framework` with no proxy in front).
- Reports from automated scanners without a demonstrated impact.
