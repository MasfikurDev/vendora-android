# Vendora

Offline-first Android shop management for owners who operate one or more businesses.

> **Status (2026-10-28):** Active early implementation; not production-ready. The Android app uses Kotlin, Jetpack Compose, Material 3, Room/SQLite, a repository layer, shop-scoped product operations, stock movement records, and an atomic checkout path. The core business flows and data recovery behavior still need broader automated and device testing.

## Product goals

- Manage multiple shops under one owner, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Track products, purchases, stock movements, sales, returns, expenses, and reports.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Keep AI optional; ordinary business operations must never depend on an AI service.

## Technology baseline

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin and Kotlin versions in `gradle/libs.versions.toml`
- JDK 17, Android SDK 36
- Room/SQLite with DAO/repository access and legacy JSON import
- GitHub Actions: debug APK, JVM tests, instrumentation-test source compilation, APK signature verification, and persistent signing-certificate verification when secrets are available. Emulator-backed instrumentation execution is opt-in.

## Current limitations

- The app is an early implementation and is not suitable as the only store of real business records.
- Room migration/import recovery and historical-data compatibility need continued validation.
- The sale and stock transaction path has targeted instrumentation regression tests; more coverage is needed for returns, adjustments, backup/restore, and schema upgrades.
- Product deletion is currently a hard delete; define and implement an archive/ledger-retention policy before relying on history for formal auditing.
- The database currently has no foreign-key constraints or database-enforced case-insensitive per-shop SKU uniqueness. Repository validation is not a substitute for all persistence-level invariants.
- A successful CI run does not prove update installation on every physical Android device. Compare the installed app's signing certificate when diagnosing update failures.
- Production signing, backup/restore, privacy/security review, and a full release process remain outstanding.

## Product scope

1. Shop creation and switching
2. Shop-scoped product catalog and inventory
3. Audited stock movement ledger
4. Purchases, sales, and returns
5. Expenses, profitability, and reports
6. Local export and backup
7. Optional AI provider boundary for read-only business questions

Staff permissions, multi-device sync, cloud services, forecasting, and specialized pharmacy workflows are later phases.

## Engineering rules

- Store money as integer minor units (poisha), never floating-point currency.
- Record stock changes as auditable movements; do not silently overwrite inventory history.
- Use transactions for operations that update a sale and stock together.
- Snapshot relevant prices and costs on transaction lines so historical reports remain meaningful.
- Include shop ownership/scope in relevant data access paths and tests.
- Use explicit, tested Room migrations; never add destructive migration fallbacks for real user data.
- Never commit credentials, signing keys, private API keys, or real customer data.
- AI output is untrusted input. Validate tool arguments and permissions, and calculate financial results in application code.

## Build and test

Requirements: JDK 17, Android SDK platform 36/build-tools 36.0.0, and the committed Gradle wrapper.

**Windows PowerShell**
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

**Linux / macOS**
```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The first two commands do not require an emulator; `connectedDebugAndroidTest` requires a connected Android device or emulator. Routine CI compiles instrumentation-test sources but does not start an emulator. To run the full Android instrumentation suite, manually dispatch **Android CI and Release Builds** with `run_instrumentation=true`. Check the exact workflow run and tested commit SHA before describing a build as verified.

## Git and build workflow

- Keep `main` buildable and use focused short-lived branches with pull requests.
- CI runs on relevant pushes to `main`, pull requests targeting `main`, and manual workflow dispatch. Routine feature-branch pushes do not trigger duplicate builds; use manual dispatch for a feature APK.
- To build a feature branch, open **Actions → Android CI and Release Builds → Run workflow** and select that branch.
- Leave `publish_release` unchecked for a downloadable artifact. Check it only when a GitHub prerelease is wanted.
- Same-repository builds require the configured persistent debug signing secrets. Fork PRs use temporary signing and cannot update an app installed with the persistent key.
- CI compares the APK certificate fingerprint with the configured keystore when persistent signing is enabled. An APK must retain the same application ID and signing certificate, and use a higher version code, to update an existing installation.
- Debug previews and beta artifacts are not production releases. Do not use them as the only copy of real business data.
- Do not merge pull requests without the repository's required checks and review safeguards.

See [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), [architecture principles](docs/architecture/principles.md), and [project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md).

## License

A license has not been selected. Do not assume this project is open source merely because the repository is public.
