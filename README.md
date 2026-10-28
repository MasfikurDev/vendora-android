# Vendora

**Offline-first shop management for Android.** Vendora is being built for owners who manage one or more retail businesses and need reliable inventory and sales workflows even when connectivity is poor.

> **Project status:** Early preview; not production-ready. The repository is actively evolving. A successful build or debug APK is not a production-readiness claim. Some features may exist only on open pull-request branches; check the [project status ledger](docs/PROJECT_STATUS_AND_HISTORY.md) and the target branch before relying on them.

## Why Vendora?

- **Offline by default:** local business operations should not depend on an internet connection or AI service.
- **Multiple shops, one owner:** keep products and business data scoped to the selected shop.
- **Inventory that can be audited:** stock changes should have a reason and a movement record.
- **Safer selling:** checkout should commit sale records and stock deductions atomically.
- **Reusable product details:** brands, categories, and units should be easy to reuse without retyping, with offline starter suggestions for selected shop types.
- **Bangladesh-aware, not compliance-claiming:** BDT values use integer poisha internally; the bundled shop-type catalog includes Bengali labels. The app does not verify licenses, VAT/TIN status, or regulated-business permissions.
- **AI-ready by design:** future AI features are optional and must not become the source of truth for money, stock, or permissions.

## Current scope

The project is a work in progress. The development branch contains a Kotlin/Compose Android app and Room-backed shop, product, and selling foundations. Exact availability depends on the branch and merged pull requests.

| Area | Current direction |
| --- | --- |
| Shop setup | Multiple shops under one owner; offline shop-type suggestions |
| Products | Shop-scoped products, brand/category/unit fields, barcode text, low-stock thresholds |
| Sales | Multi-item cart and transactional checkout foundations |
| Data integrity | Integer minor-unit money, stock movement records, price/cost snapshots, explicit database migrations |
| Open data | Small bundled shop-type taxonomy with source notes; optional external product lookup is future work |
| Later work | Purchases, returns, expenses, richer reports, backup/restore, camera barcode scanning, product images, optional AI |

The table describes project direction and current development work, not a claim that every workflow is complete or production-validated. See [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) for branch-aware detail and known gaps.

## Screenshots and demo

No maintained screenshot gallery or public demo is currently documented. Do not treat a debug APK as a production release. Available preview builds, if any, are listed on [GitHub Releases](https://github.com/MasfikurDev/vendora-android/releases); inspect each release's notes and workflow evidence before installing it.

## Technology

- Kotlin
- Android UI: Jetpack Compose and Material 3
- Persistence: Room over SQLite, with a legacy SharedPreferences/JSON import path
- Build: Gradle wrapper, Android Gradle Plugin and Kotlin versions managed in `gradle/libs.versions.toml`
- CI: GitHub Actions builds a debug APK and runs configured JVM unit tests; instrumentation source compilation is not the same as executing those tests on an emulator/device.

Use the versions committed to the repository rather than copying version numbers from old documentation.

## Build from source

### Requirements

- JDK 17
- Android SDK platform 36 and build-tools 36.0.0, as required by the current project configuration
- The committed Gradle wrapper
- Android SDK licenses accepted on the build machine

### Windows PowerShell

```powershell
git clone https://github.com/MasfikurDev/vendora-android.git
Set-Location vendora-android
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

### Linux or macOS

```bash
git clone https://github.com/MasfikurDev/vendora-android.git
cd vendora-android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is normally produced under `app/build/outputs/apk/debug/`. Confirm the exact output path from the Gradle result if the build configuration changes. To install it, use Android Studio or `adb install -r <path-to-apk>` on a device with USB debugging enabled. A debug build is for development and testing, not a signed production distribution.

### Verification boundaries

- `assembleDebug` verifies that the configured debug variant builds.
- `testDebugUnitTest` runs JVM unit tests configured for that variant.
- Compiling `androidTest` sources does **not** execute instrumentation tests.
- Migration, checkout, and shop-isolation behavior should be exercised on a test database and an emulator/device before those flows are considered verified.
- A green workflow only proves the jobs and checks that actually ran for that exact commit.

## Data and privacy principles

- Business data should remain local unless a clearly described, user-controlled feature requires otherwise.
- Never use Float/Double for money; use integer minor units (poisha for BDT).
- Scope queries and mutations by shop.
- Treat imported files and AI output as untrusted input.
- Never place private AI provider credentials in the Android client.
- Do not silently replace local prices, costs, stock, or user-entered product information with external lookup results.
- Keep backup/export behavior explicit about what data is included.

See [Open data sources and catalog rules](docs/data/open-data-sources-and-catalog-rules.md) and [Third-party notices](THIRD_PARTY_NOTICES.md) for data provenance and licensing boundaries.

## Documentation map

- [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) — what is implemented, what is proposed, and known gaps.
- [Architecture principles](docs/architecture/principles.md) — engineering constraints and design decisions.
- [Product roadmap: reusable product details](docs/product/reusable-product-details.md) — product metadata and future extensions.
- [Open data sources and catalog rules](docs/data/open-data-sources-and-catalog-rules.md) — source quality, provenance, licensing, and data rules.
- [AI agent worklog](docs/AI_AGENT_WORKLOG.md) — repository-specific handoff and verified troubleshooting notes.
- [Documentation index](docs/README.md) — navigable guide to the project docs.

## Contributing and community

Contributions are welcome, subject to the project's current scope and review capacity.

- Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Report vulnerabilities using [SECURITY.md](SECURITY.md), not a public issue.
- Ask usage and development questions through [SUPPORT.md](SUPPORT.md).
- Review the [changelog](CHANGELOG.md) for curated release notes.

Open a focused issue before substantial work. Pull requests should include relevant tests, describe limitations honestly, and update documentation when behavior or project status changes. Do not assume an open pull request is already included in `main`.

## Development and release model

- Keep `main` buildable and use focused branches and pull requests.
- Prefer small, reviewable commits using Conventional Commit-style messages.
- Do not merge stacked pull requests out of dependency order.
- Feature previews and beta artifacts may be debug-signed APKs; they are not production-signed releases.
- Production distribution requires a separately verified signing, versioning, privacy, data-protection, and release process.
- Check [GitHub Actions](https://github.com/MasfikurDev/vendora-android/actions) and the specific release page before reporting CI or release success.

## License

Vendora's application source code is licensed under the [Apache License 2.0](LICENSE). Third-party data, assets, and dependencies may have separate terms; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The project license does not automatically relicense third-party material.
