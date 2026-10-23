# Security Policy

Vendora is early in development, and security practices will evolve with the project.

## Reporting a vulnerability
Do not publish credentials, exploitable details, or private customer data in a public issue. Use GitHub's private vulnerability reporting feature for this repository if it is enabled. Otherwise, contact the repository owner through a private GitHub channel.

Include the affected version or commit, impact, reproduction steps, and any suggested mitigation. Do not include real shop or customer records.

## Security expectations
- Do not commit API keys, signing keys, passwords, tokens, or production data.
- Do not ship private AI provider credentials inside the Android app.
- Scope every business data operation to the active/authorized shop.
- Validate imported files and any structured output received from AI services.
- Keep backups and exports under the user's control and clearly indicate what they contain.
- Use least privilege for future staff roles and integrations.
