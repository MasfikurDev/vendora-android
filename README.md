# Vendora

Offline-first Android shop management for owners who operate one or more businesses.

> **Current status (2026-10-24):** Early implementation, not production-ready. The app has a Kotlin + Jetpack Compose shell, local shop creation/switching, and shop-scoped product storage implemented on the active feature branch. Product storage currently uses JSON in SharedPreferences; Room/SQLite migration, a complete inventory UI, and end-to-end business workflows are still outstanding. Check [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) for the authoritative snapshot and history.

## Product goals

- Manage multiple shops under one owner, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Track products, purchases, stock movements, sales, returns, expenses, and reports.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Make AI optional; ordinary business operations must never depend on an AI service.

## Technology baseline

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin and Kotlin versions in `gradle/libs.versions.toml`
- JDK 17, Android SDK 36
- GitHub Actions: debug APK build and JVM unit tests
- **Current persistence:** SharedPreferences + JSON for shops and products
- **Target persistence:** Room/SQLite with explicit migrations, introduced as a focused migration
- Coroutines/Flow, repository boundaries, and lifecycle-aware state as implementation evolves

Do not describe planned technologies as already implemented. Keep the app small and add architecture only when a feature needs it.

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
- Use explicit Room migrations and test them when Room is introduced.
- Never commit credentials, signing keys, private API keys, or real customer data.
- AI output is untrusted input. Validate tool arguments and permissions, and calculate financial results in application code.

## Build and test

Requirements: JDK 17, Android SDK platform 36/build-tools 36.0.0, and the committed Gradle wrapper (Gradle 8.13).

**Windows PowerShell**
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

**Linux / macOS**
```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

CI builds a debug APK and runs JVM unit tests. Instrumentation tests are present in the scaffold but are not currently run by CI. A passing CI run proves the configured build/tests pass; it does not prove the app is production-ready.

## Git and release workflow

- Keep `main` buildable.
- Use focused short-lived feature/documentation branches and open pull requests.
- Pushes to branches run CI; pull requests targeting `main` run checks.
- Successful pushes to feature branches publish branch-specific GitHub prereleases for review; these are **debug preview APKs**, not stable beta or production releases.
- Only a successful push to `main` creates the main beta prerelease. These are **debug APK beta builds**, not signed production releases.
- Production releases require a separately planned, signed release process and explicit version/tag policy.
- Do not merge pull requests without explicit authorization from the repository owner.

See [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), [architecture principles](docs/architecture/principles.md), and [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md).

## License

A license has not yet been selected. Do not assume this project is open source merely because the repository is public.

## Feature preview releases

Each successful feature-branch push is intended to publish a branch-specific GitHub prerelease after `assembleDebug` and `testDebugUnitTest` pass. The workflow run must be checked before claiming a release exists. Feature previews are temporary review builds; only `main` publishes the project beta channel.
