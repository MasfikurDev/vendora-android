# Security Policy

Vendora is an early-stage application and is not yet production-ready. This policy describes how to report potential vulnerabilities and the security expectations for contributions.

## Supported versions

Until the project publishes a stable release and documents a support window, security fixes are evaluated against the current development branch. Debug previews and prereleases are provided for testing and may contain known issues.

## Reporting a vulnerability

**Please do not disclose exploitable details in a public issue, pull request, discussion, or screenshot.**

1. Use GitHub's private vulnerability reporting feature for this repository if it is enabled.
2. If private reporting is unavailable, contact the repository owner through a private GitHub channel. Do not include credentials or real customer/shop data in the initial report.
3. Include, when safe: affected commit or release, impact, reproduction steps, prerequisites, and a suggested mitigation.

Please allow the maintainer reasonable time to investigate before public disclosure. No response-time SLA or bug-bounty program is currently promised. If a report is not acknowledged, use GitHub's private channel to follow up rather than posting sensitive details publicly.

## Security design expectations

- Never commit passwords, tokens, API keys, keystores, signing keys, or production data.
- Never embed private AI provider credentials in the Android app; client binaries can be inspected.
- Scope business data access to the correct shop and validate active-shop references.
- Validate imported files, barcode inputs, external responses, and structured AI output.
- Use database transactions for related sale and stock mutations.
- Preserve user data during migrations; do not introduce destructive migration fallbacks for convenience.
- Keep exports and backups user-controlled and explain what they contain.
- Request the least privilege needed for future staff roles and integrations.
- Review third-party dependencies and data sources for security, maintenance, license, and provenance risks.

Security reports are handled on a best-effort basis while the project is maintained. Do not assume a debug build is safe for production business data.
